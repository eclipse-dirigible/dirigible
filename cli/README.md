# Dirigible CLI JAR

## Commands

- Build projects

```shell
DIRIGIBLE_REPO_PATH='<path_to_dirigible_git_folder>'

cd $DIRIGIBLE_REPO_PATH

mvn clean install -P quick-build
```

### Samples

```shell
cd $DIRIGIBLE_REPO_PATH/cli

# help
java -jar target/dirigible-cli-*-executable.jar help

# help start
java -jar target/dirigible-cli-*-executable.jar help start

# start dirigible project
java -jar target/dirigible-cli-*-executable.jar start  \
  --dirigibleJarPath "$DIRIGIBLE_REPO_PATH/build/application/target/dirigible-application-13.0.0-SNAPSHOT-executable.jar" \
  --projectPath "<path_to_dirigible_project>"
```

### Regenerate a project from its intent

`generate` runs the IDE's Generate for every `*.intent` at the root of a project - the model files,
the `.test` manifest and `gen/` - with no platform booted, no database and no web server. The
projects its intent names in `uses:` are read from next to it, as in a workspace.

The generator is the platform's own, so `generate` runs it from the platform jar, like `start`: the
CLI is a launcher and carries none of the platform (#7793). When the CLI is installed via npm,
`--dirigibleJarPath` is resolved automatically.

```shell
DIRIGIBLE_JAR="$DIRIGIBLE_REPO_PATH/build/application/target/dirigible-application-*-executable.jar"

# regenerate in place (what changes or is added is written, what is no longer generated is deleted)
java -jar target/dirigible-cli-*-executable.jar generate --project "<path_to_project>" --dirigibleJarPath $DIRIGIBLE_JAR

# regenerate into another folder, leaving the project untouched
java -jar target/dirigible-cli-*-executable.jar generate --project "<path_to_project>" --out "<folder>" --dirigibleJarPath $DIRIGIBLE_JAR

# check: write nothing, print the unified diff of every COMMITTED file the regeneration changes or
# drops, and exit 1 if there is one
java -jar target/dirigible-cli-*-executable.jar generate --project "<path_to_project>" --check --dirigibleJarPath $DIRIGIBLE_JAR
```

Without the CLI, the platform jar runs the same generator directly:

```shell
java -Dloader.main=org.eclipse.dirigible.generate.HeadlessGenerate -jar $DIRIGIBLE_JAR --project "<path_to_project>" --check
```

The first line of the output names the platform version that generated. `--check` compares the files
the project carries: a generated file the project does not commit is listed as a count, not as drift,
so a project keeps under version control only the generated files it wants checked. Exit codes: `0`
no drift (or regenerated), `1` drift, `2` the intent could not be generated.

### Regeneration check in GitHub Actions

`.github/actions/regen-check` wraps `generate --check` for a pull-request gate, against the
Eclipse Dirigible release a module repository pins (its `dirigible-application` jar is downloaded
from Maven Central):

```yaml
- uses: eclipse-dirigible/dirigible/.github/actions/regen-check@master
  with:
    platform-version: 15.0.0
    projects: my-module another-module
```

`dirigible-jar` takes a locally built platform jar instead of a release; this repository checks its own
`tests/tests-integrations/src/main/resources/sample-intent-*` that way on every pull request.
