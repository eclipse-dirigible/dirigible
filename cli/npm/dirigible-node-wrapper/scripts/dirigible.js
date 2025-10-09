#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// should be path to the dirigible-cli jar which is installed as separate npm dependency
// or it could be part of the current package as well TBD
// currently points to the built jar
const cliJarPath = path.join(__dirname, '../../../java/target/dirigible-cli-13.0.0-SNAPSHOT-executable.jar');

// should be path to the dirigible jar which is installed as separate npm dependency
// currently points to the built dirigible jar for dev purposes
const dirigibleJarPath = path.join(__dirname, '../../../../build/application/target/dirigible-application-13.0.0-SNAPSHOT-executable.jar');

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
