# Input Number

Allows users to enter numeric values with built-in validation and step controls. This component should be paired with a label to clearly communicate the expected value and improve accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the number input when users need to enter a bounded numeric value, such as a quantity, age, or step-based setting. Set `min`, `max`, and `step` on the native `<input type="number">` to constrain the value and drive the increment/decrement controls. Always pair it with a label so the expected value is clear.

## Directive

- `x-h-input-number`

## API

### Attributes

| Attribute | Values             | Required | Description                    |
| --------- | ------------------ | -------- | ------------------------------ |
| data-size | `sm`<br/>`default` | false    | Changes the size of the input. |

### Modifiers

| Modifier | Description                           |
| -------- | ------------------------------------- |
| table    | Used when the input is inside a table |

## Examples

### Number Input

```html
<div x-h-input-number>
  <input type="number" min="0" max="10" step="2" value="4" />
</div>
```

### Invalid Number Input

```html
<div x-h-input-number>
  <input type="number" min="0" max="10" step="2" value="12" aria-invalid="true" />
</div>
```

Full docs: https://www.codbex.com/harmonia/components/input-number.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
