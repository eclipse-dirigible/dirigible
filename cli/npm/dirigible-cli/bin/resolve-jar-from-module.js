#!/usr/bin/env node
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createRequire } from 'node:module';


console.log('Executing resolve-jar-from-module.js .........');

const require = createRequire(import.meta.url);

const jarEntry = require.resolve('dirigible-jar/bin/dirigible-application-executable.jar');
console.log('Resolved JAR from dependency:', jarEntry);


const args = ['-jar', jarEntry];

const child = spawn('java', args, {
  stdio: 'inherit',
});

child.on('exit', (code) => {
  process.exit(code);
});
