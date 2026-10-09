# Amazon QLDB driver (add-on)

The Amazon QLDB driver behind `@aerokit/sdk/qldb` (`QLDBRepository`) and `org.eclipse.dirigible.sdk.qldb.Qldb`.

**Not in the default bundle.** The driver brings `qldbsession`, Ion, and the AWS SDK v1 line
(`aws-java-sdk-qldb`, `aws-java-sdk-core`, `jmespath-java`, Apache HttpClient 4), so the default bundle
does not ship it (#7783). AWS ended QLDB support on 2025-07-31. The API itself,
`dirigible-components-api-qldb` (`QLDBRepository`), the TypeScript module and the Java SDK class, is in
the default bundle. Without this add-on, opening a `QLDBRepository` throws `QldbNotAvailableException`
(a `QLDBRepositoryException`), whose message names this artifact.

## Adding it to an application

Add the artifact next to the Dirigible groups in the application's `pom.xml`. It is a pom that carries
the dependencies, so it is declared with `<type>pom</type>`:

```xml
<dependency>
    <groupId>org.eclipse.dirigible</groupId>
    <artifactId>dirigible-components-api-qldb-driver</artifactId>
    <version>${dirigible.version}</version>
    <type>pom</type>
</dependency>
```

The driver reads the AWS region and credentials from the standard AWS SDK provider chain.
