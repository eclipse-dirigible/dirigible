/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
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
