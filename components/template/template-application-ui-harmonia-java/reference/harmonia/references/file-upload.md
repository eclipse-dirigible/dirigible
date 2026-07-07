# File Upload

Lets users choose one or more files for upload. It looks like a regular input with a "Browse" button on the side; clicking the field or the button opens the native file picker, and the chosen files are shown as tags inside the field.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the File Upload when a form needs the user to attach files, such as a document, an avatar image, or supporting evidence. Constrain the selection to what you actually accept: set `accept` to limit file types and add `multiple` only when several files are genuinely allowed. Pair it with a clear label describing what to upload and any size or format requirements, and prefer it over a bare native file input when you want the selection to stay visible and on-brand. For a single short text value, use a plain Input instead.

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

## Examples

### Single file

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

### Multiple files with a custom placeholder

```html
<div x-h-input-group x-h-file-upload data-placeholder="Select images...">
  <input type="file" multiple accept="image/*" />
  <div x-h-input-group-addon data-align="inline-start">
    <div x-h-tag-group></div>
  </div>
  <div x-h-input-group-addon data-align="inline-end">
    <button type="button" x-h-button.addon>Browse</button>
  </div>
</div>
```

### Reacting to selection

Listen to the native `change` event on the file input to react to the chosen files.

```html
<div x-h-input-group x-h-file-upload x-data>
  <input type="file" multiple @change="console.log([...$event.target.files].map((f) => f.name))" />
  <div x-h-input-group-addon data-align="inline-start">
    <div x-h-tag-group></div>
  </div>
  <div x-h-input-group-addon data-align="inline-end">
    <button type="button" x-h-button.addon>Browse</button>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/file-upload.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
