# Contributing to Dirigible

You can propose contributions by sending pull requests through GitHub.

And of course you can [report issues](https://github.com/eclipse/dirigible/issues/new) and [browse the current ones](https://github.com/eclipse/dirigible/issues).

## Legal considerations

Please read the [Eclipse Foundation policy on accepting contributions via Git](https://wiki.eclipse.org/Development_Resources/Contributing_via_Git).

Please read the [Code of Conduct](CODE_OF_CONDUCT.md).


Your contribution cannot be accepted unless you have an [Eclipse Contributor Agreement](https://www.eclipse.org/legal/ECA.php) in place.

Here is the checklist for contributions to be _acceptable_:

1. [Create an account at Eclipse](https://dev.eclipse.org/site_login/)
2. Add your GitHub user name in your account settings
3. [Log into the projects portal](https://projects.eclipse.org/), look for
   ["Eclipse Contributor Agreement"](https://www.eclipse.org/legal/ECA.php), and agree to the terms.
4. Ensure that you _sign-off_ your Git commits with the _same_ email address as your Eclipse Foundation profile.

## Technical considerations

1. Please make sure your code compiles by running `mvn clean verify` which will
execute both unit and integration test phases.  Additionally, consider using
[Travis CI](http://travis-ci.org) to validate your branches before you even put them into
pull requests.  All pull requests will be validated by Travis CI in any case
and must pass before being merged.
2. When committing, your author email must be the same as your GitHub account,
and make sure that you sign-off every commit (`git commit -s`).
3. Do not make pull requests from your `master` branch, please use topic branches
instead.
4. When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible.
5. Please provide meaningful commit messages.
6. Do not forget to mention the related issue, if any.
7. A contribution is not a good contribution unless it comes with unit
tests, integration tests and documentation.

## Coding Style

Please, consider Coding Style for the different languages used in the codebase as
described here: https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Coding_Style

## Code formatting
In order to contribute to the project, you need to configure your java code formatter.
Please follow the steps bellow
### Eclipse
1. Window -> Preferences -> Java -> Code Style -> Formatter -> Import -> Select [this](https://github.com/eclipse/dirigible/blob/master/dirigible-formatter.xml) formatter
![image](https://github.com/eclipse/dirigible/assets/5058839/275463e4-5795-423c-bc29-e2cfdae42630)

2. Window -> Preferences -> Java -> Editor -> Save Actions -> Check `Perform the selected actions on save` -> Check `Format source code` -> Select -> `Format all lines`
### IntelliJ
1. File (or IntelliJ IDEA if on MacOS) -> Settings -> Editor -> Code Style -> Java -> Scheme -> Import Scheme (from the settings button) -> Eclipse XML Profile -> Select [this](https://github.com/eclipse/dirigible/blob/master/dirigible-formatter.xml) formatter -> Copy to Project (from the settings button)
![image](https://github.com/eclipse/dirigible/assets/5058839/bed1ab0a-b572-47e5-9e79-31d2644c4380)

2.  File -> Settings -> Tools -> Actions on Save -> Check `Reformat code` 
### Visual Studio Code
1. Install the extension [Language Support for Java(TM) by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java)
2. File -> Preferences -> Settings -> java.format.settings.url: Set URL (or local file path) pointing to Eclipse Formatter Profile file.

### Maven
To format the code using Maven execute the following in the root dir of the project

	mvn formatter:format


## Development process

This project is separated into modules (git subprojects).
Some of the modules are standard Java Maven modules and some of them are following the WebJars module structure.
The WebJars ones can be found at the [dirigiblelabs](https://github.com/dirigiblelabs) GitHub page.
Their names start with 'ide-', 'api-' and 'ext-' depending on what they are for.

If you want to contribute in any way, you must go through the following steps:

1. Fork this repository into your account. Do NOT fork the module you want to work on.
2. Open Dirigible IDE. If you do not have access to a deployed instance, run it locally. See [README](README.md).
3. From the sidebar, go to the Git perspective and clone the module you will be working on. Use the master/main branch.
4. Go to the Workbench perspective (the first item in the sidebar) and make your changes. For front-end modules, this will automatically apply the changes in real time and make the development process faster.
5. Return to the Git section, select your module, add all unstaged files from the Staging tab in the bottom panel, then commit and push your changes to the main/master branch.
6. Open the forked Dirigible repository (in Dirigible or your favorite IDE), and make sure you have the latest changes from the master branch.
7. Checkout to a new branch by giving it either a topic name or a name starting with 'fix-' followed by the issue number.
8. Execute `mvn clean install -Pcontent` in the terminal. This will pull all changes from all modules.
9. Execute `mvn clean install` and wait for the tests to pass.
10. In case multiple modules get updated, use `git status` to see all changes and then use `git add /path/to/changed/file` to add ONLY the changes in the module which you are responsible for.
11. Commit and push your changes.
12. Create a PR to the master branch in `eclipse/dirigible` by giving it a short but descriptive name and mention the issue number/numbers in the description (See [closing issues via PR](https://github.blog/2013-05-14-closing-issues-via-pull-requests/)).
