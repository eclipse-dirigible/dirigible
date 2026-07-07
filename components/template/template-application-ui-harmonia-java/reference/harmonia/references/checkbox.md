# Checkbox

Allows users to select or deselect an option, representing a binary choice (true/false). Checkboxes indicate the current state of a setting or preference.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-checkbox`

## Example

```html
<div class="flex items-center gap-3">
  <span x-h-checkbox>
    <input type="checkbox" id="unchecked" />
  </span>
  <label x-h-label for="unchecked">Unchecked</label>
</div>
```

More examples in the docs site: Examples.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
