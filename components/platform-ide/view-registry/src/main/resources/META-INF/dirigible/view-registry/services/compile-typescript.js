/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { command, os } from "sdk/platform";

const path = __context.get('path');

if (path && path.endsWith(".ts")) {
    const pathTokens = path.split("/");
    // const workspace = pathTokens[3];
    const project = pathTokens[4];

    const filePath = pathTokens.slice(5).join("/")
    let tscCommand = 'tsc';
    if (os.isWindows()) {
        tscCommand = 'cmd /c tsc'
    }
    const compileCommand = `${tscCommand} --module ESNext --target ES6 --moduleResolution Node --baseUrl ../ --lib ESNext,DOM --types ../modules/types ${filePath}`;
    const workingDirectory = `target/dirigible/repository/root/registry/public/${project}`;
    try {
        const result = command.execute(compileCommand, {
            workingDirectory: workingDirectory
        });
        if (result.exitCode != 0) {
            const output = `${result.standardOutput}\n${result.errorOutput}`;
            const outputLines = output.split("\n");
            const errorMessage = outputLines.filter(line => !line.includes("TS2307: Cannot find module 'sdk/")).join("\n");

            if (errorMessage.trim()) {
                console.error(`Compilation of "${filePath}" failed with exit code: ${result.exitCode}`);
                console.error(errorMessage);
            }
        }
    } catch (e) {
        console.info(`Error occurred during compilation of TypeScript file "${filePath}": ${e}`);
    }
}
