#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { ensureCliJar } from '../lib/cli-jar.js';

// downloaded by postinstall.js, or here on the first run when the install skipped it
let cliJarPath;
try {
    cliJarPath = await ensureCliJar();
} catch (error) {
    console.error(`❌ ${error.message}`);
    process.exit(1);
}

// resolve dirigible jar from package @dirigiblelabs/dirigible
const require = createRequire(import.meta.url);
const dirigibleJarPath = require.resolve('@dirigiblelabs/dirigible/data/dirigible-application-executable.jar');

const userArgs = process.argv.slice(2);

// Define commands that require the Dirigible jar
// (generate runs the platform's own generator from it, #7793)
const dirigibleJarCommands = ['start', 'generate'];

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
