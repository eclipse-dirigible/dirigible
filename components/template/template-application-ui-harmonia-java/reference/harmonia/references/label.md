# Label

Provides an accessible caption for a user interface element, most commonly paired with input controls. Labels clarify the purpose of the associated element and improve usability and accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-label`

## Example

```html
<div x-h-field>
  <label x-h-label for="labelExmpl">Name</label>
  <input x-h-input id="labelExmpl" name="name" placeholder="Ivan Strashimechkarov" />
</div>
```

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
