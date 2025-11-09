#!/usr/bin/env node

import { execSync } from "child_process";
import { existsSync } from "fs";

const isWindows = process.platform === "win32";
const script = isWindows ? "build-source.ps1" : "build-source.sh";

if (!existsSync(script)) {
  console.error(`❌ Build script not found: ${script}`);
  process.exit(1);
}

console.log(`🛠️  Detected ${isWindows ? "Windows" : "Unix"} environment, running ${script}...`);

try {
  if (isWindows) {
    // Run via PowerShell on Windows
    execSync(`powershell -ExecutionPolicy Bypass -File "${script}"`, { stdio: "inherit" });
  } else {
    // Run via bash/sh on Unix-like systems
    execSync(`bash "${script}"`, { stdio: "inherit" });
  }
} catch (err) {
  console.error(`❌ Build failed while running ${script}`);
  process.exit(1);
}
