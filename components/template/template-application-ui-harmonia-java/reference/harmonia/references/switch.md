# Switch

Allows users to toggle a binary state, such as true/false or on/off. Functionally, switches serve the same purpose as checkboxes but emphasize immediate, interactive state changes.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use switches for settings or options that can be turned on or off instantly, especially when the change takes effect immediately. Make sure the associated label clearly indicates the action. Avoid using switches for independent yes/no choices that do not have immediate effect. Checkboxes are more appropriate in that case.

## Directive

- `x-h-switch`

## API

### Attributes

| Attribute | Type               | Required | Description                  |
| --------- | ------------------ | -------- | ---------------------------- |
| data-size | `default`<br/>`sm` | false    | Sets the size of the switch. |

## Examples

```html
<div class="flex items-center gap-3">
  <span x-h-switch>
    <input type="checkbox" id="sw" />
  </span>
  <label x-h-label for="sw">Just switch</label>
</div>
```

```html
<div class="flex items-center gap-3">
  <span x-h-switch data-size="sm">
    <input type="checkbox" id="sws" />
  </span>
  <label x-h-label for="sws">Just switch</label>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/switch.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
