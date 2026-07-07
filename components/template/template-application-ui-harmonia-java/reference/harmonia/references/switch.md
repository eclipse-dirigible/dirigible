# Switch

Allows users to toggle a binary state, such as true/false or on/off. Functionally, switches serve the same purpose as checkboxes but emphasize immediate, interactive state changes.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-switch`

## API

### Attributes

| Attribute | Type               | Required | Description                  |
| --------- | ------------------ | -------- | ---------------------------- |
| data-size | `default`<br/>`sm` | false    | Sets the size of the switch. |

## Example

```html
<div class="flex items-center gap-3">
  <span x-h-switch>
    <input type="checkbox" id="sw" />
  </span>
  <label x-h-label for="sw">Just switch</label>
</div>
```

More examples in the docs site: Examples.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
