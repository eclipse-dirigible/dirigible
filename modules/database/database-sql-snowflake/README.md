# Snowflake SQL dialect

The Snowflake `ISqlDialect` and its `ISqlDialectProvider` registration. Part of the default bundle
(through `dirigible-components-group-database`).

**The Snowflake JDBC driver is not bundled.** `net.snowflake:snowflake-jdbc` is a single ~99 MB jar,
so Dirigible ships the dialect only. The dialect code does not compile against the driver; the
platform refers to it by class name (`net.snowflake.client.jdbc.SnowflakeDriver`) only. The driver
version is still managed in `dependencies/pom.xml` (`snowflake.version`), so an application that
imports the Dirigible BOM gets the tested version.

## Adding the driver

Pick one:

- **In the application's `pom.xml`** (the way an edition that targets Snowflake, such as a Snowpark
  Container Services image, ships it):

  ```xml
  <dependency>
      <groupId>net.snowflake</groupId>
      <artifactId>snowflake-jdbc</artifactId>
  </dependency>
  ```

- **At runtime** as a `scope: "platform"` dependency of a project, which appends the jar to the
  system classloader (JDBC drivers have to live there for `DriverManager`); it is active without a
  restart.
- **As a drop-in** jar in the `/modules` folder of the deployment.

## Without the driver

A data source (including `DefaultDB`/`SystemDB`) whose driver is
`net.snowflake.client.jdbc.SnowflakeDriver` fails to initialize with an error that names the driver
and the data source and says that it is not on the classpath.

`dirigible-components-data-source-snowpark` (the Snowpark connection configurator) and
`dirigible-components-security-snowflake` do not depend on the driver either; they stay in the
default bundle and only take effect with a Snowflake data source.
