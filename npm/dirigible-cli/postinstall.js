// Fetches the CLI jar at install time. A failure does not fail the install: the launcher
// downloads the jar on its first run, which also covers installs that skip install scripts
// (--ignore-scripts, pnpm) and a release Maven Central has not synced yet.
import { ensureCliJar } from './lib/cli-jar.js';

try {
    await ensureCliJar();
} catch (error) {
    console.warn(`⚠️ ${error.message}`);
    console.warn('⚠️ The Dirigible CLI JAR will be downloaded on the first run of "dirigible".');
}
