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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
        // Wipe stale workspace index so JDT.LS re-imports projects with the current .classpath.
        // Without this, a cached Eclipse workspace that references deleted JAR paths (e.g. from
        // a previous temp-dir extraction) causes unresolved-import errors on every restart.
        deleteDirectory(dataDir);
        Files.createDirectories(dataDir);

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
     */
    private boolean containsJavaFile(Path dir) {
        try (Stream<Path> walk = Files.walk(dir, 5)) {
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
        cmd.add("-Xmx512m");
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
        installChecked = true;
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
