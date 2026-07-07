# Label

Provides an accessible caption for a user interface element, most commonly paired with input controls. Labels clarify the purpose of the associated element and improve usability and accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use labels to clearly describe form fields, controls, or interactive elements. Each label should be concise, descriptive, and associated with its corresponding element to support screen readers and assistive technologies. Avoid using visual cues alone to indicate the purpose of a control.

## Directive

- `x-h-label`

## Examples

```html
<div x-h-field>
  <label x-h-label for="labelExmpl">Name</label>
  <input x-h-input id="labelExmpl" name="name" placeholder="Ivan Strashimechkarov" />
</div>
```

Full docs: https://www.codbex.com/harmonia/components/label.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
