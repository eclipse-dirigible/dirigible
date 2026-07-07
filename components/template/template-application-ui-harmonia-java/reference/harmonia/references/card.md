# Card

A flexible container that organizes content into distinct sections, typically including a header, main content area, and footer. Cards provide a clear, self-contained layout for displaying related information.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-card` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-card`
- `x-h-card-header`
- `x-h-card-title`
- `x-h-card-description`
- `x-h-card-action`
- `x-h-card-content`
- `x-h-card-footer`

## Example

```html
<div x-h-card>
  <div x-h-card-header>
    <div x-h-card-title>Random Bill</div>
    <div x-h-card-description>Billed to you</div>
    <div x-h-card-action>
      <div class="hbox items-start gap-1 text-positive">
        <span class="text-2xl">$256</span>
        <span class="text-sm" style="padding-top: 0.2rem">.16</span>
      </div>
    </div>
  </div>
  <div x-h-card-content class="vbox h-full gap-4">
    <textarea class="h-full" name="note-to-bill" x-h-textarea placeholder="Add note to bill"></textarea>
    <div class="flex items-center gap-2 pr-2">
      <span x-h-switch data-size="sm">
        <input type="checkbox" id="saveNoteSw" />
      </span>
      <label x-h-label for="saveNoteSw">Save note</label>
    </div>
  </div>
  <div x-h-card-footer class="hbox justify-end gap-2">
    <button x-h-button data-variant="link">Report</button>
    <button x-h-button>Reject</button>
    <button x-h-button data-variant="primary">Pay</button>
  </div>
</div>
```

More examples in the docs site: Login Form.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
