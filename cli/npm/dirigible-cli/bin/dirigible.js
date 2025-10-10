#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createRequire } from 'node:module';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const cliJarPath = path.join(__dirname, './dirigible-cli.jar');

// resolve dirigible jar from package dirigible-jar
const require = createRequire(import.meta.url);
const dirigibleJarPath = require.resolve('dirigible-jar/bin/dirigible-application.jar');

// Pass any CLI arguments after "npm run dirigible -- ..."
const userArgs = process.argv.slice(2);

// Define commands that require the Dirigible jar
const dirigibleJarCommands = ['start'];

// Determine if the user command matches one of the whitelisted ones
const userCommand = userArgs[0];
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
