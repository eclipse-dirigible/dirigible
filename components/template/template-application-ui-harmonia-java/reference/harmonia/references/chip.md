# Chip

A compact, interactive element used to represent an applied filter, a selected item, or a categorization. Chips support semantic color variants and an optional close button for dismissal.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-chip` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-chip`
- `x-h-chip-close`

## API

### Attributes

#### x-h-chip

> Must be applied to a `<button>` element.

| Attribute    | Type                                                                                       | Required | Description    |
| ------------ | ------------------------------------------------------------------------------------------ | -------- | -------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline` | false    | Semantic color |

#### x-h-chip-close

> Must be applied to a `<span>` element.

| Attribute       | Type   | Required | Description                                              |
| --------------- | ------ | -------- | -------------------------------------------------------- |
| aria-label      | string | true\*   | Accessible label for the close action                    |
| aria-labelledby | string | true\*   | References an element whose text labels the close action |

::: info \* One of `aria-label` or `aria-labelledby` is required.
:::

## Example

```html
<button x-h-chip>Chip</button>
```

More examples in the docs site: Truncate Text, Icon & Text, Icon, Text & Close Button, Outline Variant, Primary Variant, Information Variant, Warning Variant, Positive Variant, Negative Variant, With Popover.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
