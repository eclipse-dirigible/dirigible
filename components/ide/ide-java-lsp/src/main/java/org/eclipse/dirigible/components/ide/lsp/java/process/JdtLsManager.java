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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * cross-project references. Instances are created lazily on the first WebSocket connection and
 * destroyed when the Spring context shuts down. If the JDT.LS distribution is not yet present at
 * the configured install directory, it is downloaded automatically from
 * {@code DIRIGIBLE_JAVA_LSP_DOWNLOAD_URL}.
 *
 * <p>
 * When a new {@link JavaCompiledEvent} arrives (fired by {@code JavaLoader} after each rebuild),
 * all live instances are notified via {@code workspace/didChangeWatchedFiles} so that JDT.LS
 * re-indexes the compiled output directory without requiring a restart.
 */
@Component
@ConditionalOnProperty(name = "java.lsp.enabled", havingValue = "true", matchIfMissing = true)
public class JdtLsManager implements DisposableBean, ApplicationListener<JavaCompiledEvent> {

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

    public JdtLsManager(ClassPathIndex classPathIndex, Optional<JavaCompiledOutputDirectory> compiledOutputDirectory) {
        this.classPathIndex = classPathIndex;
        this.compiledOutputDir = compiledOutputDirectory.map(JavaCompiledOutputDirectory::get)
                                                        .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Public API used by the WebSocket handler
    // -------------------------------------------------------------------------

    /**
     * Returns (and lazily starts) the JDT.LS instance for the given workspace. Also ensures Eclipse
     * project metadata files exist for every Java project currently in the workspace. Thread-safe; at
     * most one process is ever started per key.
     */
    public JdtLsInstance getOrStart(String username, String workspace) throws Exception {
        ensureInstalled();
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
     * Notifies all live JDT.LS instances that compiled class files have changed so they can
     * re-index without a restart.
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
        logger.info("[java-lsp] Starting JDT.LS for {}/{} → {}", username, workspace, workspaceRootPath);

        Process process = new ProcessBuilder(cmd).start();
        return new JdtLsInstance(process, virtualRoot, realRoot);
    }

    /**
     * Ensures Eclipse project descriptor files ({@code .project}, {@code .classpath}) exist for
     * every direct sub-directory of {@code workspaceRoot} that contains at least one
     * {@code .java} file.
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
                            logger.warn("[java-lsp] Could not create Eclipse project files in {}: {}", projectDir,
                                    e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            logger.warn("[java-lsp] Could not list workspace directory {}: {}", workspaceRoot, e.getMessage(), e);
        }
    }

    /**
     * Returns {@code true} if the given directory contains at least one {@code .java} file within
     * the first five levels of nesting.
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
     * project and activate type resolution, completion, and diagnostics. Dirigible does not create
     * them when a user creates a new Java project through the IDE, so we generate them here on first
     * LSP connection.
     */
    private void ensureEclipseProjectFiles(Path projectRoot, String project) throws IOException {
        Path dotProject = projectRoot.resolve(".project");
        if (!Files.exists(dotProject)) {
            Files.writeString(dotProject, buildProjectXml(project), StandardCharsets.UTF_8);
            logger.info("[java-lsp] Created .project for {}", project);
        }
        Path dotClasspath = projectRoot.resolve(".classpath");
        if (!Files.exists(dotClasspath)) {
            Files.writeString(dotClasspath, buildClasspathXml(), StandardCharsets.UTF_8);
            logger.info("[java-lsp] Created .classpath for {}", project);
        }
    }

    private static String buildProjectXml(String project) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<projectDescription>\n"
                + "    <name>" + project + "</name>\n"
                + "    <natures>\n"
                + "        <nature>org.eclipse.jdt.core.javanature</nature>\n"
                + "    </natures>\n"
                + "    <buildSpec>\n"
                + "        <buildCommand>\n"
                + "            <name>org.eclipse.jdt.core.javabuilder</name>\n"
                + "        </buildCommand>\n"
                + "    </buildSpec>\n"
                + "</projectDescription>\n";
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
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<classpath>\n"
                + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>\n"
                + libs
                + "    <classpathentry kind=\"output\" path=\"bin\"/>\n"
                + "</classpath>\n";
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

    // -------------------------------------------------------------------------
    // Installation helpers
    // -------------------------------------------------------------------------

    private synchronized void ensureInstalled() throws Exception {
        if (installChecked)
            return;
        jdtlsHome = resolveInstallDir();
        if (!isInstalled()) {
            if (!extractBundled()) {
                try {
                    download();
                } catch (Exception e) {
                    Path archive = jdtlsHome.resolve("jdtls.tar.gz");
                    Files.deleteIfExists(archive);
                    throw e;
                }
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
                logger.debug("[java-lsp] No bundled JDT.LS resource found, will attempt download");
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

    private void download() throws Exception {
        String url = DirigibleConfig.JAVA_LSP_DOWNLOAD_URL.getStringValue();
        if (url == null || url.isBlank()) {
            throw new Exception("[java-lsp] JDT.LS is not installed at " + jdtlsHome + " and DIRIGIBLE_JAVA_LSP_DOWNLOAD_URL is not set. "
                    + "Extract a JDT.LS release to that directory or set the download URL.");
        }
        logger.info("[java-lsp] Downloading JDT.LS from {} ...", url);
        Files.createDirectories(jdtlsHome);
        Path archive = jdtlsHome.resolve("jdtls.tar.gz");

        // curl handles GitHub's multi-hop redirects (github.com → objects.githubusercontent.com)
        // reliably; Java's HttpClient can fail on the second hop with some JDK versions.
        int curlRc = new ProcessBuilder("curl", "-fsSL", "--retry", "3", "--output", archive.toString(), url).redirectErrorStream(true)
                                                                                                             .start()
                                                                                                             .waitFor();
        if (curlRc != 0) {
            throw new Exception("[java-lsp] curl download failed (exit " + curlRc + "). Check that " + url + " is reachable.");
        }

        long size = Files.size(archive);
        if (size < 1024 * 1024) {
            throw new Exception("[java-lsp] Downloaded archive is too small (" + size
                    + " bytes) — the URL may be wrong or the server returned an error page. URL: " + url);
        }

        logger.info("[java-lsp] Extracting JDT.LS ({} MB) to {} ...", size / (1024 * 1024), jdtlsHome);
        int tarRc = new ProcessBuilder("tar", "xzf", archive.toString(), "-C", jdtlsHome.toString()).inheritIO()
                                                                                                    .start()
                                                                                                    .waitFor();
        if (tarRc != 0) {
            throw new Exception("[java-lsp] tar extraction failed with exit code " + tarRc);
        }
        Files.deleteIfExists(archive);
        logger.info("[java-lsp] JDT.LS installed at {}", jdtlsHome);
    }
}
