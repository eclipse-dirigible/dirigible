#!/usr/bin/env node
import { spawn } from 'node:child_process';
import which from 'which';

console.log('Executing resolve-jar-with-which.js .........');

const jarPath = which.sync('dirigible-executable.jar');
console.log('Resolved JAR path:', jarPath);


const args = ['-jar', jarPath];

const child = spawn('java', args, {
  stdio: 'inherit',
});

child.on('exit', (code) => {
  process.exit(code);
});
