# Input

Provides a single-line field for users to enter text or color values. Inputs should always be paired with a label to clearly communicate the expected content and ensure accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use input fields for capturing user data, such as names, emails, or color. If you need a numeric input, use the Input Number instead. Avoid leaving inputs unlabeled, as this can confuse users and reduce accessibility.

## Directive

- `x-h-input`

## API

### Attributes

| Attribute | Values             | Required | Description                    |
| --------- | ------------------ | -------- | ------------------------------ |
| data-size | `sm`<br/>`default` | false    | Changes the size of the input. |

### Modifiers

| Modifier | Description                                  |
| -------- | -------------------------------------------- |
| group    | Used when the input is inside an input group |
| table    | Used when the input is inside a table        |

### Validation timing

By default this control shows native-constraint errors (for example `required`) only after the user interacts with it or attempts to submit, not on page load. To validate on load instead, set `data-validate="immediate"` on a wrapping `x-h-fieldset`, `x-h-field`, or any ancestor element. Setting `aria-invalid="true"` yourself always shows the error immediately. See Fieldset for details.

## Examples

### Text Input

<!-- prettier-ignore -->
```html
<input x-h-input type="text" placeholder="Search..." />
<input x-h-input data-size="sm" type="text" placeholder="Search..." />
```

### Color Input

```html
<input x-h-input type="color" value="#26a269" />
```

### Invalid Input

```html
<input x-h-input type="text" aria-invalid="true" />
```

Full docs: https://www.codbex.com/harmonia/components/input.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
