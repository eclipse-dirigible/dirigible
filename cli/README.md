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
