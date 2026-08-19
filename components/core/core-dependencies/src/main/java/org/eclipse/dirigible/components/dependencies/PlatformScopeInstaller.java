/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import org.eclipse.dirigible.launcher.agent.InstrumentationHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Activates {@code scope: "platform"} dependencies by appending their JARs to the <b>system</b>
 * classloader via {@link Instrumentation#appendToSystemClassLoaderSearch(JarFile)} - always from
 * the immutable versioned local-repository paths.
 *
 * <p>
 * <b>The append-only contract - deliberate, do not "fix" into removability.</b> The system
 * classloader can only ever grow: the JVM offers no removal, and the libraries this tier exists for
 * are exactly the ones that must not live in a swappable loader in the first place:
 * <ul>
 * <li><b>JDBC drivers</b> - {@code DriverManager} applies a caller-classloader visibility check, so
 * a driver visible only to the modules loader cannot serve platform code obtaining connections; it
 * has to be on the system classpath.</li>
 * <li><b>JNI-bearing libraries</b> (netty natives, SQLite, AWS CRT, ...) - the JVM binds a native
 * library to exactly one classloader per process; loading it in a swappable loader would throw
 * {@code UnsatisfiedLinkError: already loaded in another classloader} on the first generation
 * swap.</li>
 * </ul>
 * Consequently: <b>adding</b> is restartless; <b>upgrade and removal take effect at the next
 * launch</b>. A changed version for an already-active {@code groupId:artifactId} is never appended
 * again in the same process - it is reported as {@code pending-restart} with a WARN naming both
 * versions. Restarts converge through part 1's resolved-modules seed directory, which puts the
 * currently-declared platform JARs on the launch classpath, so the agent path only ever serves
 * mid-flight additions.
 *
 * <p>
 * FQN collisions across appended JARs follow JVM semantics (first loaded wins); an overlap of
 * package roots with already-appended JARs is detected and reported with a WARN.
 */
@Component
class PlatformScopeInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformScopeInstaller.class);

    /** The status of an active artifact. */
    static final String STATUS_ACTIVE = "active";

    /** The status of an artifact that activates at the next launch. */
    static final String STATUS_PENDING_RESTART = "pending-restart";

    /** The status of an artifact that failed to activate. */
    static final String STATUS_FAILED = "failed";

    /** The JDBC driver service entry. */
    private static final String JDBC_SERVICES_ENTRY = "META-INF/services/java.sql.Driver";

    /**
     * The reported activation state of one platform-scoped artifact.
     *
     * @param coordinate the groupId:artifactId:version coordinate
     * @param status active, pending-restart or failed
     * @param message what happened, operator-readable
     */
    record PlatformArtifactState(String coordinate, String status, String message) {
    }

    /**
     * An artifact active in this process.
     *
     * @param version the active version
     * @param jar the jar serving it
     */
    private record ActiveArtifact(String version, Path jar) {
    }

    /** The instrumentation source - injectable for tests. */
    private final Supplier<Instrumentation> instrumentationSupplier;

    /** groupId:artifactId of everything active in this process, to its version and jar. */
    private final Map<String, ActiveArtifact> activeByGa = new LinkedHashMap<>();

    /** The package roots of the appended jars, for the collision warning. */
    private final Set<String> appendedPackageRoots = new LinkedHashSet<>();

    /** The launch-classpath jar file names, for boot convergence detection. */
    private final Set<String> launchJarNames;

    /**
     * Instantiates the installer reading the instrumentation the launcher agent captured.
     */
    PlatformScopeInstaller() {
        this(InstrumentationHolder::get);
    }

    /**
     * Instantiates the installer with an explicit instrumentation source - the constructor the unit
     * tests use.
     *
     * @param instrumentationSupplier the instrumentation source
     */
    PlatformScopeInstaller(Supplier<Instrumentation> instrumentationSupplier) {
        this.instrumentationSupplier = instrumentationSupplier;
        Set<String> names = new LinkedHashSet<>();
        for (Path jar : DependencyPaths.launchClasspathJars()) {
            names.add(String.valueOf(jar.getFileName()));
        }
        this.launchJarNames = names;
    }

    /**
     * Reconciles the resolved platform-tier JARs into the running process, per the append-only
     * contract.
     *
     * @param localRepository the local repository the artifacts live in
     * @param artifacts the resolved platform-tier jar paths
     * @return the per-artifact activation states
     */
    synchronized List<PlatformArtifactState> install(Path localRepository, List<Path> artifacts) {
        List<PlatformArtifactState> states = new ArrayList<>();
        for (Path jar : artifacts) {
            String coordinate = DependencyPaths.coordinate(localRepository, jar);
            int lastColon = coordinate.lastIndexOf(':');
            String ga = lastColon > 0 ? coordinate.substring(0, lastColon) : coordinate;
            String version = lastColon > 0 ? coordinate.substring(lastColon + 1) : "";

            if (onLaunchClasspath(jar)) {
                // seeded by the previous session's resolution - active since launch
                activeByGa.putIfAbsent(ga, new ActiveArtifact(version, jar));
                states.add(new PlatformArtifactState(coordinate, STATUS_ACTIVE, "On the launch classpath since this launch"));
                continue;
            }

            ActiveArtifact active = activeByGa.get(ga);
            if (active != null) {
                if (active.version()
                          .equals(version)) {
                    states.add(new PlatformArtifactState(coordinate, STATUS_ACTIVE, "Active in this process"));
                } else {
                    String message = "Version [" + active.version() + "] stays active in this process; [" + version
                            + "] takes effect at the next launch - the platform tier is append-only";
                    LOGGER.warn("Platform dependency [{}]: active as [{}], declared as [{}] - the new version takes effect at the "
                            + "next launch (append-only contract)", ga, active.version(), version);
                    states.add(new PlatformArtifactState(coordinate, STATUS_PENDING_RESTART, message));
                }
                continue;
            }

            Instrumentation instrumentation = instrumentationSupplier.get();
            if (instrumentation == null) {
                states.add(new PlatformArtifactState(coordinate, STATUS_PENDING_RESTART,
                        "No instrumentation in this launch - activates at the next launch from the resolved-modules seed"));
                continue;
            }

            try {
                Set<String> packageRoots = packageRoots(jar);
                Set<String> overlap = new LinkedHashSet<>(packageRoots);
                overlap.retainAll(appendedPackageRoots);
                if (!overlap.isEmpty()) {
                    LOGGER.warn("Platform dependency [{}] shares the package root(s) {} with already-appended jars - on an FQN "
                            + "collision the first-loaded class wins", coordinate, overlap);
                }
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
                registerJdbcDrivers(jar);
                activeByGa.put(ga, new ActiveArtifact(version, jar));
                appendedPackageRoots.addAll(packageRoots);
                LOGGER.info("Platform dependency [{}] appended to the system classloader from [{}]", coordinate, jar);
                states.add(new PlatformArtifactState(coordinate, STATUS_ACTIVE, "Appended to the system classloader"));
            } catch (IOException e) {
                LOGGER.error("Platform dependency [{}] could not be appended from [{}]", coordinate, jar, e);
                states.add(new PlatformArtifactState(coordinate, STATUS_FAILED, "Could not append: " + e.getMessage()));
            }
        }
        return states;
    }

    /**
     * Whether the jar is already on the launch classpath - matched by file name against the loader.path
     * jars, including the resolved-modules seed's group-prefixed link names.
     *
     * @param jar the artifact jar
     * @return true when a launch-classpath jar carries it
     */
    private boolean onLaunchClasspath(Path jar) {
        String fileName = String.valueOf(jar.getFileName());
        if (launchJarNames.contains(fileName)) {
            return true;
        }
        for (String launchJarName : launchJarNames) {
            if (launchJarName.endsWith("-" + fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The package roots (first two segments) of the jar's classes, for the collision warning.
     *
     * @param jar the jar
     * @return the package roots
     * @throws IOException when the jar is unreadable
     */
    private static Set<String> packageRoots(Path jar) throws IOException {
        Set<String> roots = new LinkedHashSet<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/") || "module-info.class".equals(name)) {
                    continue;
                }
                String[] segments = name.split("/");
                if (segments.length > 2) {
                    roots.add(segments[0] + "/" + segments[1]);
                } else if (segments.length == 2) {
                    roots.add(segments[0]);
                }
            }
        }
        return roots;
    }

    /**
     * Loads the JDBC driver classes an appended jar declares, so they self-register with
     * {@code DriverManager}. Needed because the manager's ServiceLoader scan runs once per JVM -
     * usually long before a driver arrives at runtime - so a driver appended later would otherwise stay
     * invisible until something touches its class.
     *
     * @param jar the appended jar
     */
    private static void registerJdbcDrivers(Path jar) {
        List<String> driverClasses = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry services = jarFile.getJarEntry(JDBC_SERVICES_ENTRY);
            if (services == null) {
                return;
            }
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(jarFile.getInputStream(services), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        driverClasses.add(trimmed);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read the JDBC driver service entries of [{}]", jar, e);
            return;
        }
        for (String driverClass : driverClasses) {
            try {
                // loading through the system classloader runs the driver's static initializer,
                // which registers it with DriverManager
                Class.forName(driverClass, true, ClassLoader.getSystemClassLoader());
                LOGGER.info("JDBC driver [{}] registered from the appended platform dependency", driverClass);
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.warn("JDBC driver [{}] declared by [{}] could not be loaded", driverClass, jar, e);
            }
        }
    }

}
