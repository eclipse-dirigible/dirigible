#!/usr/bin/env node
//
// Cross-platform (macOS / Linux / Windows) driver for the local Dirigible dev loop.
// Node 22+ is a hard build prerequisite of this project, so it is available everywhere.
//
// Usage:
//   node .claude/scripts/dirigible.mjs build [quick|full]
//   node .claude/scripts/dirigible.mjs start [--debug]
//   node .claude/scripts/dirigible.mjs stop
//   node .claude/scripts/dirigible.mjs test [all|unit|integration]
//
//   build : quick -> mvn -T 1C clean install -P quick-build (default; no tests/javadoc/license/format)
//           full  -> mvn clean install (all unit tests)
//   start : launch the fat jar in the background, record PID + log, poll readiness.
//           --debug enables JDWP on port 8000 (transport=dt_socket,server=y,suspend=n).
//   stop  : terminate the instance recorded in the PID file.
//   test  : run the test suites (clean install). all (default) -> -P tests; unit -> -P unit-tests;
//           integration -> -P integration-tests. Runs headless when integration tests are involved.

import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { closeSync, existsSync, mkdirSync, openSync, readdirSync, readFileSync, readSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { setTimeout as sleep } from 'node:timers/promises';

const IS_WINDOWS = process.platform === 'win32';
const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(SCRIPT_DIR, '..', '..');
const RUN_DIR = join(REPO_ROOT, '.claude', 'run');
const PID_FILE = join(RUN_DIR, 'dirigible.pid');
const LOG_FILE = join(RUN_DIR, 'dirigible.log');
const PORT = process.env.DIRIGIBLE_SERVER_PORT || '8080';
const READINESS_URL = `http://localhost:${PORT}/actuator/health/readiness`;

function ts() {
  return new Date().toLocaleTimeString('en-GB', { hour12: false }); // HH:MM:SS
}

// Timestamped progress line so the user can see what the command is doing right now.
function log(message) {
  console.log(`>> [${ts()}] ${message}`);
}

function elapsed(startedAtMs) {
  const s = Math.round((Date.now() - startedAtMs) / 1000);
  return s >= 60 ? `${Math.floor(s / 60)}m ${s % 60}s` : `${s}s`;
}

function fail(message) {
  console.error(`>> [${ts()}] ${message}`);
  process.exit(1);
}

// --- maven ----------------------------------------------------------------

function runMaven(args) {
  // shell:true lets Windows resolve `mvn` -> `mvn.cmd` via PATHEXT; POSIX uses the PATH binary.
  const result = spawnSync('mvn', args, { cwd: REPO_ROOT, stdio: 'inherit', shell: true });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

// By default we do NOT `clean`, so the runtime state the app keeps under
// ./target/dirigible (H2 DB + on-disk repository) survives a rebuild. Pass
// clean=true to wipe target/ and start from a fresh DB/repository.
function build(profile = 'quick', clean = false) {
  const startedAt = Date.now();
  const cleanGoal = clean ? ['clean'] : [];
  let args;
  if (profile === 'quick') {
    log(`Build: quick-build${clean ? ', clean' : ''} (skips tests, javadoc, license, formatting).`);
    args = ['-T', '1C', ...cleanGoal, 'install', '-P', 'quick-build'];
  } else if (profile === 'full') {
    log(`Build: full${clean ? ', clean' : ''} (runs all unit tests).`);
    args = [...cleanGoal, 'install'];
  } else {
    fail(`Unknown profile '${profile}' (expected 'quick' or 'full')`);
  }
  if (clean) {
    log('Clean requested — wiping target/, including the runtime H2 DB and repository under ./target/dirigible.');
  }
  log(`Running: mvn ${args.join(' ')}`);
  log('This can take several minutes — Maven output follows.');
  runMaven(args);
  log(`Build finished in ${elapsed(startedAt)}.`);
}

// --- test -----------------------------------------------------------------

// Mirrors the maven invocations CI runs (.github/workflows/build.yml). `clean` is
// always included: it matches CI and wipes the stale file-backed H2 under
// tests/tests-integrations/target/dirigible that otherwise breaks IT re-runs.
// Headless is on whenever integration tests run (Selenide drives Chrome).
function test(mode = 'all') {
  const startedAt = Date.now();
  if (mode === 'it') {
    mode = 'integration';
  }
  let label, args;
  if (mode === 'all') {
    label = 'all tests (unit + integration)';
    args = ['clean', 'install', '-P', 'tests', '-D', 'selenide.headless=true'];
  } else if (mode === 'unit') {
    label = 'unit tests only';
    args = ['clean', 'install', '-P', 'unit-tests'];
  } else if (mode === 'integration') {
    label = 'integration tests only';
    args = ['clean', 'install', '-P', 'integration-tests', '-D', 'selenide.headless=true'];
  } else {
    fail(`Unknown test mode '${mode}' (expected 'all', 'unit', or 'integration')`);
  }
  log(`Running ${label}.`);
  if (mode !== 'unit') {
    log('Integration tests boot the full app and drive headless Chrome — this can take a long time; ttyd must be installed.');
  }
  log(`Running: mvn ${args.join(' ')}`);
  log('This can take many minutes — Maven output follows.');
  // shell:true so Windows resolves `mvn` -> `mvn.cmd` via PATHEXT; POSIX uses the PATH binary.
  const result = spawnSync('mvn', args, { cwd: REPO_ROOT, stdio: 'inherit', shell: true });
  if (result.status === 0) {
    log(`Tests PASSED in ${elapsed(startedAt)}.`);
  } else {
    log(`Tests FAILED after ${elapsed(startedAt)} (mvn exit ${result.status}).`);
    if (mode !== 'unit') {
      log('Selenide screenshots/reports: tests/tests-integrations/build/reports/tests');
    }
    process.exit(result.status ?? 1);
  }
}

// --- process helpers ------------------------------------------------------

function isAlive(pid) {
  try {
    process.kill(pid, 0); // signal 0 only probes existence
    return true;
  } catch (err) {
    return err.code === 'EPERM'; // exists but owned by another user
  }
}

function resolveJar() {
  const targetDir = join(REPO_ROOT, 'build', 'application', 'target');
  if (!existsSync(targetDir)) return null;
  const jars = readdirSync(targetDir)
    .filter((name) => /^dirigible-application-.*-executable\.jar$/.test(name))
    .map((name) => join(targetDir, name))
    .sort((a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs); // newest first
  return jars[0] ?? null;
}

// --- start ----------------------------------------------------------------

async function start(debug) {
  log(`Starting Dirigible${debug ? ' in debug mode' : ''}...`);
  mkdirSync(RUN_DIR, { recursive: true });

  log('Checking for an already-running instance...');
  if (existsSync(PID_FILE)) {
    const oldPid = Number(readFileSync(PID_FILE, 'utf8').trim());
    if (oldPid && isAlive(oldPid)) {
      fail(`Dirigible already running (PID ${oldPid}). Stop it first.`);
    }
    log('Found a stale PID file — clearing it.');
    rmSync(PID_FILE, { force: true });
  }

  log('Resolving the executable jar...');
  const jar = resolveJar();
  if (!jar) {
    fail('No executable jar under build/application/target/. Build first.');
  }
  log(`Using jar: ${jar}`);

  const javaArgs = [];
  if (debug) {
    javaArgs.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000');
  }
  javaArgs.push('-jar', jar);

  log(`Launching the JVM${debug ? ' with JDWP on port 8000' : ''}...`);
  log(`Starting a fresh log at ${LOG_FILE}.`);
  const out = openSync(LOG_FILE, 'w'); // truncate: each start gets a fresh log
  const child = spawn('java', javaArgs, {
    cwd: REPO_ROOT,
    detached: true, // own process group so it survives this Node process
    stdio: ['ignore', out, out]
  });
  child.unref();
  writeFileSync(PID_FILE, String(child.pid));

  log(`Dirigible process started (PID ${child.pid}).`);
  console.log(`   Log: ${LOG_FILE}`);
  console.log(`   UI:  http://localhost:${PORT}  (admin/admin)`);
  if (debug) {
    console.log('   JDWP debug port: 8000 (attach transport=dt_socket, suspend=n)');
  }

  await waitForReadiness(child.pid);
}

async function waitForReadiness(pid, timeoutMs = 180_000) {
  const startedAt = Date.now();
  const deadline = startedAt + timeoutMs;
  let lastBeat = startedAt;
  log(`Waiting for the server to become ready (polling ${READINESS_URL})...`);
  while (Date.now() < deadline) {
    if (!isAlive(pid)) {
      fail(`Process ${pid} exited during startup. Check the log: ${LOG_FILE}`);
    }
    try {
      const res = await fetch(READINESS_URL);
      if (res.ok) {
        log(`Server is ready after ${elapsed(startedAt)} (${READINESS_URL} -> ${res.status}).`);
        return;
      }
    } catch {
      // not up yet
    }
    if (Date.now() - lastBeat >= 10_000) {
      log(`...still booting (${elapsed(startedAt)} elapsed)`);
      lastBeat = Date.now();
    }
    await sleep(2000);
  }
  log(`Timed out after ${timeoutMs / 1000}s waiting for readiness. It may still be booting — check ${LOG_FILE}.`);
}

// --- stop -----------------------------------------------------------------

async function stop() {
  log('Stopping Dirigible...');
  if (!existsSync(PID_FILE)) {
    log('No PID file — Dirigible is not running (started via this script).');
    return;
  }
  const pid = Number(readFileSync(PID_FILE, 'utf8').trim());
  if (!pid || !isAlive(pid)) {
    log(`Recorded process (PID ${pid || '?'}) is not alive. Removing stale PID file.`);
    rmSync(PID_FILE, { force: true });
    return;
  }

  if (IS_WINDOWS) {
    log(`Terminating process tree of PID ${pid} (taskkill /T /F)...`);
    // No real graceful signal on Windows; terminate the tree forcefully.
    spawnSync('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    log(`Sending SIGTERM to PID ${pid} and waiting up to 15s for graceful shutdown...`);
    try {
      process.kill(pid, 'SIGTERM');
    } catch {
      // already gone
    }
    for (let i = 0; i < 15 && isAlive(pid); i++) {
      await sleep(1000);
    }
    if (isAlive(pid)) {
      log('Still alive after 15s — sending SIGKILL.');
      try {
        process.kill(pid, 'SIGKILL');
      } catch {
        // already gone
      }
    }
  }
  rmSync(PID_FILE, { force: true });
  log('Stopped.');
}

// --- logs -----------------------------------------------------------------

function formatStamp(d) {
  const p = (n, w = 2) => String(n).padStart(w, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${p(d.getMilliseconds(), 3)}`;
}

// Select the lines logged within the last `minutes`. The server's timestamp
// (`YYYY-MM-DD HH:MM:SS.mmm`, fixed-width, local time) is lexicographically ordered
// the same as chronologically, so we compare the timestamp prefix as a STRING
// against a formatted cutoff — far faster than `new Date()` per line on a big log.
// Continuation lines without a timestamp inherit the previous line's verdict so
// multi-line entries (stack traces) stay intact.
function linesSince(allLines, minutes) {
  const cutoff = formatStamp(new Date(Date.now() - minutes * 60_000));
  const stampRe = /^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})/;
  const selected = [];
  let including = false;
  for (const line of allLines) {
    const m = line.match(stampRe);
    if (m) {
      including = m[1] >= cutoff;
    }
    if (including) {
      selected.push(line);
    }
  }
  return selected;
}

// Cross-platform `tail -f` for the background server log. Used standalone and as
// the auto-follower launched by start/debug; runs until the server stops or it is
// killed. Built on fs primitives only — no `tail`, which Windows lacks.
// Snapshot by default (print recent lines and exit) — that output reliably shows
// up in the Claude session. With follow=true it tails for a bounded window so a
// FOREGROUND run still returns the captured output instead of hanging forever.
async function logs({ lines = 80, sinceMinutes = 0, follow = false, seconds = 60 } = {}) {
  if (!existsSync(LOG_FILE)) {
    log('Waiting for the log file to appear (is the server starting?)...');
  }
  for (let i = 0; i < 30 && !existsSync(LOG_FILE); i++) {
    await sleep(1000); // server may still be booting
  }
  if (!existsSync(LOG_FILE)) {
    fail(`No log file at ${LOG_FILE}. Start Dirigible first.`);
  }

  // Backlog is either the last N lines or everything logged in the last M minutes.
  const existing = readFileSync(LOG_FILE, 'utf8');
  const existingLines = existing.split('\n');
  let backlog;
  if (sinceMinutes > 0) {
    backlog = linesSince(existingLines, sinceMinutes);
    log(`Log entries from the last ${sinceMinutes} min of ${LOG_FILE}:`);
    if (backlog.length === 0) {
      console.log(`   (no entries in the last ${sinceMinutes} min — the server may be idle)`);
    }
  } else {
    backlog = existingLines.slice(Math.max(0, existingLines.length - lines - 1));
    log(`Last ${lines} lines of ${LOG_FILE}:`);
  }
  process.stdout.write(backlog.join('\n'));
  let offset = Buffer.byteLength(existing);

  if (!follow) {
    console.log('');
    log('Snapshot only — re-run /dirigible-logs to refresh, or use follow mode to watch live.');
    return;
  }

  const unbounded = seconds <= 0; // --seconds 0 → follow until the server stops (or killed)
  const deadline = Date.now() + seconds * 1000;
  console.log('');
  log(
    unbounded
      ? 'Following live until the server stops (or this command is stopped)...'
      : `Following live for up to ${seconds}s (the server keeps running afterwards)...`
  );

  while (unbounded || Date.now() < deadline) {
    await sleep(700);

    if (existsSync(PID_FILE)) {
      const pid = Number(readFileSync(PID_FILE, 'utf8').trim());
      if (pid && !isAlive(pid)) {
        console.log('');
        log('Server process is no longer running — detaching log follower.');
        return;
      }
    }

    let size;
    try {
      size = statSync(LOG_FILE).size;
    } catch {
      continue; // file briefly unavailable (rotation) — retry next tick
    }
    if (size < offset) {
      offset = 0; // truncated or rotated
    }
    if (size > offset) {
      const fd = openSync(LOG_FILE, 'r');
      try {
        const buf = Buffer.alloc(size - offset);
        const read = readSync(fd, buf, 0, buf.length, offset);
        process.stdout.write(buf.subarray(0, read).toString('utf8'));
        offset += read;
      } finally {
        closeSync(fd);
      }
    }
  }
  console.log('');
  log(`Stopped following after ${seconds}s (server still running). Re-run /dirigible-logs to continue.`);
}

function parseLogsOpts(rest) {
  const opts = { lines: 80, sinceMinutes: 0, follow: false, seconds: 60 };
  const li = rest.indexOf('--lines');
  if (li !== -1 && rest[li + 1]) {
    const n = Number(rest[li + 1]);
    if (Number.isInteger(n) && n > 0) {
      opts.lines = n;
    }
  }
  const mi = rest.indexOf('--since');
  if (mi !== -1 && rest[mi + 1]) {
    const n = Number(rest[mi + 1]);
    if (Number.isInteger(n) && n > 0) {
      opts.sinceMinutes = n; // minutes; takes precedence over --lines for the backlog
    }
  }
  if (rest.includes('--follow') || rest.includes('follow')) {
    opts.follow = true;
  }
  const si = rest.indexOf('--seconds');
  if (si !== -1 && rest[si + 1]) {
    const n = Number(rest[si + 1]);
    if (Number.isInteger(n) && n >= 0) {
      opts.seconds = n; // 0 = unbounded follow
    }
  }
  return opts;
}

// --- dispatch -------------------------------------------------------------

const [command, ...rest] = process.argv.slice(2);
switch (command) {
  case 'build': {
    const profile = rest.find((a) => a === 'quick' || a === 'full') || 'quick';
    build(profile, rest.includes('--clean'));
    break;
  }
  case 'start':
    await start(rest.includes('--debug'));
    break;
  case 'stop':
    await stop();
    break;
  case 'logs':
    await logs(parseLogsOpts(rest));
    break;
  case 'test':
    // Pass the raw mode through (default 'all') so an unrecognized arg fails fast
    // in test() instead of silently falling back to running the whole suite.
    test(rest[0] || 'all');
    break;
  default:
    fail(`Unknown command '${command ?? ''}'. Use: build [quick|full] [--clean] | start [--debug] | stop | logs [--lines N | --since MIN] [--follow] [--seconds N] | test [all|unit|integration]`);
}
