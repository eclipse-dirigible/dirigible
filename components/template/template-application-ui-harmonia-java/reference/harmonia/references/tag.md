# Tag

A compact element used to display small pieces of information, labels, or metadata. Tags can also indicate keyboard shortcuts or other contextual cues.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-tag` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-tag`
- `x-h-tag-group`

## Example

```html
<button x-h-button data-variant="outline">
  Cancel
  <div x-h-tag>Esc</div>
</button>
```

More examples in the docs site: Examples.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
