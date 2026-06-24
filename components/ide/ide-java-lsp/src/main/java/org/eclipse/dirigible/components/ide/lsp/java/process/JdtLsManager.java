/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.lsp.java.process;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.engine.java.runtime.ClassPathIndex;
import org.eclipse.dirigible.engine.java.runtime.JavaCompiledEvent;
import org.eclipse.dirigible.engine.java.runtime.JavaCompiledOutputDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Manages JDT Language Server processes — one per {@code username/workspace} pair.
 *
 * <p>
 * A single JDT.LS instance covers the entire workspace so that sibling projects can resolve
 * cross-project references. Instances are created on first WebSocket connection and destroyed when
 * the Spring context shuts down.
 *
 * <p>
 * At application startup ({@link #run(ApplicationArguments)}), the bundled {@code jdtls.tar.gz}
 * resource (downloaded during the Maven build and packed into the application JAR) is extracted to
 * the configured install directory in a background thread. No network access is performed at
 * runtime; if the bundled resource is absent (e.g. quick-build), JDT.LS is simply disabled.
 *
 * <p>
 * When a new {@link JavaCompiledEvent} arrives (fired by {@code JavaLoader} after each rebuild),
 * all live instances are notified via {@code workspace/didChangeWatchedFiles} so that JDT.LS
 * re-indexes the compiled output directory without requiring a restart.
 */
@Component
@ConditionalOnProperty(name = "java.lsp.enabled", havingValue = "true", matchIfMissing = true)
public class JdtLsManager implements DisposableBean, ApplicationRunner, ApplicationListener<JavaCompiledEvent> {

    private static final Logger logger = LoggerFactory.getLogger(JdtLsManager.class);

    /** key = "username/workspace" → running instance */
    private final Map<String, JdtLsInstance> instances = new ConcurrentHashMap<>();

    private final ClassPathIndex classPathIndex;

    /**
     * On-disk directory for compiled user {@code .class} files. {@code null} when {@code engine-java}
     * is not active (defensive; in practice both modules are always co-deployed).
     */
    private final Path compiledOutputDir;

    private volatile Path jdtlsHome;
    private volatile boolean installChecked = false;

    /**
     * Set to {@code true} once the JDT.LS distribution has been verified (or extracted) at startup.
     * {@code false} means JDT.LS is either still being prepared or was not found in the bundled
     * resources (e.g. quick-build). All {@link #getOrStart} calls are rejected until this is
     * {@code true}.
     */
    private volatile boolean available = false;

    public JdtLsManager(ClassPathIndex classPathIndex, Optional<JavaCompiledOutputDirectory> compiledOutputDirectory) {
        this.classPathIndex = classPathIndex;
        this.compiledOutputDir = compiledOutputDirectory.map(JavaCompiledOutputDirectory::get)
                                                        .orElse(null);
    }

    // -------------------------------------------------------------------------
    // ApplicationRunner — eager startup
    // -------------------------------------------------------------------------

    /**
     * Called by Spring after the application context is fully started. Extracts the bundled JDT.LS
     * distribution (if not already present on disk) in a virtual background thread so that the first
     * WebSocket connection does not pay the extraction cost.
     */
    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual()
              .name("jdtls-install")
              .start(() -> {
                  try {
                      ensureInstalled();
                      available = true;
                      logger.info("[java-lsp] JDT.LS is ready at {}", jdtlsHome);
                      // Materialise the platform classpath now (it is cached on disk), so the first Java
                      // file the user opens does not pay the multi-second extraction on its critical path.
                      try {
                          int entries = classPathIndex.classPathEntries()
                                                      .size();
                          logger.info("[java-lsp] Pre-warmed compile classpath: {} entries", entries);
                      } catch (Exception warmEx) {
                          logger.warn("[java-lsp] Classpath pre-warm failed (will materialise lazily on first use)", warmEx);
                      }
                  } catch (Exception e) {
                      logger.warn("[java-lsp] JDT.LS is not available: {}. Java language support will be disabled.", e.getMessage());
                  }
              });
    }

    // -------------------------------------------------------------------------
    // Public API used by the WebSocket handler
    // -------------------------------------------------------------------------

    /**
     * Returns (and lazily starts) the JDT.LS instance for the given workspace. Also ensures Eclipse
     * project metadata files exist for every Java project currently in the workspace. Thread-safe; at
     * most one process is ever started per key.
     *
     * @throws IllegalStateException if JDT.LS has not finished initializing or is not available
     */
    public JdtLsInstance getOrStart(String username, String workspace) throws Exception {
        if (!available) {
            throw new IllegalStateException(
                    "[java-lsp] JDT.LS is not yet available — still starting up or not bundled in this build. Try again in a moment.");
        }
        String key = username + "/" + workspace;
        JdtLsInstance existing = instances.get(key);
        if (existing != null && existing.isAlive()) {
            ensureEclipseProjectFilesForWorkspace(workspaceRoot(username, workspace));
            return existing;
        }
        synchronized (this) {
            existing = instances.get(key);
            if (existing != null && existing.isAlive()) {
                ensureEclipseProjectFilesForWorkspace(workspaceRoot(username, workspace));
                return existing;
            }
            JdtLsInstance fresh = startInstance(username, workspace);
            instances.put(key, fresh);
            return fresh;
        }
    }

    /** Finds the instance that owns the given WebSocket session ID. */
    public JdtLsInstance findForSession(String sessionId) {
        for (JdtLsInstance inst : instances.values()) {
            if (inst.hasSession(sessionId))
                return inst;
        }
        return null;
    }

    @Override
    public void destroy() {
        logger.info("[java-lsp] Shutting down {} instance(s)", instances.size());
        instances.values()
                 .forEach(JdtLsInstance::destroy);
        instances.clear();
    }

    /**
     * Notifies all live JDT.LS instances that compiled class files have changed so they can re-index
     * without a restart.
     */
    @Override
    public void onApplicationEvent(JavaCompiledEvent event) {
        if (compiledOutputDir == null) {
            return;
        }
        for (JdtLsInstance inst : instances.values()) {
            if (inst.isAlive()) {
                inst.notifyCompiledOutputChanged(compiledOutputDir, event.getCompiledFqns(), event.getRemovedFqns());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Process startup
    // -------------------------------------------------------------------------

    private JdtLsInstance startInstance(String username, String workspace) throws Exception {
        Path workspaceRootPath = workspaceRoot(username, workspace);
        Files.createDirectories(workspaceRootPath);
        ensureEclipseProjectFilesForWorkspace(workspaceRootPath);

        Path dataDir = jdtlsHome.resolve("data")
                                .resolve(username)
                                .resolve(workspace);
        // Reuse the JDT.LS index across restarts so the (expensive) re-index of the platform classpath
        // is not paid every time. The cached index only goes stale when the classpath itself changes
        // (ClassPathIndex extracts to a stable dir and re-extracts only on a new build), so wipe the
        // index only when a classpath fingerprint marker is missing or no longer matches.
        Path fingerprintMarker = dataDir.resolveSibling(dataDir.getFileName() + ".classpath-fp");
        String fingerprint = classpathFingerprint();
        boolean indexValid = !fingerprint.isEmpty() && Files.isDirectory(dataDir) && Files.exists(fingerprintMarker)
                && fingerprint.equals(readFingerprint(fingerprintMarker));
        if (indexValid) {
            logger.info("[java-lsp] Reusing cached JDT.LS index for {}/{}", sanitize(username), sanitize(workspace));
        } else {
            logger.info("[java-lsp] Building JDT.LS index for {}/{} (first run or classpath changed)", sanitize(username),
                    sanitize(workspace));
            deleteDirectory(dataDir);
            Files.createDirectories(dataDir);
            if (!fingerprint.isEmpty()) {
                Files.writeString(fingerprintMarker, fingerprint);
            }
        }

        String launcherJar = findLauncherJar();
        String configDir = resolveConfigDir();

        // Virtual URI root the browser uses; real URI root JDT.LS sees on disk.
        String virtualRoot = "file:///workspace/" + workspace + "/";
        String realRoot = workspaceRootPath.toUri()
                                           .toString();
        if (!realRoot.endsWith("/")) {
            realRoot += "/";
        }

        List<String> cmd = buildCommand(launcherJar, configDir, dataDir.toString());
        logger.info("[java-lsp] Starting JDT.LS for {}/{} → {}", sanitize(username), sanitize(workspace), workspaceRootPath);

        Process process = new ProcessBuilder(cmd).start();
        return new JdtLsInstance(process, virtualRoot, realRoot);
    }

    /**
     * Ensures Eclipse project descriptor files ({@code .project}, {@code .classpath}) exist for every
     * direct sub-directory of {@code workspaceRoot} that contains at least one {@code .java} file.
     */
    private void ensureEclipseProjectFilesForWorkspace(Path workspaceRoot) {
        if (!Files.isDirectory(workspaceRoot)) {
            return;
        }
        try (Stream<Path> children = Files.list(workspaceRoot)) {
            children.filter(Files::isDirectory)
                    .filter(this::containsJavaFile)
                    .forEach(projectDir -> {
                        try {
                            ensureEclipseProjectFiles(projectDir, projectDir.getFileName()
                                                                            .toString());
                        } catch (IOException e) {
                            logger.warn("[java-lsp] Could not create Eclipse project files in {}: {}", sanitize(projectDir.toString()),
                                    e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            logger.warn("[java-lsp] Could not list workspace directory {}: {}", sanitize(workspaceRoot.toString()), e.getMessage(), e);
        }
    }

    /**
     * Returns {@code true} if the given directory contains at least one {@code .java} file within the
     * first five levels of nesting.
     *
     * <p>
     * {@link FileVisitOption#FOLLOW_LINKS} is required because git-backed workspace projects are
     * symlinks into the repository's {@code .git} store. Without it, {@link Files#walk} treats the
     * symlinked project directory as a non-traversable file, finds no {@code .java}, and the project is
     * silently skipped — so {@code .project}/{@code .classpath} are never generated and JDT.LS falls
     * back to a JRE-only default project (SDK and project types do not resolve).
     */
    private boolean containsJavaFile(Path dir) {
        try (Stream<Path> walk = Files.walk(dir, 5, FileVisitOption.FOLLOW_LINKS)) {
            return walk.anyMatch(p -> !Files.isDirectory(p) && p.getFileName()
                                                                .toString()
                                                                .endsWith(".java"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Writes {@code .project} and {@code .classpath} into {@code projectRoot} when they are absent.
     *
     * <p>
     * JDT.LS requires these two Eclipse project descriptor files to recognise a directory as a Java
     * project and activate type resolution, completion, and diagnostics. Dirigible does not create them
     * when a user creates a new Java project through the IDE, so we generate them here on first LSP
     * connection.
     */
    private void ensureEclipseProjectFiles(Path projectRoot, String project) throws IOException {
        Path dotProject = projectRoot.resolve(".project");
        Files.writeString(dotProject, buildProjectXml(project), StandardCharsets.UTF_8);
        logger.debug("[java-lsp] Wrote .project for {}", sanitize(project));

        Path dotClasspath = projectRoot.resolve(".classpath");
        Files.writeString(dotClasspath, buildClasspathXml(), StandardCharsets.UTF_8);
        logger.debug("[java-lsp] Wrote .classpath for {}", sanitize(project));
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }

    private static String buildProjectXml(String project) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<projectDescription>\n" + "    <name>" + project + "</name>\n"
                + "    <natures>\n" + "        <nature>org.eclipse.jdt.core.javanature</nature>\n" + "    </natures>\n"
                + "    <buildSpec>\n" + "        <buildCommand>\n" + "            <name>org.eclipse.jdt.core.javabuilder</name>\n"
                + "        </buildCommand>\n" + "    </buildSpec>\n" + "</projectDescription>\n";
    }

    private String buildClasspathXml() {
        StringBuilder libs = new StringBuilder();
        for (Path entry : classPathIndex.classPathEntries()) {
            libs.append("    <classpathentry kind=\"lib\" path=\"")
                .append(entry.toString())
                .append("\"/>\n");
        }
        if (compiledOutputDir != null) {
            libs.append("    <classpathentry kind=\"lib\" path=\"")
                .append(compiledOutputDir.toString())
                .append("\"/>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<classpath>\n" + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n" + libs
                + "    <classpathentry kind=\"output\" path=\"bin\"/>\n" + "</classpath>\n";
    }

    private List<String> buildCommand(String launcherJar, String configDir, String dataDir) {
        // Re-use the same JVM that runs Dirigible so the JDK path is always known.
        String javaExe = ProcessHandle.current()
                                      .info()
                                      .command()
                                      .orElse("java");
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("--add-modules=ALL-SYSTEM");
        cmd.add("--add-opens=java.base/java.util=ALL-UNNAMED");
        cmd.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        cmd.add("-Declipse.application=org.eclipse.jdt.ls.core.id1");
        cmd.add("-Dosgi.bundles.defaultStartLevel=4");
        cmd.add("-Declipse.product=org.eclipse.jdt.ls.core.product");
        cmd.add("-Dlog.protocol=true");
        cmd.add("-Dlog.level=ALL");
        cmd.add("-noverify");
        cmd.add("-Xmx" + DirigibleConfig.JAVA_LSP_MAX_HEAP.getStringValue());
        cmd.add("-jar");
        cmd.add(launcherJar);
        cmd.add("-configuration");
        cmd.add(configDir);
        cmd.add("-data");
        cmd.add(dataDir);
        return cmd;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the on-disk workspace root for the given user and workspace name, mirroring the layout
     * used by {@code FileSystemRepository}:
     * {@code <repoRoot>/dirigible/repository/root/users/<username>/<workspace>}
     */
    private static Path workspaceRoot(String username, String workspace) {
        String repoRoot = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue();
        return Paths.get(repoRoot, "dirigible", "repository", "root", "users", username, workspace)
                    .toAbsolutePath()
                    .normalize();
    }

    /**
     * SHA-256 over the sorted classpath entry paths; identifies whether a cached index is still valid.
     */
    private String classpathFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            classPathIndex.classPathEntries()
                          .stream()
                          .map(Path::toString)
                          .sorted()
                          .forEach(entry -> digest.update(entry.getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of()
                            .formatHex(digest.digest());
        } catch (Exception e) {
            logger.warn("[java-lsp] Could not compute classpath fingerprint; index will be rebuilt", e);
            return "";
        }
    }

    private static String readFingerprint(Path marker) {
        try {
            return Files.readString(marker)
                        .trim();
        } catch (IOException e) {
            return "";
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null)
                    throw exc;
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Installation helpers
    // -------------------------------------------------------------------------

    private synchronized void ensureInstalled() throws Exception {
        if (installChecked)
            return;
        jdtlsHome = resolveInstallDir();
        if (!isInstalled()) {
            if (!extractBundled()) {
                throw new IOException(
                        "[java-lsp] JDT.LS is not installed at " + jdtlsHome + " and no bundled archive was found in this build. "
                                + "Run a full Maven build (without -P quick-build) to download and bundle JDT.LS, "
                                + "or pre-extract a JDT.LS release to that directory.");
            }
        }
        installDebugPlugin();
        installChecked = true;
    }

    /**
     * Copies the bundled {@code com.microsoft.java.debug.plugin.jar} into the JDT.LS {@code plugins/}
     * directory and registers it in every {@code config_<platform>/config.ini} file. A no-op when the
     * bundled resource is absent (e.g. quick-build without the download).
     *
     * <p>
     * JDT.LS uses Equinox with a hardcoded {@code osgi.bundles} list in {@code config.ini}. Only
     * bundles explicitly listed there are installed — placing a JAR in the {@code plugins/} directory
     * alone is not sufficient. This method therefore both copies the JAR and appends the bundle
     * reference to every platform-specific {@code config.ini}.
     */
    private void installDebugPlugin() {
        try (InputStream bundled = getClass().getClassLoader()
                                             .getResourceAsStream("jdtls-debug/com.microsoft.java.debug.plugin.jar")) {
            if (bundled == null) {
                logger.debug("[java-lsp] No bundled debug plugin found — Java debugger will be unavailable");
                return;
            }
            Path pluginsDir = jdtlsHome.resolve("plugins");

            // Stage to a temp file so we can read the Bundle-Version before committing the final name.
            // Equinox requires the naming convention <symbolicname>_<version>.jar.
            Path staging = pluginsDir.resolve(".debug-plugin-staging.jar");
            Files.copy(bundled, staging, StandardCopyOption.REPLACE_EXISTING);

            String bundleVersion = "0.0.0";
            try (JarFile jar = new JarFile(staging.toFile())) {
                java.util.jar.Manifest mf = jar.getManifest();
                if (mf != null) {
                    String v = mf.getMainAttributes()
                                 .getValue("Bundle-Version");
                    if (v != null && !v.isBlank()) {
                        bundleVersion = v.trim();
                    }
                }
            }

            String targetFileName = "com.microsoft.java.debug.plugin_" + bundleVersion + ".jar";

            // Remove any pre-existing debug plugin JARs (staging file excluded by its '.' prefix).
            try (Stream<Path> existing = Files.list(pluginsDir)) {
                existing.filter(p -> p.getFileName()
                                      .toString()
                                      .startsWith("com.microsoft.java.debug.plugin"))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException ignored) {
                            }
                        });
            }

            Path target = pluginsDir.resolve(targetFileName);
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("[java-lsp] Installed debug plugin at {}", target);

            // Register in config.ini — required because JDT.LS/Equinox ignores JARs that are not
            // listed in the osgi.bundles property of config_<platform>/config.ini.
            registerInConfigIni(targetFileName);
        } catch (Exception e) {
            logger.warn("[java-lsp] Could not install debug plugin: {}", e.getMessage());
        }
    }

    /**
     * Adds {@code pluginFileName} to the {@code osgi.bundles} list in every
     * {@code config_<platform>/config.ini} under {@link #jdtlsHome}. Existing entries for
     * {@code com.microsoft.java.debug.plugin} (any version) are removed first to avoid duplicates.
     */
    private void registerInConfigIni(String pluginFileName) {
        // In config.ini (Java properties format) colons must be escaped as \:
        String pluginEntry = "reference\\:file\\:" + pluginFileName + "@4";
        try (Stream<Path> children = Files.list(jdtlsHome)) {
            children.filter(p -> p.getFileName()
                                  .toString()
                                  .startsWith("config_")
                    && Files.isDirectory(p))
                    .forEach(configDir -> {
                        Path configIni = configDir.resolve("config.ini");
                        if (Files.exists(configIni)) {
                            try {
                                addToOsgiBundles(configIni, pluginEntry);
                            } catch (IOException e) {
                                logger.warn("[java-lsp] Could not update {}: {}", configIni, e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            logger.warn("[java-lsp] Could not scan jdtlsHome for config dirs: {}", e.getMessage());
        }
    }

    private static void addToOsgiBundles(Path configIni, String pluginEntry) throws IOException {
        String content = Files.readString(configIni, StandardCharsets.UTF_8);

        // Strip any existing entry for com.microsoft.java.debug.plugin (possibly a different version)
        // to avoid loading two versions simultaneously.
        String cleaned = content.replaceAll(",reference\\\\:file\\\\:com\\.microsoft\\.java\\.debug\\.plugin[^,\\r\\n]*", "");

        if (cleaned.contains(pluginEntry)) {
            return;
        }

        // Append our entry at the end of the osgi.bundles line.
        int osgiBundlesIdx = cleaned.indexOf("osgi.bundles=");
        if (osgiBundlesIdx < 0) {
            logger.warn("[java-lsp] osgi.bundles property not found in {}", configIni);
            return;
        }
        // osgi.bundles is a single very long line in JDT.LS config.ini (no backslash continuation).
        int lineEnd = cleaned.indexOf('\n', osgiBundlesIdx);
        if (lineEnd < 0) {
            lineEnd = cleaned.length();
        }
        // Trim trailing \r if present (Windows line endings)
        int insertAt = (lineEnd > 0 && cleaned.charAt(lineEnd - 1) == '\r') ? lineEnd - 1 : lineEnd;
        String updated = cleaned.substring(0, insertAt) + "," + pluginEntry + cleaned.substring(insertAt);
        Files.writeString(configIni, updated, StandardCharsets.UTF_8);
        logger.info("[java-lsp] Registered debug plugin in {}", configIni);
    }

    /**
     * Extracts the JDT.LS tar.gz that was bundled into the JAR at build time.
     *
     * @return {@code true} if the bundled resource was found and extracted successfully
     */
    private boolean extractBundled() throws Exception {
        try (InputStream bundled = getClass().getClassLoader()
                                             .getResourceAsStream("jdtls/jdtls.tar.gz")) {
            if (bundled == null) {
                logger.debug("[java-lsp] No bundled JDT.LS resource found in this build");
                return false;
            }
            logger.info("[java-lsp] Extracting bundled JDT.LS to {} ...", jdtlsHome);
            Files.createDirectories(jdtlsHome);
            Process tar = new ProcessBuilder("tar", "xzf", "-", "-C", jdtlsHome.toString()).redirectError(ProcessBuilder.Redirect.INHERIT)
                                                                                           .start();
            bundled.transferTo(tar.getOutputStream());
            tar.getOutputStream()
               .close();
            int rc = tar.waitFor();
            if (rc != 0) {
                throw new Exception("[java-lsp] tar extraction of bundled JDT.LS failed with exit code " + rc);
            }
            logger.info("[java-lsp] JDT.LS installed from bundled resource at {}", jdtlsHome);
            return true;
        }
    }

    private Path resolveInstallDir() {
        String configured = DirigibleConfig.JAVA_LSP_INSTALL_DIR.getStringValue();
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured)
                        .toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.home"), ".dirigible", "jdtls")
                    .toAbsolutePath();
    }

    private boolean isInstalled() {
        try {
            findLauncherJar();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String findLauncherJar() throws Exception {
        Path plugins = jdtlsHome.resolve("plugins");
        if (!Files.isDirectory(plugins)) {
            throw new Exception("[java-lsp] plugins dir not found: " + plugins);
        }
        return Files.list(plugins)
                    .filter(p -> {
                        String name = p.getFileName()
                                       .toString();
                        return name.startsWith("org.eclipse.equinox.launcher_") && name.endsWith(".jar");
                    })
                    .map(Path::toString)
                    .findFirst()
                    .orElseThrow(() -> new Exception("[java-lsp] Equinox launcher jar not found in " + plugins));
    }

    private String resolveConfigDir() {
        String os = System.getProperty("os.name", "")
                          .toLowerCase();
        String arch = System.getProperty("os.arch", "")
                            .toLowerCase();
        boolean arm = arch.contains("aarch64") || arch.contains("arm");
        String name;
        if (os.contains("win"))
            name = "config_win";
        else if (os.contains("mac"))
            name = arm ? "config_mac_arm" : "config_mac";
        else
            name = arm ? "config_linux_arm" : "config_linux";
        return jdtlsHome.resolve(name)
                        .toString();
    }

}
