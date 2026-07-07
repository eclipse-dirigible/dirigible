# Focus

A behavior-only directive that programmatically sets focus on an element based on a specified condition. Focus helps guide user interaction and improves accessibility by controlling which elements receive input or attention.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-focus`

## API

### Arguments

| Attribute | Type    | Required | Description                          |
| --------- | ------- | -------- | ------------------------------------ |
| `self`    | boolean | true     | When true, the element gets focused. |

## Example

```html
<div x-data="{ sheetVisible: false }">
  <div x-h-sheet-overlay="sheetVisible">
    <div x-h-sheet class="vbox gap-4 p-4" data-align="bottom">
      <div x-h-field>
        <label x-h-label for="focusExample-2">Write comment</label>
        <textarea x-h-textarea id="focusExample-2" class="resize-none" placeholder="Comment..." x-h-focus="sheetVisible"></textarea>
      </div>
      <div class="hbox justify-end gap-2">
        <button x-h-button data-variant="primary" @click="sheetVisible = false">Send</button>
        <button x-h-button data-variant="outline" @click="sheetVisible = false">Cancel</button>
      </div>
    </div>
  </div>
  <button x-h-button @click="sheetVisible = true">Show</button>
</div>
```

More examples in the docs site: Focus an input with a delay.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
