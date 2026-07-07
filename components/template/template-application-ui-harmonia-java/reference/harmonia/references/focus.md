# Focus

A behavior-only directive that programmatically sets focus on an element based on a specified condition. Focus helps guide user interaction and improves accessibility by controlling which elements receive input or attention.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the focus directive to move or trap focus in forms, dialogs, modals, or other interactive elements. Avoid shifting focus unnecessarily, as this can disrupt the user experience and accessibility.

## Directive

- `x-h-focus`

## API

### Arguments

| Attribute | Type    | Required | Description                          |
| --------- | ------- | -------- | ------------------------------------ |
| `self`    | boolean | true     | When true, the element gets focused. |

## Examples

### Focus an input with a delay

```html
<div x-data="app">
  <input x-h-input x-h-focus="focusInput" />
</div>
<script>
  document.addEventListener('alpine:init', () => {
    Alpine.data('app', () => ({
      focusInput: false,
      init() {
        setTimeout(() => {
          this.focusInput = true;
        }, 2000);
      },
    }));
  });
</script>
```

### Show sheet and focus textarea

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

Full docs: https://www.codbex.com/harmonia/utilities/focus.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
