# Split

The split component provides a flexible layout for dividing content into resizable panels.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-split` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

## Example

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

More examples in the docs site: Vertical split (2 panels), Border-style gutter, Hide panels based on screen size, Dynamically add/remove panels, Dynamically create layout.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
