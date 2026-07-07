# Radio

A single-choice input that allows users to select one option from a set.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use radio buttons when users must choose only one option from a group. All options must be clearly labeled and grouped logically. Avoid using radio buttons for independent yes/no choices as checkboxes are more appropriate in that case.

## Directive

- `x-h-radio`

## Examples

```html
<form class="flex flex-col gap-3">
  <div x-h-field data-orientation="horizontal">
    <span x-h-radio>
      <input type="radio" name="size" id="r_small" value="small" checked />
    </span>
    <label x-h-label for="r_small">Small</label>
  </div>
  <div x-h-field data-orientation="horizontal">
    <span x-h-radio>
      <input type="radio" name="size" id="r_medium" value="medium" />
    </span>
    <label x-h-label for="r_medium">Medium</label>
  </div>
  <div x-h-field data-orientation="horizontal">
    <span x-h-radio>
      <input type="radio" name="size" id="r_large" value="large" />
    </span>
    <label x-h-label for="r_large">Large</label>
  </div>
</form>
```

Full docs: https://www.codbex.com/harmonia/components/radio.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
