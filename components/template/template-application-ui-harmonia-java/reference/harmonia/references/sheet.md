# Sheet

The sheet component is a side panel that overlays the window content and is shown on one side of the screen. It can be used for displaying additional content like settings, input forms, cookie notices or other components like a Sidebar.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-sheet` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-sheet`
- `x-h-sheet-overlay`

## API

### Attributes

#### x-h-sheet-overlay

| Attribute | Type    | Required | Description                                                                                                               |
| --------- | ------- | -------- | ------------------------------------------------------------------------------------------------------------------------- |
| `self`    | boolean | true     | Show/hide the sheet. This is a two-way bind. When the user clicks on the overlay, the sheet will be automatically hidden. |

#### x-h-sheet

| Attribute  | Type                                      | Required | Description                                                  |
| ---------- | ----------------------------------------- | -------- | ------------------------------------------------------------ |
| data-align | `top`<br/>`right`<br/>`bottom`<br/>`left` | false    | Aligns the sheet to one side of the screen. Default is left. |

## Example

```html
<div x-data="{ isOpen: false, side: 'left' }">
  <div x-h-sheet-overlay="isOpen">
    <div x-h-sheet :data-align="side">
      <div class="flex flex-col gap-2 p-2">
        <button x-h-button @click="side = 'top'">Top</button>
        <button x-h-button @click="side = 'right'">Right</button>
        <button x-h-button @click="side = 'bottom'">Bottom</button>
        <button x-h-button @click="side = 'left'">Left</button>
        <button x-h-button @click="isOpen = false">Close</button>
      </div>
    </div>
  </div>

  <button x-h-button @click="isOpen = true">Open</button>
</div>
```

More examples in the docs site: Sheet with Sidebar.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
