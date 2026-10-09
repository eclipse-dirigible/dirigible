# Microsoft SharePoint CMS provider (add-on)

Backs the platform's CMIS repository (Documents, `@aerokit/sdk/cms`, `org.eclipse.dirigible.sdk.cms.Cmis`)
with a SharePoint document library through the Microsoft Graph API.

**Not in the default bundle.** The Graph SDK, Azure Identity, MSAL and their transitive dependencies
weigh about 80 MB, so `dirigible-components-group-api` and `dirigible-components-group-engines` do not
include this provider. Both modules are still built and released with every Dirigible version.

## Adding it to an application

Add the two artifacts next to the Dirigible groups in the application's `pom.xml`:

```xml
<dependency>
    <groupId>org.eclipse.dirigible</groupId>
    <artifactId>dirigible-components-engine-cms-sharepoint</artifactId>
    <version>${dirigible.version}</version>
</dependency>
<dependency>
    <groupId>org.eclipse.dirigible</groupId>
    <artifactId>dirigible-components-api-sharepoint</artifactId>
    <version>${dirigible.version}</version>
</dependency>
```

`engine-cms-sharepoint` already depends on `api-sharepoint`; listing both keeps the intent explicit.

## Configuration

| Variable | Purpose |
|---|---|
| `DIRIGIBLE_CMS_PROVIDER=cms-provider-ms-sharepoint` | Selects this provider. |
| `DIRIGIBLE_MS_SHAREPOINT_SITE_HOSTNAME`, `DIRIGIBLE_MS_SHAREPOINT_SITE_PATH` | The site whose default document library holds the repository. |
| `DIRIGIBLE_MS_SHAREPOINT_TENANT_ID`, `DIRIGIBLE_MS_SHAREPOINT_CLIENT_ID`, `DIRIGIBLE_MS_SHAREPOINT_CLIENT_SECRET` | App-registration (client-credentials) authentication. |
| `DIRIGIBLE_MS_SHAREPOINT_TOKEN` | Alternative: a static bearer token. |

If `DIRIGIBLE_CMS_PROVIDER` names this provider but the add-on is not on the classpath, the
application refuses to start and the error names the artifact to add.

## TLS

`api-sharepoint` excludes `netty-tcnative-boringssl-static` (BoringSSL natives for five platforms).
The Azure Netty HTTP client used for token acquisition then runs on the JDK SSL provider; the Graph
calls themselves go through OkHttp, which always used JDK SSL.
