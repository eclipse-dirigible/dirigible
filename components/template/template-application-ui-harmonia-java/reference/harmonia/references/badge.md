# Badge

Displays a short label used to convey the semantic status of an object. Badges use color and, optionally, an icon to provide quick visual cues and reinforce meaning at a glance.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-badge` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-badge`
- `x-h-badge-indicator`

## API

### Attributes

| Attribute    | Type                                                                                       | Required | Description    |
| ------------ | ------------------------------------------------------------------------------------------ | -------- | -------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline` | false    | Semantic color |

## Example

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with badge">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator>121</span>
</button>
```

More examples in the docs site: Badge, Badge Indicator.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
