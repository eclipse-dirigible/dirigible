// Downloads the CLI jar of this package's version from Maven Central and verifies its SHA-256.
// The jar is not bundled: since #7708 it carries the intent engine and the templates, and npm
// refuses a package of its size (#7771).
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import https from 'node:https';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const packageDir = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const { version } = JSON.parse(fs.readFileSync(path.join(packageDir, 'package.json'), 'utf8'));
const dataDir = path.join(packageDir, 'data');
const versionFile = path.join(dataDir, 'version.txt');
const url = `https://repo.maven.apache.org/maven2/org/eclipse/dirigible/dirigible-cli/${version}/dirigible-cli-${version}-executable.jar`;
const MAX_REDIRECTS = 5;
// some repository hosts and proxies refuse a request without one
const HEADERS = { 'User-Agent': `dirigible-cli-npm/${version} node/${process.version}` };

export const cliJarPath = path.join(dataDir, 'dirigible-cli-executable.jar');

function isPresent() {
    return fs.existsSync(cliJarPath) && fs.existsSync(versionFile)
        && fs.readFileSync(versionFile, 'utf8').trim() === version;
}

function request(target, redirects = 0) {
    return new Promise((resolve, reject) => {
        https.get(target, { headers: HEADERS }, res => {
            const { statusCode, headers } = res;
            if (statusCode >= 300 && statusCode < 400 && headers.location) {
                res.resume();
                if (redirects >= MAX_REDIRECTS) {
                    reject(new Error(`Too many redirects downloading ${target}`));
                    return;
                }
                resolve(request(new URL(headers.location, target).toString(), redirects + 1));
                return;
            }
            if (statusCode === 404) {
                res.resume();
                // npm publishes at once, Maven Central takes a while to sync a new release
                reject(new Error(`Dirigible CLI v${version} is not on Maven Central yet (${target}). A new release can take up to 30 minutes to appear - retry in a few minutes.`));
                return;
            }
            if (statusCode !== 200) {
                res.resume();
                reject(new Error(`Failed to download ${target}: HTTP ${statusCode}`));
                return;
            }
            resolve(res);
        }).on('error', error => reject(new Error(`Failed to download ${target}: ${error.message}`)));
    });
}

async function readText(target) {
    const res = await request(target);
    res.setEncoding('utf8');
    let text = '';
    for await (const chunk of res) {
        text += chunk;
    }
    return text;
}

async function download() {
    const expected = (await readText(`${url}.sha256`)).trim().split(/\s+/)[0].toLowerCase();
    console.error(`⬇️ Downloading Dirigible CLI JAR v${version} from URL: ${url}...`);
    fs.mkdirSync(dataDir, { recursive: true });
    // Written beside the target and renamed only once verified, so an interrupted or corrupted
    // download never leaves a jar the launcher would run.
    const partFile = `${cliJarPath}.part`;
    const hash = createHash('sha256');
    try {
        const res = await request(url);
        const file = fs.createWriteStream(partFile);
        await new Promise((resolve, reject) => {
            res.on('data', chunk => hash.update(chunk));
            res.on('error', error => reject(new Error(`Download of ${url} interrupted: ${error.message}`)));
            file.on('error', reject);
            file.on('finish', resolve);
            res.pipe(file);
        });
        const actual = hash.digest('hex');
        if (actual !== expected) {
            throw new Error(`Checksum mismatch for ${url}: expected ${expected}, got ${actual}`);
        }
        fs.renameSync(partFile, cliJarPath);
        fs.writeFileSync(versionFile, version, 'utf8');
        console.error(`✅ Download complete: ${cliJarPath}`);
    } finally {
        fs.rmSync(partFile, { force: true });
    }
}

/** Downloads the jar unless the one of this package's version is already in place. */
export async function ensureCliJar() {
    if (!isPresent()) {
        await download();
    }
    return cliJarPath;
}
