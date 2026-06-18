#!/usr/bin/env node
//
// Cross-platform (macOS / Linux / Windows) driver for the local Dirigible dev loop.
// Node 22+ is a hard build prerequisite of this project, so it is available everywhere.
//
// Usage:
//   node .claude/scripts/dirigible.mjs build [quick|full]
//   node .claude/scripts/dirigible.mjs start [--debug]
//   node .claude/scripts/dirigible.mjs stop
//
//   build : quick -> mvn -T 1C clean install -P quick-build (default; no tests/javadoc/license/format)
//           full  -> mvn clean install (all unit tests)
//   start : launch the fat jar in the background, record PID + log, poll readiness.
//           --debug enables JDWP on port 8000 (transport=dt_socket,server=y,suspend=n).
//   stop  : terminate the instance recorded in the PID file.

import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { existsSync, mkdirSync, openSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { setTimeout as sleep } from 'node:timers/promises';

const IS_WINDOWS = process.platform === 'win32';
const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(SCRIPT_DIR, '..', '..');
const RUN_DIR = join(REPO_ROOT, '.claude', 'run');
const PID_FILE = join(RUN_DIR, 'dirigible.pid');
const LOG_FILE = join(RUN_DIR, 'dirigible.log');
const PORT = process.env.DIRIGIBLE_SERVER_PORT || '8080';
const READINESS_URL = `http://localhost:${PORT}/actuator/health/readiness`;

function fail(message) {
  console.error(`>> ${message}`);
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

function build(profile = 'quick') {
  if (profile === 'quick') {
    console.log('>> Building Dirigible (quick-build: no tests/javadoc/license/format)');
    runMaven(['-T', '1C', 'clean', 'install', '-P', 'quick-build']);
  } else if (profile === 'full') {
    console.log('>> Building Dirigible (full: all unit tests)');
    runMaven(['clean', 'install']);
  } else {
    fail(`Unknown profile '${profile}' (expected 'quick' or 'full')`);
  }
  console.log('>> Build finished.');
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
  mkdirSync(RUN_DIR, { recursive: true });

  if (existsSync(PID_FILE)) {
    const oldPid = Number(readFileSync(PID_FILE, 'utf8').trim());
    if (oldPid && isAlive(oldPid)) {
      fail(`Dirigible already running (PID ${oldPid}). Stop it first.`);
    }
    rmSync(PID_FILE, { force: true }); // stale
  }

  const jar = resolveJar();
  if (!jar) {
    fail('No executable jar under build/application/target/. Build first.');
  }

  const javaArgs = [];
  if (debug) {
    javaArgs.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000');
  }
  javaArgs.push('-jar', jar);

  console.log(`>> Starting: ${jar}`);
  const out = openSync(LOG_FILE, 'a');
  const child = spawn('java', javaArgs, {
    cwd: REPO_ROOT,
    detached: true, // own process group so it survives this Node process
    stdio: ['ignore', out, out]
  });
  child.unref();
  writeFileSync(PID_FILE, String(child.pid));

  console.log(`>> Dirigible started (PID ${child.pid})`);
  console.log(`   Log: ${LOG_FILE}`);
  console.log(`   UI:  http://localhost:${PORT}  (admin/admin)`);
  if (debug) {
    console.log('   JDWP debug port: 8000 (attach transport=dt_socket, suspend=n)');
  }

  await waitForReadiness(child.pid);
}

async function waitForReadiness(pid, timeoutMs = 180_000) {
  const deadline = Date.now() + timeoutMs;
  process.stdout.write('>> Waiting for readiness');
  while (Date.now() < deadline) {
    if (!isAlive(pid)) {
      console.log('');
      fail(`Process ${pid} exited during startup. Check the log: ${LOG_FILE}`);
    }
    try {
      const res = await fetch(READINESS_URL);
      if (res.ok) {
        console.log(`\n>> Ready: ${READINESS_URL} -> ${res.status}`);
        return;
      }
    } catch {
      // not up yet
    }
    process.stdout.write('.');
    await sleep(2000);
  }
  console.log('');
  console.log(`>> Timed out after ${timeoutMs / 1000}s waiting for readiness. It may still be booting — check ${LOG_FILE}.`);
}

// --- stop -----------------------------------------------------------------

async function stop() {
  if (!existsSync(PID_FILE)) {
    console.log('>> No PID file — Dirigible is not running (started via this script).');
    return;
  }
  const pid = Number(readFileSync(PID_FILE, 'utf8').trim());
  if (!pid || !isAlive(pid)) {
    console.log(`>> Recorded process (PID ${pid || '?'}) is not alive. Removing stale PID file.`);
    rmSync(PID_FILE, { force: true });
    return;
  }

  console.log(`>> Stopping Dirigible (PID ${pid})...`);
  if (IS_WINDOWS) {
    // No real graceful signal on Windows; terminate the tree forcefully.
    spawnSync('taskkill', ['/PID', String(pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    try {
      process.kill(pid, 'SIGTERM');
    } catch {
      // already gone
    }
    for (let i = 0; i < 15 && isAlive(pid); i++) {
      await sleep(1000);
    }
    if (isAlive(pid)) {
      console.log('>> Still alive after 15s — sending SIGKILL.');
      try {
        process.kill(pid, 'SIGKILL');
      } catch {
        // already gone
      }
    }
  }
  rmSync(PID_FILE, { force: true });
  console.log('>> Stopped.');
}

// --- dispatch -------------------------------------------------------------

const [command, ...rest] = process.argv.slice(2);
switch (command) {
  case 'build':
    build(rest[0]);
    break;
  case 'start':
    await start(rest.includes('--debug'));
    break;
  case 'stop':
    await stop();
    break;
  default:
    fail(`Unknown command '${command ?? ''}'. Use: build [quick|full] | start [--debug] | stop`);
}
