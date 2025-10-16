# @dirigiblelabs/dirigible

[![npm version](https://badge.fury.io/js/%40dirigiblelabs%2Fdirigible.svg)](https://www.npmjs.com/package/@dirigiblelabs/dirigible)
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0)

This package provides a direct distribution channel for the
official [Eclipse Dirigible](https://github.com/eclipse-dirigible/dirigible) standalone executable JAR file
via the npm registry under the `@dirigiblelabs` organization scope.

It is designed for use in environments where npm is the preferred package manager for artifact retrieval.

---

## 🚀 Usage

Since Dirigible is a Java application, you must have the **Java Development Kit (JDK) 21 or higher** installed on your
system to run the executable JAR. The default path of the jar is
`@dirigiblelabs/dirigible/bin/dirigible-application-executable.jar`

### 1. Installation

Install the package as a development dependency in your project:

```shell
npm install @dirigiblelabs/dirigible --save-dev
```

### 2. Execution

To run the Dirigible server, you need to execute the JAR file using the `java -jar` command.

The executable JAR file is installed in your `node_modules` folder. You can use a one-line command to locate and run it:

```shell
java -jar $(npm prefix)/node_modules/@dirigiblelabs/dirigible/bin/dirigible-application-executable.jar
```

The server will start, typically accessible at [http://localhost:8080](http://localhost:8080). Default Login `admin` /
`admin`

## 🔗 Project Resources

For comprehensive documentation, API references, and to learn more about the Eclipse Dirigible platform, please visit
the official channels:

- Official GitHub Repository: https://github.com/eclipse-dirigible/dirigible
- Official Project Website: https://www.dirigible.io/
