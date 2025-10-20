# dirigible-demo-project

A simple dirigible demo project which is used for `@dirigiblelabs/dirigible-cli` testing.

<!-- TOC -->

* [dirigible-demo-project](#dirigible-demo-project)
    * [Validation scenarios](#validation-scenarios)
        * [Prerequisites](#prerequisites)
        * [Test released version of dirigible-cli](#test-released-version-of-dirigible-cli)
        * [Test using local versions](#test-using-local-versions)
    * [Dirigible demo project validation](#dirigible-demo-project-validation)
        * [Validate npm scripts](#validate-npm-scripts)
        * [Validate dirigible-cli global installation](#validate-dirigible-cli-global-installation)
        * [Application validation](#application-validation)

<!-- TOC -->

## Validation scenarios

### Prerequisites

- Set variables

```shell
DIRIGIBLE_REPO_PATH='<path_to_git_repo>'
```

- Cleanup
  Execute the following to ensure `@dirigiblelabs/dirigible-cli` is not installed locally.

```shell
npm uninstall -g @dirigiblelabs/dirigible-cli

npm unlink -g @dirigiblelabs/dirigible-cli
```

### Test released version of dirigible-cli

An example for version `12.28.0`

- Set `dirigible-cli` version

```shell
DIRIGIBLE_VERSION='12.28.0'

cd "$DIRIGIBLE_REPO_PATH/npm/tests/dirigible-demo-project"

# set dirigible-cli version
npm pkg set devDependencies.@dirigiblelabs/dirigible-cli=$DIRIGIBLE_VERSION
```

- Install `dirigible-cli` globally

```shell
npm install -g @dirigiblelabs/dirigible-cli@$DIRIGIBLE_VERSION

dirigible version
```

- Execute the steps in [Dirigible demo project validation](#dirigible-demo-project-validation)

### Test using local versions

- Build the projects

```shell
# build the whole project
cd $DIRIGIBLE_REPO_PATH
mvn clean install -P quick-build

# or build the CLI project only
cd $DIRIGIBLE_REPO_PATH/cli
mvn clean install -P quick-build
```

- Copy the CLI jar to the npm project

```shell
cp -f "$DIRIGIBLE_REPO_PATH/cli/target/dirigible-cli-13.0.0-SNAPSHOT-executable.jar" \
  "$DIRIGIBLE_REPO_PATH/npm/dirigible-cli/bin/dirigible-cli.jar"
```

- Set `@dirigiblelabs/dirigible` version

```shell
cd $DIRIGIBLE_REPO_PATH/npm/dirigible-cli
# v12.28.0 for example 
npm pkg set dependencies.@dirigiblelabs/dirigible=12.28.0
```

- Install `dirigible-cli`

```shell
npm install
```

- Test `dirigible-cli` npm scripts

```shell
npm run dirigible help
```

- Install `dirigible-cli` globally

```shell
npm install -g .

dirigible help
```

- Set `dirigible-cli` version in the demo project

```shell
cd "$DIRIGIBLE_REPO_PATH/npm/tests/dirigible-demo-project"

# set dirigible-cli version
npm pkg set devDependencies.@dirigiblelabs/dirigible-cli=file:../dirigible-cli
```

- Execute the steps in [Dirigible demo project validation](#dirigible-demo-project-validation)

## Dirigible demo project validation

### Validate npm scripts

- Start the project

```shell
cd "$DIRIGIBLE_REPO_PATH/npm/tests/dirigible-demo-project"

npm install
npm run start
```

- Verify the project by following the steps described [here](#application-validation)

### Validate dirigible-cli global installation

- Start the project

```shell
cd "$DIRIGIBLE_REPO_PATH/npm/tests/dirigible-demo-project"

dirigible start
```

- Verify the project by following the steps described [here](#application-validation)

### Application validation

- GET: http://localhost:8080/services/ts/dirigible-demo-project/hello.ts should return `200` with body
  `Hello World!`
- Check in the `Database` perspective that table `STUDENTS` is created
- Check in the `Processes` perspective that process `DemoProcess` is deployed
