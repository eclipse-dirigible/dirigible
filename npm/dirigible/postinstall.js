const fs = require("fs");
const https = require("https");
const path = require("path");

const { version } = require("./package.json");

const dataDir = path.join(__dirname, "data");
const jarFile = path.join(dataDir, "dirigible-application-executable.jar");
const versionFile = path.join(dataDir, "version.txt");

// Construct URL using the version variable
const url = `https://repo.maven.apache.org/maven2/org/eclipse/dirigible/dirigible-application/${version}/dirigible-application-${version}-executable.jar`;

fs.mkdirSync(dataDir, { recursive: true });

// Read the last downloaded version, if any
let lastVersion = null;
if (fs.existsSync(versionFile)) {
  lastVersion = fs.readFileSync(versionFile, "utf8").trim();
}

// Skip download if the same version is already present
if (lastVersion === version && fs.existsSync(jarFile)) {
  console.log(`✅ Dirigible JAR v${version} already present. Skipping download.`);
  process.exit(0);
}

console.log(`⬇️ Downloading Dirigible JAR v${version} from URL: ${url}...`);

https.get(url, res => {
  if (res.statusCode !== 200) {
    console.error(`❌ Failed to download: HTTP ${res.statusCode}`);
    res.resume(); // discard data
    process.exit(1);
  }

  const file = fs.createWriteStream(jarFile);
  res.pipe(file);

  file.on("finish", () => {
    file.close();
    fs.writeFileSync(versionFile, version, "utf8");
    console.log(`✅ Download complete: ${jarFile}`);
  });
});
