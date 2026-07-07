# Tag

A compact element used to display small pieces of information, labels, or metadata. Tags can also indicate keyboard shortcuts or other contextual cues.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use tags to highlight keywords, categories, statuses, or shortcuts in a concise and visually distinct way. Make sure tags are clear and readable, and avoid overcrowding interfaces with too many tags, as this can reduce clarity and focus.

## Directives

`x-h-tag` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-tag`
- `x-h-tag-group`

## Examples

```html
<div x-h-tag-group>
  <div x-h-tag>⌘</div>
  <div x-h-tag>⇧</div>
  <div x-h-tag>⌥</div>
  <div x-h-tag>⌃</div>
</div>
```

```html
<div x-h-tag-group>
  <div x-h-tag>Ctrl</div>
  <span>+</span>
  <div x-h-tag>B</div>
</div>
```

```html
<div x-h-tag-group>
  <div x-h-tag>Historical</div>
  <div x-h-tag>Adventure</div>
  <div x-h-tag>Psychological</div>
</div>
```

```html
<button x-h-button data-variant="outline">
  Cancel
  <div x-h-tag>Esc</div>
</button>
```

Full docs: https://www.codbex.com/harmonia/components/tag.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
