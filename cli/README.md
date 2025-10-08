# Dirigible CLI

__Prerequisite__

```shell
DIRIGIBLE_REPO_PATH='/Users/iliyan/work/git/dirigible-fork'

cd $DIRIGIBLE_REPO_PATH
mvn clean install -P quick-build
```

## Java CLI

### Build

```shell
cd $DIRIGIBLE_REPO_PATH/cli
mvn clean install

```

### Commands

```shell
cd $DIRIGIBLE_REPO_PATH/cli

# help
java -jar target/dirigible-cli-*-executable.jar help

# greet
java -jar target/dirigible-cli-*-executable.jar greet Ivan
java -jar target/dirigible-cli-*-executable.jar greet --name John

# start dirigible project
java -jar target/dirigible-cli-*-executable.jar help start

java -jar target/dirigible-cli-*-executable.jar start  \
  --dirigibleJarPath "$DIRIGIBLE_REPO_PATH/build/application/target/dirigible-application-13.0.0-SNAPSHOT-executable.jar" \
  --projectPath "$DIRIGIBLE_REPO_PATH/cli/demo/dirigible-demo-project"

```

## NodeJS CLI wrapper

### Install wrapper globally on your machine

```shell
cd $DIRIGIBLE_REPO_PATH/cli/dirigible-node-wrapper
npm i -g .
```

### Install wrapper using link on your machine

```shell
cd $DIRIGIBLE_REPO_PATH/cli/dirigible-node-wrapper
npm ci
npm link

# npm unlink -g dirigible-cli
```

### Run from wrapper

```shell
# using the global package installation
dirigible greet NameFromGlobalConfig

# using npm scripts defined in the package.json
$DIRIGIBLE_REPO_PATH/cli/dirigible-node-wrapper
npm run dirigible greet NameFromScripts

```

## Demo project

### Prerequisite

### Run demo application

```shell
cd $DIRIGIBLE_REPO_PATH/cli/demo/dirigible-demo-project
npm clean-install

# start using start script defined in the package.json
npm run start

# start using dirigible directly
dirigible start
```

### Test application works

- GET: http://localhost:8080/services/ts/dirigible-demo-project/hello.ts should return `200` with body `Hello World!`
- Check in the `Database` perspective that table `STUDENTS` is created
- Check in the `Processes` perspective that process `DemoProcess` is deployed
