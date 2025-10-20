# @dirigiblelabs/dirigible-cli

[![npm version](https://badge.fury.io/js/%40dirigiblelabs%2Fdirigible-cli.svg)](https://www.npmjs.com/package/@dirigiblelabs/dirigible-cli)
[![License](https://img.shields.io/badge/License-EPL--2.0-blue.svg)](https://opensource.org/licenses/EPL-2.0)

**Eclipse Dirigible CLI**

A powerful command-line interface tool to streamline the development and management of
your [Eclipse Dirigible](https://github.com/eclipse-dirigible/dirigible) projects.

## 🚀 Overview

The `@dirigiblelabs/dirigible-cli` package is the essential external utility for developers working with **Eclipse
Dirigible**. It abstracts the complexity of common development tasks, allowing you to interact with and manage your
projects directly from your terminal using a single command: `dirigible`.

The CLI helps in developing Eclipse Dirigible projects by providing **useful commands** for typical development
workflows.

## 📦 Installation

There are two primary ways to install and use the Dirigible CLI: globally for system-wide access, or locally as a
development dependency for project-specific use.

### 1. Global Installation (Recommended for CLI Tools)

Install the CLI globally using `npm` to run the `dirigible` command directly from any directory in your terminal.

```shell
npm install -g @dirigiblelabs/dirigible-cli
```

### 2. Local Project Dependency

Install the CLI as a development dependency if you prefer to manage it per-project.

```shell
npm install --save-dev @dirigiblelabs/dirigible-cli
```

When installed locally, you can access the command using `npx` (e.g., `npx dirigible <command>`) or by registering
scripts in your `package.json`.

**Example of registering a script:**

```json
// package.json
{
  ...
  "scripts": {
    "start": "dirigible start"
  }
}
```

You can then execute the script using:

```shell
npm run start
```

## 💡 Usage

The CLI is invoked using the single command `dirigible`.

### Check All Commands

To see a comprehensive list of all supported commands, arguments, and options, execute the built-in help command:

```shell
dirigible help
```

This will output detailed information on how to use each feature of the CLI.

## 🔗 Project Resources

For comprehensive documentation, API references, and to learn more about the Eclipse Dirigible platform, please visit
the official channels:

- Official GitHub Repository: https://github.com/eclipse-dirigible/dirigible
- Official Project Website: https://www.dirigible.io/
