// Downloads the CLI jar of this package's version from Maven Central and verifies its SHA-256.
// The jar is not bundled: since #7708 it carries the intent engine and the templates, and npm
// refuses a package of its size (#7771).
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import https from 'node:https';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const { version } = JSON.parse(fs.readFileSync(path.join(__dirname, 'package.json'), 'utf8'));

const dataDir = path.join(__dirname, 'data');
const jarFile = path.join(dataDir, 'dirigible-cli-executable.jar');
const versionFile = path.join(dataDir, 'version.txt');
const url = `https://repo.maven.apache.org/maven2/org/eclipse/dirigible/dirigible-cli/${version}/dirigible-cli-${version}-executable.jar`;

function fail(message) {
    console.error(`❌ ${message}`);
    process.exit(1);
}

function get(target, onResponse) {
    https.get(target, res => {
        if (res.statusCode !== 200) {
            res.resume();
            fail(`Failed to download ${target}: HTTP ${res.statusCode}`);
        }
        onResponse(res);
    }).on('error', error => fail(`Failed to download ${target}: ${error.message}`));
}

fs.mkdirSync(dataDir, { recursive: true });

const lastVersion = fs.existsSync(versionFile) ? fs.readFileSync(versionFile, 'utf8').trim() : null;
if (lastVersion === version && fs.existsSync(jarFile)) {
    console.log(`✅ Dirigible CLI JAR v${version} already present. Skipping download.`);
    process.exit(0);
}

get(`${url}.sha256`, checksumResponse => {
    let expected = '';
    checksumResponse.setEncoding('utf8');
    checksumResponse.on('data', chunk => expected += chunk);
    checksumResponse.on('end', () => {
        expected = expected.trim().split(/\s+/)[0].toLowerCase();
        console.log(`⬇️ Downloading Dirigible CLI JAR v${version} from URL: ${url}...`);
        get(url, jarResponse => {
            // Written beside the target and renamed only once verified, so an interrupted or
            // corrupted download never leaves a jar the launcher would run.
            const partFile = `${jarFile}.part`;
            const hash = createHash('sha256');
            const file = fs.createWriteStream(partFile);
            jarResponse.on('data', chunk => hash.update(chunk));
            jarResponse.on('error', error => fail(`Download interrupted: ${error.message}`));
            jarResponse.pipe(file);
            file.on('finish', () => {
                file.close(() => {
                    const actual = hash.digest('hex');
                    if (actual !== expected) {
                        fs.rmSync(partFile, { force: true });
                        fail(`Checksum mismatch for ${url}: expected ${expected}, got ${actual}`);
                    }
                    fs.renameSync(partFile, jarFile);
                    fs.writeFileSync(versionFile, version, 'utf8');
                    console.log(`✅ Download complete: ${jarFile}`);
                });
            });
        });
    });
});
