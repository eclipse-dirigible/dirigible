# Split

The split component provides a flexible layout for dividing content into resizable panels.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use Split when you need a side-by-side layout for content, such as editors, dashboards, or comparison panels. Avoid using Split for layouts where resizable content is unnecessary, as it will only add complexity.

> **Note:** Element hierarchy
> The panel elements MUST be direct children of the split element. Otherwise, there will be some collisions with the styles.

## Directives

`x-h-split` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-split`
- `x-h-split-panel`

## API

### Attributes

#### x-h-split

| Attribute        | Type                         | Required | Description                                                      |
| ---------------- | ---------------------------- | -------- | ---------------------------------------------------------------- |
| data-orientation | `horizontal`<br />`vertical` | true     | Orientation of the layout.                                       |
| data-variant     | `border`<br />`handle`       | false    | Style of the gutter. Default is `handle`.                        |
| data-locked      | boolean                      | false    | Locks/disables the resize handles.                               |
| data-key         | string                       | false    | Stores the layout state in localStorage under the specified key. |

#### x-h-split-panel

| Attribute       | Type    | Required | Description                                                   |
| --------------- | ------- | -------- | ------------------------------------------------------------- |
| data-collapse   | boolean | false    | Collapses the panel to the minimum size.                      |
| data-gutterless | boolean | false    | Removes the resize handle. Usually paired with `data-locked`. |
| data-hidden     | boolean | false    | Hides the panel.                                              |
| data-locked     | boolean | false    | Locks/disables the resize handle.                             |
| data-size       | number  | false    | Initial size of the panel.                                    |
| data-min        | number  | false    | Minimum size of the panel.                                    |
| data-max        | number  | false    | Naximum size of the panel.                                    |

## Examples

### Horizontal split (2 panels)

```html
<div class="size-full" x-h-split data-orientation="horizontal" data-variant="handle" data-locked="false">
  <div class="rounded-md border shadow-md" x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Left panel</div>
  </div>
  <div class="rounded-md border shadow-md" x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Right panel</div>
  </div>
</div>
```

### Vertical split (2 panels)

```html
<div class="size-full" x-h-split data-orientation="vertical" data-variant="handle" data-locked="false">
  <div class="rounded-md border shadow-md" x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Top panel</div>
  </div>
  <div class="rounded-md border shadow-md" x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Bottom panel</div>
  </div>
</div>
```

### Border-style gutter

This is useful for split-window layouts. The gutter is visually thin but provides a wider interactive area for reliable mouse and touch interaction.

```html
<div class="size-full" x-h-split data-orientation="vertical" data-variant="border" data-locked="false">
  <div x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Left panel</div>
  </div>
  <div x-h-split-panel>
    <div class="flex size-full items-center justify-center overflow-hidden">Right panel</div>
  </div>
</div>
```

### Hide panels based on screen size

You can use the Breakpoint Listener in order to hide a panel (or panels) based on screen size.
In the following example, the left and right panels will hide if the screen is less then 1024 pixels wide.

```html
<div x-data="ResponsiveSplitController" class="size-full">
  <div x-h-split class="size-full" data-orientation="horizontal" data-variant="border">
    <div x-h-split-panel :data-hidden="panelVisiblility.left">
      <div class="overflow-auto">Left panel</div>
    </div>
    <div x-h-split-panel>
      <div class="overflow-auto">Cener panel</div>
    </div>
    <div x-h-split-panel :data-hidden="panelVisiblility.right">
      <div class="overflow-auto">Right panel</div>
    </div>
  </div>
</div>

<script type="text/javascript">
  Alpine.data('ResponsiveSplitController', () => ({
    panelVisiblility: {
      left: true,
      right: true,
    },
    init() {
      const breakpointListener = Harmonia.getBreakpointListener((matches) => {
        this.panelVisiblility.left = matches;
        this.panelVisiblility.right = matches;
      }, 1024);
    },
  }));
</script>
```

### Dynamically add/remove panels

You can use the `x-for` directive to add or remove panels dynamically.

```html
<div x-data="DynamicSplitController" class="vbox size-full">
  <div x-h-toolbar>
    <button x-h-button data-variant="primary" @click="add()">Add</button>
    <div x-h-toolbar-spacer></div>
    <button x-h-button data-variant="negative" @click="remove()">Remove</button>
  </div>
  <div x-h-split data-orientation="vertical" data-variant="border">
    <template x-for="panel in panels" x-bind:key="panel.id">
      <div x-h-split-panel>
        <div class="overflow-auto" x-text="panel.name"></div>
      </div>
    </template>
  </div>
</div>

<script>
  Alpine.data('DynamicSplitController', () => ({
    panels: [
      {
        name: 'Panel 1',
        id: 1,
      },
      {
        name: 'Panel 2',
        id: 2,
      },
    ],
    add() {
      this.panels.push({
        name: `Panel ${this.panels.length + 1}`,
        id: this.panels.length + 1,
      });
    },
    remove() {
      this.panels.pop();
    },
  }));
</script>
```

### Dynamically create layout

You can use the Template directive to create layouts dynamically.

```html
<div x-data="RecursiveSplitController" class="vbox size-full">
  <div x-h-split data-orientation="horizontal" data-variant="border">
    <template x-for="panel in panels" :key="panel.id">
      <template x-h-template="$refs.panelTemplate" x-data="{ panel: panel }"></template>
    </template>
    <template x-ref="panelTemplate">
      <div x-h-split-panel>
        <template x-if="panel.children">
          <div x-h-split data-orientation="vertical" data-variant="border">
            <template x-for="childPanel in panel.children" :key="childPanel.id">
              <template x-h-template="$refs.panelTemplate" x-data="{ panel: childPanel }"></template>
            </template>
          </div>
        </template>
        <template x-if="!panel.children">
          <div class="overflow-auto" x-text="panel.name"></div>
        </template>
      </div>
    </template>
  </div>
</div>

<script>
  Alpine.data('RecursiveSplitController', () => ({
    panels: [
      {
        name: 'Left',
        id: 1,
      },
      {
        id: 2,
        children: [
          {
            name: 'Top',
            id: 'top',
          },
          {
            name: 'Bottom',
            id: 'bottom',
          },
        ],
      },
      {
        name: 'Right',
        id: 3,
      },
    ],
  }));
</script>
```

Full docs: https://www.codbex.com/harmonia/layouts/split.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
