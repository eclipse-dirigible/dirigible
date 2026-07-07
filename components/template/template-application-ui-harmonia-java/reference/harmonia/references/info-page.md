# Info Page

Provides a structured layout to display instructional content, messages, or status information, such as empty states or error notifications. Info Pages combine text, imagery, and optional actions to clearly communicate context to the user.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-info-page` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-info-page`
- `x-h-info-page-header`
- `x-h-info-page-media`
- `x-h-info-page-title`
- `x-h-info-page-description`
- `x-h-info-page-content`

## API

### Modifiers

#### x-h-info-page-media

| Modifier | Description                                                                 |
| -------- | --------------------------------------------------------------------------- |
| icon     | Applies styles for inline svg icons. Do not activate when using an img tag. |

## Example

```html
<div x-h-info-page>
  <div x-h-info-page-header>
    <div x-h-info-page-media>
      <img src="/harmonia/logo/harmonia.svg" alt="@harmonia" width="256px" />
    </div>
    <div x-h-info-page-title>Harmonia</div>
    <div x-h-info-page-description>UI component library for Alpine.js</div>
  </div>
  <div x-h-info-page-content>
    <button x-h-button data-variant="primary">GitHub Page</button>
  </div>
</div>
```

More examples in the docs site: With inline SVG icon, With border.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
