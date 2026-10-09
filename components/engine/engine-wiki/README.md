# Wiki engine (add-on)

Renders Markdown (`*.md`) and Confluence-wiki (`*.confluence`) project files to HTML: the
`MarkdownSynchronizer` and `ConfluenceSynchronizer` register the artefacts, and `WikiEndpoint` serves the
rendered pages at `/services/wiki`.

**Not in the default bundle.** `dirigible-components-group-engines` does not include this module, so the
default application carries neither the synchronizers nor the endpoint. The module is still built and
released with every Dirigible version. It depends only on flexmark core, its tables and strikethrough
extensions, and Mylyn WikiText Confluence (about 2 MB).

## Adding it to an application

Add the artifact next to the Dirigible groups in the application's `pom.xml`:

```xml
<dependency>
    <groupId>org.eclipse.dirigible</groupId>
    <artifactId>dirigible-components-engine-wiki</artifactId>
    <version>${dirigible.version}</version>
</dependency>
```

The security configuration already covers `/services/wiki/*` and `/public/wiki/*`.
