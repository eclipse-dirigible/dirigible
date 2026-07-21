/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
// Post-bundling completeness check: every SDK module folder under src/ (one with an index.ts)
// must have produced dist/esm/<module>/index.mjs and dist/cjs/<module>/index.js. A truncated
// dist otherwise ships silently inside the jar and kills the ENTIRE JS layer at runtime with a
// cryptic module-resolution error, far from the cause - see eclipse-dirigible/dirigible#6339.
// Runs from the modules/ directory, right after the esbuild/tsc steps in build-source.sh/.ps1.
const { existsSync, readdirSync, statSync } = require("fs");
const { join } = require("path");

const modules = readdirSync("src").filter((name) => {
    try {
        return statSync(join("src", name)).isDirectory() && existsSync(join("src", name, "index.ts"));
    } catch (ignored) {
        return false;
    }
});

if (modules.length === 0) {
    console.error("verify-dist: no SDK module folders found under src/ - wrong working directory?");
    process.exit(1);
}

const missing = [];
for (const name of modules) {
    if (!existsSync(join("dist", "esm", name, "index.mjs"))) {
        missing.push(`dist/esm/${name}/index.mjs`);
    }
    if (!existsSync(join("dist", "cjs", name, "index.js"))) {
        missing.push(`dist/cjs/${name}/index.js`);
    }
}

if (missing.length > 0) {
    console.error(`verify-dist: the bundled SDK dist is INCOMPLETE - ${missing.length} missing output(s):`);
    for (const entry of missing) {
        console.error(`  ${entry}`);
    }
    console.error("A jar packaged from this state fails every '@aerokit/sdk/*' import at runtime");
    console.error("(eclipse-dirigible/dirigible#6339). A concurrent build mutating this checkout is the");
    console.error("usual cause - rebuild this module on its own and avoid parallel builds of one tree.");
    process.exit(1);
}

console.log(`verify-dist: OK - ${modules.length} SDK modules present in dist/esm and dist/cjs.`);
