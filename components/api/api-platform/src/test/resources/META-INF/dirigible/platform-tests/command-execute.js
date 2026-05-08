import { Command } from '@aerokit/sdk/platform/command';
import { Assert } from 'test/assert';
import { OS } from '@aerokit/sdk/platform/os';

const cmdForExec = OS.isWindows() ? "cmd /c echo 'hello dirigible!'" : "echo 'hello dirigible!'";
const result = Command.execute(cmdForExec);
console.log("[Result]: " + result);

Assert.assertTrue(result !== undefined && result !== null);
