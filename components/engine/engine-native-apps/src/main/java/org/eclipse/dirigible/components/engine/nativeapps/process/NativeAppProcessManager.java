/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Command;
import org.eclipse.dirigible.components.engine.nativeapps.domain.CommandArgument;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Lifecycle;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.util.LogSanitizer;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Owns the OS processes of all local {@link NativeApp native apps}.
 *
 * <p>
 * State is kept in-process; on JVM restart everything is rediscovered from the registry. The only
 * place that interacts with {@link ProcessBuilder}.
 */
@Component
public class NativeAppProcessManager {

    public static final String NATIVE_APP_PORT_ENV = "DIRIGIBLE_NATIVE_APP_PORT";
    public static final String NATIVE_APP_PID_ENV = "DIRIGIBLE_NATIVE_APP_PID";

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppProcessManager.class);

    private static final long READY_POLL_INTERVAL_MS = 200L;
    private static final long STOP_GRACE_MS = 5_000L;
    private static final long STOP_COMMAND_TIMEOUT_MS = 10_000L;

    private final IRepository repository;
    private final ConcurrentHashMap<Long, RuntimeState> states = new ConcurrentHashMap<>();

    public NativeAppProcessManager(IRepository repository) {
        this.repository = repository;
    }

    /** True iff {@code app} is LOCAL and an owned process is currently alive. */
    public boolean isAlive(NativeApp app) {
        if (app == null || app.getKind() != NativeAppKind.LOCAL) {
            return false;
        }
        RuntimeState state = states.get(app.getId());
        return state != null && state.isAlive();
    }

    public Optional<RuntimeState> getState(NativeApp app) {
        if (app == null || app.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(states.get(app.getId()));
    }

    /**
     * Starts the process for {@code app} (or returns if already running) and blocks until the process
     * is accepting TCP connections on the resolved port. Thread-safe per app id.
     */
    public synchronized RuntimeState startAndAwaitReady(NativeApp app) {
        if (app.getKind() != NativeAppKind.LOCAL) {
            throw new IllegalArgumentException("Only LOCAL native apps can be started: " + app);
        }
        RuntimeState existing = states.get(app.getId());
        if (existing != null && existing.isAlive()) {
            return existing;
        }
        RuntimeState started = doStart(app);
        awaitReady(app, started);
        return started;
    }

    /** Best-effort fire-and-forget start. Errors are logged. */
    public void startAsync(NativeApp app) {
        Thread t = new Thread(() -> {
            try {
                startAndAwaitReady(app);
            } catch (RuntimeException ex) {
                LOGGER.error("Failed to start native app [{}]: {}", app.getName(), ex.getMessage(), ex);
            }
        }, "native-app-start-" + app.getName());
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop(NativeApp app) {
        if (app == null || app.getId() == null) {
            return;
        }
        RuntimeState state = states.remove(app.getId());
        if (state == null) {
            return;
        }
        Process p = state.getProcess();
        long pid = (p != null) ? p.pid() : -1L;
        LOGGER.info("Stopping native app [{}]: PID [{}], port [{}].", LogSanitizer.sanitize(app.getName()), pid, state.getPort());
        // Snapshot the descendant tree FIRST — before anything kills the held root. Two things can
        // break the parent link, after which descendants() returns an empty list:
        // (1) the author's lifecycle.stop targets $DIRIGIBLE_NATIVE_APP_PID directly (the safe
        // pattern this platform encourages), so npm/sh dies before we get a chance to walk;
        // (2) the platform's own Process.destroy() below.
        // Shell-based launchers commonly produce chains like `sh → npm → sh → node`, and SIGTERM
        // to the held root does not always propagate (especially when `exec` rewrites the held PID
        // into npm, which then forks a fresh sh+node pair that survives the npm exit). After the
        // root dies the OS reparents the orphans to init/launchd and they are no longer reachable
        // as descendants of the held handle.
        List<ProcessHandle> descendants = (p != null) ? p.toHandle()
                                                         .descendants()
                                                         .toList()
                : List.of();
        runStopCommandsBestEffort(app, state.getPort(), pid);
        if (p == null) {
            killLeftoverDescendants(app, descendants);
            return;
        }
        p.destroy();
        try {
            if (!p.waitFor(STOP_GRACE_MS, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Native app [{}] (PID [{}]) did not stop within {}ms; forcibly destroying.",
                        LogSanitizer.sanitize(app.getName()), pid, STOP_GRACE_MS);
                p.destroyForcibly();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread()
                  .interrupt();
            p.destroyForcibly();
        }
        killLeftoverDescendants(app, descendants);
    }

    /**
     * SIGKILLs any descendants still alive after the root process exits — handles the case where the
     * held root (typically a shell or npm) failed to propagate SIGTERM through to the actual server
     * child. Going straight to {@link ProcessHandle#destroyForcibly()} (SIGKILL) is the right call
     * here: the root already had its chance to clean up gracefully and didn't propagate.
     */
    private void killLeftoverDescendants(NativeApp app, List<ProcessHandle> descendants) {
        for (ProcessHandle d : descendants) {
            if (!d.isAlive()) {
                continue;
            }
            String command = d.info()
                              .command()
                              .orElse("<unknown>");
            LOGGER.info("Killing leftover descendant of native app [{}]: PID [{}], command [{}].", LogSanitizer.sanitize(app.getName()),
                    d.pid(), LogSanitizer.sanitize(command));
            d.destroyForcibly();
        }
    }

    public void stopAll() {
        for (Long id : new ArrayList<>(states.keySet())) {
            RuntimeState state = states.remove(id);
            if (state == null || state.getProcess() == null) {
                continue;
            }
            Process proc = state.getProcess();
            List<ProcessHandle> descendants = proc.toHandle()
                                                  .descendants()
                                                  .toList();
            proc.destroy();
            for (ProcessHandle d : descendants) {
                if (d.isAlive()) {
                    d.destroyForcibly();
                }
            }
        }
    }

    private RuntimeState doStart(NativeApp app) {
        NativeAppConfig config = app.getConfig();
        if (config == null || config.getLifecycle() == null || config.getLifecycle()
                                                                     .getStart() == null) {
            throw new IllegalStateException("Native app [" + app.getName() + "] has no lifecycle.start");
        }
        Command command = OsCommandResolver.pickForCurrentOs(config.getLifecycle()
                                                                   .getStart()
                                                                   .getCommands())
                                           .orElseThrow(() -> new IllegalStateException("Native app [" + app.getName()
                                                   + "] has no start command for OS [" + OsCommandResolver.currentOs() + "]"));

        int port = PortResolver.resolve(app.getDefaultPort());
        File workingDir = resolveWorkingDir(app, command);
        ProcessBuilder pb = new ProcessBuilder(buildCommandTokens(command));
        pb.directory(workingDir);
        pb.redirectErrorStream(false);
        Map<String, String> env = pb.environment();
        env.put(NATIVE_APP_PORT_ENV, Integer.toString(port));

        // INFO logs just the executable + arg count to avoid leaking secrets that an author may
        // have embedded in command arguments. Full sanitized command is at DEBUG for diagnostics.
        List<String> commandTokens = pb.command();
        LOGGER.info("Starting native app [{}] in [{}] on port [{}] with executable [{}] and [{}] argument(s)",
                LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(workingDir.getAbsolutePath()), port,
                LogSanitizer.sanitize(commandTokens.isEmpty() ? "" : commandTokens.get(0)), Math.max(0, commandTokens.size() - 1));
        LOGGER.debug("Native app [{}] full start command: {}", LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(commandTokens));

        Process process;
        try {
            process = pb.start();
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to start native app [" + app.getName() + "] in [" + workingDir.getAbsolutePath() + "]: " + ex.getMessage(), ex);
        }
        // Log the PID alongside the port so the operator can correlate later (e.g.
        // `lsof -ti tcp:<port>` / `ps -p <pid>`). The held PID is the immediate child Dirigible
        // spawned — for shell-based launchers this is the wrapper, and the real listener may be
        // a grandchild reached via `exec`. Either way, the OS process tree under this PID covers it.
        LOGGER.info("Native app [{}] spawned: PID [{}], port [{}].", LogSanitizer.sanitize(app.getName()), process.pid(), port);
        // Build the state first so the stderr pump can tee lines into its ring buffer — the
        // awaitReady failure path inlines the most recent stderr lines into the exception message
        // so operators don't have to chase per-app child loggers to find out what went wrong.
        RuntimeState state = new RuntimeState(process, port, Instant.now(), workingDir);
        ProcessLogPump.start(app.getName(), process, state::recordStderrLine);
        states.put(app.getId(), state);
        return state;
    }

    private void awaitReady(NativeApp app, RuntimeState state) {
        long deadline = System.currentTimeMillis() + readyTimeoutMs();
        while (System.currentTimeMillis() < deadline) {
            if (!state.isAlive()) {
                // Give the stderr pump a brief moment to flush so the message includes the actual
                // failure lines from the child rather than an empty tail. The drain thread reads
                // line-by-line from a closed pipe; under typical Linux/macOS scheduling it finishes
                // within a few ms of the process exit.
                drainStderrBriefly();
                states.remove(app.getId());
                throw new IllegalStateException(buildPrematureExitMessage(app, state));
            }
            if (isLoopbackPortOpen(state.getPort())) {
                LOGGER.info("Native app [{}] is ready: PID [{}], port [{}].", app.getName(), state.getProcess()
                                                                                                  .pid(),
                        state.getPort());
                return;
            }
            try {
                Thread.sleep(READY_POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException("Interrupted while waiting for native app [" + app.getName() + "] to become ready.", ie);
            }
        }
        state.getProcess()
             .destroy();
        states.remove(app.getId());
        throw new IllegalStateException(buildReadyTimeoutMessage(app, state));
    }

    private static void drainStderrBriefly() {
        try {
            Thread.sleep(READY_POLL_INTERVAL_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread()
                  .interrupt();
        }
    }

    /**
     * Builds the IllegalStateException message used when a spawned native-app process exits before its
     * TCP port becomes reachable. Includes the exit code, elapsed runtime, port, working directory and
     * the most recent stderr lines captured by {@link ProcessLogPump} — everything an operator needs to
     * diagnose without chasing per-app child loggers.
     */
    private static String buildPrematureExitMessage(NativeApp app, RuntimeState state) {
        Process p = state.getProcess();
        long pid = p.pid();
        int exitCode;
        try {
            exitCode = p.exitValue();
        } catch (IllegalThreadStateException ex) {
            exitCode = Integer.MIN_VALUE;
        }
        long elapsedMs = Duration.between(state.getStartedAt(), Instant.now())
                                 .toMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("Native app [")
          .append(app.getName())
          .append("] (PID ")
          .append(pid)
          .append(") exited prematurely after ")
          .append(elapsedMs)
          .append(" ms")
          .append(exitCode == Integer.MIN_VALUE ? "" : " with exit code " + exitCode)
          .append(", before its TCP port [")
          .append(state.getPort())
          .append("] became reachable. Working directory: [")
          .append(state.getWorkingDir()
                       .getAbsolutePath())
          .append("]. ");
        String tail = state.snapshotRecentStderr();
        if (tail.isEmpty()) {
            sb.append("No stderr was captured before the process exited — check that the configured executable exists on PATH.");
        } else {
            sb.append("Last stderr lines from the child:\n")
              .append(indent(tail));
        }
        sb.append("\nFull live child stdout/stderr stream into loggers 'org.eclipse.dirigible.nativeapps.")
          .append(app.getName())
          .append(".stdout' and '.stderr'.");
        return sb.toString();
    }

    private static String buildReadyTimeoutMessage(NativeApp app, RuntimeState state) {
        return "Native app [" + app.getName() + "] (PID " + state.getProcess()
                                                                 .pid()
                + ") did not become ready within " + readyTimeoutMs() + " ms; expected a TCP listener on port [" + state.getPort()
                + "]. Working directory: [" + state.getWorkingDir()
                                                   .getAbsolutePath()
                + "]. Verify that the spawned process reads the " + NATIVE_APP_PORT_ENV
                + " env var and binds to it. Bump DIRIGIBLE_NATIVE_APP_READY_TIMEOUT_MS if a cold-start phase legitimately needs longer "
                + "(e.g. first-time `npm install`). Live child output streams into loggers 'org.eclipse.dirigible.nativeapps."
                + app.getName() + ".stdout' and '.stderr'.";
    }

    private static String indent(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            out.append("    ")
               .append(line)
               .append('\n');
        }
        return out.toString();
    }

    /**
     * Runs the configured {@code lifecycle.stop} command for the current OS, if any. Returns silently
     * (with diagnostic logs) when no stop command applies — the caller still falls back to
     * {@link Process#destroy()} / {@link Process#destroyForcibly()}, so the process always exits.
     *
     * <p>
     * Two contract env vars are exported on the stop subprocess:
     * <ul>
     * <li>{@value #NATIVE_APP_PORT_ENV} — the resolved port (symmetric with the start command). Without
     * this, an author's stop script that reads the env var with a fallback like {@code ${PORT:-8080}}
     * could resolve to the platform's own port and accidentally signal the Dirigible JVM.</li>
     * <li>{@value #NATIVE_APP_PID_ENV} — the held root PID of the spawned process. Authors who need a
     * custom stop script should target only this PID (e.g. {@code kill "$DIRIGIBLE_NATIVE_APP_PID"})
     * rather than enumerate PIDs by port (e.g. {@code lsof -ti tcp:"$PORT" | xargs kill}). The latter
     * matches every PID with a TCP FD on the port, which after a proxy round-trip includes the
     * Dirigible JVM itself via Spring Cloud Gateway's HttpClient keep-alive — sending SIGTERM to that
     * PID brings the platform down.</li>
     * </ul>
     */
    private void runStopCommandsBestEffort(NativeApp app, int port, long pid) {
        NativeAppConfig config = app.getConfig();
        if (config == null) {
            LOGGER.debug("No stop command run for native app [{}]: typed config is unavailable.", app.getName());
            return;
        }
        Lifecycle lifecycle = config.getLifecycle();
        if (lifecycle == null || lifecycle.getStop() == null) {
            LOGGER.debug("No stop command run for native app [{}]: lifecycle.stop is not declared.", app.getName());
            return;
        }
        Optional<Command> stopCmd = OsCommandResolver.pickForCurrentOs(lifecycle.getStop()
                                                                                .getCommands());
        if (stopCmd.isEmpty()) {
            LOGGER.info("Native app [{}] has lifecycle.stop but no command entry for OS [{}]; "
                    + "falling back to Process.destroy(). Add an [os: {}] entry to lifecycle.stop.commands to run a custom stop script.",
                    app.getName(), OsCommandResolver.currentOs(), OsCommandResolver.currentOs());
            return;
        }
        Command command = stopCmd.get();
        // If the project tree is already gone (the workbench's "unpublish" deletes it before the
        // synchronizer fires DELETE), the stop script almost certainly can't work — it typically
        // reads package.json / node_modules from there. Skip and let Process.destroy() (followed
        // by destroyForcibly() in stop()) terminate the spawned chain instead.
        File projectRoot = resolveProjectRoot(app);
        if (!projectRoot.exists()) {
            LOGGER.info(
                    "Skipping lifecycle.stop for native app [{}] on port [{}]: project directory [{}] no longer exists (files already removed); terminating via Process.destroy() instead.",
                    LogSanitizer.sanitize(app.getName()), port, LogSanitizer.sanitize(projectRoot.getAbsolutePath()));
            return;
        }
        File workingDir = resolveWorkingDir(app, command);
        ProcessBuilder pb = new ProcessBuilder(buildCommandTokens(command));
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Map<String, String> stopEnv = pb.environment();
        stopEnv.put(NATIVE_APP_PORT_ENV, Integer.toString(port));
        if (pid > 0) {
            stopEnv.put(NATIVE_APP_PID_ENV, Long.toString(pid));
        }
        List<String> stopTokens = pb.command();
        LOGGER.info("Running stop command for native app [{}] in [{}]: executable [{}] with [{}] argument(s)",
                LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(workingDir.getAbsolutePath()),
                LogSanitizer.sanitize(stopTokens.isEmpty() ? "" : stopTokens.get(0)), Math.max(0, stopTokens.size() - 1));
        LOGGER.debug("Native app [{}] full stop command: {}", LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(stopTokens));
        try {
            Process p = pb.start();
            ProcessLogPump.start(app.getName() + ".stop", p, null);
            if (!p.waitFor(STOP_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Stop command for native app [{}] did not finish within {}ms; forcibly destroying.", app.getName(),
                        STOP_COMMAND_TIMEOUT_MS);
                p.destroyForcibly();
            } else {
                LOGGER.debug("Stop command for native app [{}] exited with code [{}].", app.getName(), p.exitValue());
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread()
                      .interrupt();
            }
            LOGGER.warn("Stop command for native app [{}] failed: {}", app.getName(), ex.getMessage());
        }
    }

    /**
     * Resolves the on-disk working directory for {@code command}.
     *
     * <p>
     * The base is the artefact's own <strong>project root</strong> (derived from
     * {@link NativeApp#getLocation()}). A null / blank / {@code "."} {@code command.dir} resolves to
     * the project root itself; otherwise it's joined as a subpath. Authors should not embed the project
     * folder name into {@code dir} — the platform knows it from the artefact location, and embedding it
     * makes the artefact brittle to repo renames or clones-under-a-different-name.
     *
     * <p>
     * If the explicitly configured subpath doesn't exist on disk, a WARN is logged with a concrete
     * remediation hint and the project root is used as a graceful fallback.
     */
    private File resolveWorkingDir(NativeApp app, Command command) {
        File projectRoot = resolveProjectRoot(app);
        String dir = command.getDir();
        if (dir == null || dir.isBlank() || ".".equals(dir.trim())) {
            return projectRoot;
        }
        File resolved = new File(projectRoot, dir);
        if (!resolved.exists()) {
            LOGGER.warn(
                    "Native app [{}] declares command.dir [{}] resolving to [{}] which does not exist on disk; "
                            + "falling back to the project root [{}]. The platform resolves command.dir relative to the artefact's "
                            + "project root — drop command.dir if your start command should run at the project root, or set it "
                            + "to a subpath that exists. (Common cause: command.dir was set to the project folder name; that's "
                            + "no longer needed and breaks when the project is cloned under a different folder.)",
                    LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(dir), LogSanitizer.sanitize(resolved.getAbsolutePath()),
                    LogSanitizer.sanitize(projectRoot.getAbsolutePath()));
            return projectRoot;
        }
        return resolved;
    }

    /**
     * Returns the on-disk project root for {@code app} — the directory under {@code /registry/public/}
     * whose name is the first path segment of {@link NativeApp#getLocation()}.
     */
    private File resolveProjectRoot(NativeApp app) {
        String location = app.getLocation();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "Native app [" + app.getName() + "] has no location; cannot resolve project root for the working directory.");
        }
        Path locationPath = Path.of(location);
        if (locationPath.getNameCount() == 0) {
            throw new IllegalStateException("Native app [" + app.getName() + "] has location [" + location
                    + "] with no project segment; cannot resolve project root.");
        }
        String projectName = locationPath.getName(0)
                                         .toString();
        File publicRoot = new File(repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC));
        return new File(publicRoot, projectName);
    }

    private static List<String> buildCommandTokens(Command command) {
        List<String> tokens = new ArrayList<>();
        if (command.getExecutable() != null && !command.getExecutable()
                                                       .isBlank()) {
            for (String t : command.getExecutable()
                                   .trim()
                                   .split("\\s+")) {
                if (!t.isEmpty()) {
                    tokens.add(t);
                }
            }
        }
        for (CommandArgument arg : command.getArguments()) {
            if (arg.getName() != null && !arg.getName()
                                             .isBlank()) {
                tokens.add(arg.getName());
            }
            if (arg.getValue() != null && !arg.getValue()
                                              .isBlank()) {
                tokens.add(arg.getValue());
            }
        }
        return tokens;
    }

    private static long readyTimeoutMs() {
        return DirigibleConfig.NATIVE_APP_READY_TIMEOUT_MS.getIntValue();
    }

    private static boolean isLoopbackPortOpen(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 250);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
