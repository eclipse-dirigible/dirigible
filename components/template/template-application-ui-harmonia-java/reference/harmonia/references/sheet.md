# Sheet

The sheet component is a side panel that overlays the window content and is shown on one side of the screen. It can be used for displaying additional content like settings, input forms, cookie notices or other components like a Sidebar.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use sheets to present supplementary information or interactive content without navigating away from the main interface. Avoid overloading sheets with too much content, as the available space is limited.

## Directives

`x-h-sheet` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

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

## Examples

### Sheet with Sidebar

```html
<div x-data="SheetController">
  <div x-h-sheet-overlay="isOpen">
    <div x-h-sheet :data-align="side">
      <div x-h-sidebar>
        <div x-h-sidebar-content>
          <div x-h-sidebar-group>
            <div x-h-sidebar-group-label>Application</div>
            <div x-h-sidebar-group-content>
              <ul x-h-sidebar-menu>
                <li x-h-sidebar-menu-item>
                  <button x-h-sidebar-menu-button data-active="false" @click="isOpen = false">
                    <i x-h-lucide role="img" data-lucide="house"></i>
                    <span>Home</span>
                    <span x-h-sidebar-menu-badge>11</span>
                  </button>
                </li>
                <li x-h-sidebar-menu-item>
                  <button x-h-sidebar-menu-button data-active="false" @click="isOpen = false">
                    <i x-h-lucide role="img" data-lucide="file-text"></i>
                    <span>Documents</span>
                  </button>
                </li>
                <li x-h-sidebar-menu-item>
                  <button x-h-sidebar-menu-button data-active="false" @click="isOpen = false">
                    <i x-h-lucide role="img" data-lucide="blocks"></i>
                    <span>Extensions</span>
                  </button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <button x-h-button @click="openSheet()">Open Sheet</button>
</div>

<script type="text/javascript">
  Alpine.data('SheetController', () => ({
    isOpen: false,
    side: 'left',
    openSheet() {
      this.isOpen = true;
    },
  }));
</script>
```

### Alignment

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

Full docs: https://www.codbex.com/harmonia/components/sheet.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
