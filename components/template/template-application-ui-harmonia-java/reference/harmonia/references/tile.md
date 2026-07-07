# Tile

A container that presents content, previews, or shortcuts in a compact, visually distinct format. Tiles can group related information, link to pages or modules, act as selectable options that wrap a checkbox or radio, or provide an interactive preview.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-tile-group` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-tile-group`
- `x-h-tile`
- `x-h-tile-header`
- `x-h-tile-media`
- `x-h-tile-content`
- `x-h-tile-title`
- `x-h-tile-description`
- `x-h-tile-actions`
- `x-h-tile-footer`

## API

### Attributes

::: info
`x-h-tile-group` sets `role="list"` by default, but leaves any `role` you set in place. For a group of selectable tiles, set `role="group"` (checkboxes) or `role="radiogroup"` (radios) plus `aria-label`/`aria-labelledby` on the group.
:::

#### x-h-tile

| Attribute    | Type                               | Required | Description                                                         |
| ------------ | ---------------------------------- | -------- | ------------------------------------------------------------------- |
| data-variant | `outline`<br/>`shadow`<br/>`muted` | false    | Changes the style of the tile. Ignored when the tile is selectable. |

::: info
When `x-h-tile` is placed on a `<label>` element it becomes a selectable tile (see Selectable): it wraps a single checkbox or radio, is styled with the outline look automatically (`data-variant` is ignored), and reacts to the nested input being checked, focused, or disabled.
:::

#### x-h-tile-media

| Attribute    | Type                             | Required | Description                          |
| ------------ | -------------------------------- | -------- | ------------------------------------ |
| data-variant | `default`<br/>`icon`<br/>`image` | false    | Changes the style of the tile media. |

## Example

```html
<div x-h-tile data-variant="outline" class="tile-double-md">Double length medium size custom tile</div>
```

More examples in the docs site: Variants, As link, With image or icon, In group, Selectable, Checkbox group, Radio group.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
