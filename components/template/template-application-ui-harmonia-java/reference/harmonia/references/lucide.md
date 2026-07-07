# Lucide

An optional plugin that keeps [Lucide](https://lucide.dev) icons in sync with the Alpine/Harmonia lifecycle. Place `x-h-lucide` on a Lucide placeholder and it renders that icon when Alpine initializes the element, including markup added dynamically through `x-h-include`, a router (e.g. Pinecone-router) or Alpine's `x-for` / `x-if`. No global `lucide.createIcons` scans and no event wiring are needed.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-lucide`

## API

### Arguments

| Attribute  | Type   | Required | Description                                                                                    |
| ---------- | ------ | -------- | ---------------------------------------------------------------------------------------------- |
| expression | string | false    | The icon name, e.g. `x-h-lucide="'home'"`. Used only when there is no `data-lucide` attribute. |

### Attributes

| Attribute     | Type   | Required | Description                                                                                |
| ------------- | ------ | -------- | ------------------------------------------------------------------------------------------ |
| `data-lucide` | string | false    | The icon name. Takes priority over the expression, so existing Lucide markup is a drop-in. |

The placeholder's other attributes (`class`, `role`, `aria-*`, sizing, etc.) are copied onto the rendered `<svg>`, which replaces the placeholder element.

## Example

```html
<i x-h-lucide="'arrow-up-right'"></i>
```

More examples in the docs site: CDN, Drop-in for existing markup, Inside dynamic content.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
