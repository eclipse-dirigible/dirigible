# Sidebar

A vertical navigation panel used to present top-level application links or sections. Sidebars provide persistent access to primary navigation, helping users move through the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-sidebar` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-sidebar`
- `x-h-sidebar-header`
- `x-h-sidebar-header-item`
- `x-h-sidebar-content`
- `x-h-sidebar-group`
- `x-h-sidebar-group-label`
- `x-h-sidebar-group-action`
- `x-h-sidebar-group-content`
- `x-h-sidebar-menu`
- `x-h-sidebar-menu-item`
- `x-h-sidebar-menu-button`
- `x-h-sidebar-menu-action`
- `x-h-sidebar-menu-badge`
- `x-h-sidebar-menu-skeleton`
- `x-h-sidebar-separator`
- `x-h-sidebar-menu-sub`
- `x-h-sidebar-footer`

## API

### Attributes

#### x-h-sidebar

| Attribute       | Type    | Required | Description                             |
| --------------- | ------- | -------- | --------------------------------------- |
| data-collapsed  | boolean | false    | Collapses the sidebar to an icon width. |
| data-floating   | boolean | false    | Adds border and shadow to the sidebar.  |
| data-borderless | boolean | false    | Removes the side border (left/right).   |

#### x-h-sidebar-menu-button

| Attribute   | Type                        | Required | Description                       |
| ----------- | --------------------------- | -------- | --------------------------------- |
| data-active | boolean                     | false    | Sets the menu button as active.   |
| data-size   | `default`<br/>`sm`<br/>`lg` | false    | Sets the size of the menu button. |

#### x-h-sidebar-header

| Attribute       | Type    | Required | Description            |
| --------------- | ------- | -------- | ---------------------- |
| data-borderless | boolean | false    | Removes bottom border. |

#### x-h-sidebar-menu-sub

| Attribute | Type    | Required | Description                                                                                                |
| --------- | ------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| data-line | boolean | false    | Draws a line on the left side of the menu, indicating which items are part of the menu. Default is `true`. |

#### x-h-sidebar-footer

| Attribute       | Type    | Required | Description         |
| --------------- | ------- | -------- | ------------------- |
| data-borderless | boolean | false    | Removes top border. |

### Modifiers

#### x-h-sidebar

| Modifier | Description                    |
| -------- | ------------------------------ |
| right    | Adds border to the left side.  |
| left     | Adds border to the right side. |

#### x-h-sidebar-group

| Modifier  | Type    | Required | Description                                                        |
| --------- | ------- | -------- | ------------------------------------------------------------------ |
| collapsed | boolean | false    | Enables collapse/expand for the group content. Default is `false`. |

#### x-h-sidebar-menu-action

| Modifier | Description                             |
| -------- | --------------------------------------- |
| autohide | The action will be shown only on hover. |

#### x-h-sidebar-menu-skeleton

| Modifier | Description                                                                      |
| -------- | -------------------------------------------------------------------------------- |
| icon     | Adds an icon shape to the skeleton to indicate that the items will have an icon. |

## Example

```html
<div class="hbox size-full gap-2" x-data="{ collapsed: false }">
  <div x-h-sidebar :data-collapsed="collapsed">
    <div x-h-sidebar-header>
      <div x-h-sidebar-header-item>
        <i x-h-lucide role="img" class="size-6" data-lucide="box"></i>
        <span>Harmonia</span>
      </div>
    </div>
    <div x-h-sidebar-content></div>
    <div x-h-sidebar-footer data-borderless="true">
      <button x-h-sidebar-menu-button @click="collapsed = !collapsed">
        <svg x-h-icon :data-icon="collapsed ? 'chevron-right' : 'chevron-left'" role="presentation"></svg>
        <span x-text="collapsed ? 'Expand' : 'Collapse'"></span>
      </button>
    </div>
  </div>
</div>
```

More examples in the docs site: Sidebar header and footer, Borderless Sidebar, Sidebar content, Sidebar right side, Collapsed sidebar, Sidebar skeleton, Full example.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
