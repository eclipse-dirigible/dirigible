# File Upload

Lets users choose one or more files for upload. It looks like a regular input with a "Browse" button on the side; clicking the field or the button opens the native file picker, and the chosen files are shown as tags inside the field.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-file-upload`

## API

### Attributes

#### x-h-file-upload

| Attribute        | Values | Required | Description                                                        |
| ---------------- | ------ | -------- | ------------------------------------------------------------------ |
| data-placeholder | string | false    | Text shown when no file is selected (default: `"No file chosen"`). |

The `<input type="file">` keeps its native attributes - set `multiple`, `accept`, `required`, `name` and `disabled` directly on it. The selected files are shown as display-only tags; picking again replaces the selection, mirroring the native control (there is no per-file remove button).

### Model

There is no `x-model`. The native `<input type="file">` is the source of truth: listen to its `change` event and read its `.files`, exactly as you would with a plain file input.

## Example

```html
<div x-h-input-group x-h-file-upload>
  <input type="file" />
  <div x-h-input-group-addon data-align="inline-start">
    <div x-h-tag-group></div>
  </div>
  <div x-h-input-group-addon data-align="inline-end">
    <button type="button" x-h-button.addon>Browse</button>
  </div>
</div>
```

More examples in the docs site: Multiple files with a custom placeholder, Reacting to selection.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
