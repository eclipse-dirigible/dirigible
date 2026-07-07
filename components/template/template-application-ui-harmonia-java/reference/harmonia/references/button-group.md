# Button Group

Groups related buttons into a single container to present them as a unified set of actions. This helps establish visual relationships and improves clarity when multiple actions are closely related.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-button-group` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-button-group`
- `x-h-button-group-separator`

## API

### Attributes

| Attribute        | Type                         | Required | Description                                  |
| ---------------- | ---------------------------- | -------- | -------------------------------------------- |
| data-orientation | `horizontal`<br />`vertical` | false    | Changes the orientation of the button group. |

## Example

```html
<div x-h-button-group>
  <button x-h-button data-variant="outline">Action</button>
  <button x-h-button data-size="icon" data-variant="outline" aria-label="Add button">
    <i x-h-lucide role="img" data-lucide="plus"></i>
  </button>
</div>
<div x-h-button-group>
  <button x-h-button>Left</button>
  <div x-h-button-group-separator></div>
  <button x-h-button>Right</button>
</div>
```

More examples in the docs site: Vertical.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
