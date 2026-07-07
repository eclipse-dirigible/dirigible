# Textarea

Provides a multi-line input field for users to enter longer text. Textareas should always be paired with a label to clearly communicate the expected content and ensure accessibility.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use textareas for capturing extended input, such as comments, descriptions, or messages. The label should be descriptive. Provide optional hints or validation to guide proper entry. Avoid leaving textareas unlabeled, as this can confuse users and reduce accessibility.

## Directive

- `x-h-textarea`

## API

### Modifiers

| Modifier | Description                                     |
| -------- | ----------------------------------------------- |
| group    | Used when the textarea is inside an input group |

### Validation timing

By default this control shows native-constraint errors (for example `required`) only after the user interacts with it or attempts to submit, not on page load. To validate on load instead, set `data-validate="immediate"` on a wrapping `x-h-fieldset`, `x-h-field`, or any ancestor element. Setting `aria-invalid="true"` yourself always shows the error immediately. See Fieldset for details.

## Examples

```html
<textarea x-h-textarea placeholder="Comment..."></textarea>
```

### Without resize handle

```html
<textarea x-h-textarea class="resize-none"></textarea>
```

Full docs: https://www.codbex.com/harmonia/components/textarea.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
