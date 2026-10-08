#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
import { createRequire } from 'node:module';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
// downloaded by postinstall.js, which an install with --ignore-scripts skips
const cliJarPath = path.join(__dirname, '../data/dirigible-cli-executable.jar');
if (!fs.existsSync(cliJarPath)) {
    console.error(`❌ The Dirigible CLI JAR is missing at ${cliJarPath}. Run "npm rebuild @dirigiblelabs/dirigible-cli" to download it.`);
    process.exit(1);
}

// resolve dirigible jar from package @dirigiblelabs/dirigible
const require = createRequire(import.meta.url);
const dirigibleJarPath = require.resolve('@dirigiblelabs/dirigible/data/dirigible-application-executable.jar');

const userArgs = process.argv.slice(2);

// Define commands that require the Dirigible jar
const dirigibleJarCommands = ['start'];

const userCommand = userArgs[0];

if(userArgs && userArgs.length === 0){
    // execute help command by default
    userArgs.push('help');
}

// Determine if the user command matches one of the whitelisted ones
const shouldAddExtraArgs = dirigibleJarCommands.includes(userCommand);

// Add extra args only if needed
const extraArgs = shouldAddExtraArgs ? ['--dirigibleJarPath', dirigibleJarPath] : [];

const args = ['-jar', cliJarPath, ...userArgs, ...extraArgs];

const child = spawn('java', args, {
  stdio: 'inherit',
});

child.on('exit', (code) => {
  process.exit(code);
});
