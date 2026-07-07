# Textarea

Provides a multi-line input field for users to enter longer text. Textareas should always be paired with a label to clearly communicate the expected content and ensure accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-textarea`

## API

### Modifiers

| Modifier | Description                                     |
| -------- | ----------------------------------------------- |
| group    | Used when the textarea is inside an input group |

### Validation timing

By default this control shows native-constraint errors (for example `required`) only after the user interacts with it or attempts to submit, not on page load. To validate on load instead, set `data-validate="immediate"` on a wrapping `x-h-fieldset`, `x-h-field`, or any ancestor element. Setting `aria-invalid="true"` yourself always shows the error immediately. See Fieldset for details.

## Example

```html
<textarea x-h-textarea placeholder="Comment..."></textarea>
```

More examples in the docs site: Without resize handle.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
