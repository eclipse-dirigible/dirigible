# etcd client (add-on)

The jetcd client library behind `@aerokit/sdk/etcd` (`etcd/client`) and `org.eclipse.dirigible.sdk.etcd.Client`.

**Not in the default bundle.** jetcd pulls in Vert.x, gRPC, protobuf and Google's common protos
(about 10 MB in 30-odd jars) for one facade, so the default bundle does not ship it (#7783). The API
itself, `dirigible-components-api-etcd` (`EtcdFacade`), the TypeScript module and the Java SDK class,
is in the default bundle. Without this add-on every call throws `EtcdNotAvailableException`, whose
message names this artifact.

## Adding it to an application

Add the artifact next to the Dirigible groups in the application's `pom.xml`. It is a pom that carries
the dependencies, so it is declared with `<type>pom</type>`:

```xml
<dependency>
    <groupId>org.eclipse.dirigible</groupId>
    <artifactId>dirigible-components-api-etcd-client</artifactId>
    <version>${dirigible.version}</version>
    <type>pom</type>
</dependency>
```

## Configuration

| Variable | Purpose |
|---|---|
| `DIRIGIBLE_ETCD_CLIENT_ENDPOINT` | The etcd endpoint, default `http://localhost:2379`. |
