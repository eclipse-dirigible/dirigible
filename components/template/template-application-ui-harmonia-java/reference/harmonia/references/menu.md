# Menu

A structured list of options, optionally including headers, used to create navigational menus, context menus, or dropdowns. Menus organize actions or links in a clear and accessible way.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-menu` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-menu`
- `x-h-menu-trigger`
- `x-h-menu-item`
- `x-h-menu-sub`
- `x-h-menu-item-secondary`
- `x-h-menu-separator`
- `x-h-menu-label`
- `x-h-menu-checkbox-item`
- `x-h-menu-radio-item`

## API

### Attributes

#### x-h-menu

| Attribute        | Type                                                                                                                                                                          | Required | Description                                                                                                                                                                                                            |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-align       | `bottom-start`<br/>`bottom`<br/>`bottom-end`<br/>`right-start`<br/>`right`<br/>`right-end`<br/>`left-start`<br/>`left`<br/>`left-end`<br/>`top-start`<br/>`top`<br/>`top-end` | false    | Aligns the menu relative to the cursor or relative to the trigger if in dropdown mode.                                                                                                                                 |
| data-innerclicks | boolean                                                                                                                                                                       | false    | Prevents the menu from closing when there is a click inside it.<br/>Enabling or disabling this option on a menu or submenu does not affect its nested submenus. Each menu and submenu can be configured independently. |

#### x-h-menu-item

| Attribute     | Type       | Required | Description                                                                                                         |
| ------------- | ---------- | -------- | ------------------------------------------------------------------------------------------------------------------- |
| data-variant  | `negative` | false    | Semantic color of the item.                                                                                         |
| data-disabled | boolean    | false    | Disables the item.                                                                                                  |
| data-inset    | boolean    | false    | Adds padding to the item in order to align it with ones which have an icon.                                         |
| data-active   | boolean    | false    | Marks the item as active. Sets `aria-current="page"` and applies active styling. Use only inside a navigation menu. |

#### x-h-menu-sub

| Attribute     | Type    | Required | Description                                                                    |
| ------------- | ------- | -------- | ------------------------------------------------------------------------------ |
| data-disabled | boolean | false    | Disabled the subitem.                                                          |
| data-inset    | boolean | false    | Adds padding to the subitem in order to align it with ones which have an icon. |

#### x-h-menu-label

| Attribute  | Type    | Required | Description                                                                             |
| ---------- | ------- | -------- | --------------------------------------------------------------------------------------- |
| data-inset | boolean | false    | Adds padding to the label in order to align it with items and subitems that have icons. |

#### x-h-menu-radio-item

| Attribute | Type | Required | Description                                                               |
| --------- | ---- | -------- | ------------------------------------------------------------------------- |
| `self`    | any  | true     | Sets the value of the radio item. Expects a string literal or a variable. |

### Modifiers

#### x-h-menu-trigger

| Modifier | Description                                              |
| -------- | -------------------------------------------------------- |
| dropdown | Activates dropdown mode.                                 |
| chevron  | Rotates the last icon inside the trigger at 180 degrees. |

#### x-h-menu

| Modifier | Description                    |
| -------- | ------------------------------ |
| sub      | Menu will behave as a submenu. |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<button x-h-button x-h-menu-trigger.dropdown>Dropdown</button>
<ul x-h-menu>
  <div x-h-menu-label>Profile</div>
  <li x-h-menu-item>Set yourself as away</li>
  <li x-h-menu-sub>
    <span>Pause notifications</span>
    <ul x-h-menu.sub>
      <li x-h-menu-item>15 minutes</li>
      <li x-h-menu-item>30 minutes</li>
      <li x-h-menu-item>1 hour</li>
      <li x-h-menu-item>2 hours</li>
      <li x-h-menu-item>4 hours</li>
      <li x-h-menu-item>1 day</li>
    </ul>
  </li>
  <div x-h-menu-label>Team</div>
  <li x-h-menu-item>Invite users</li>
  <div x-h-menu-separator></div>
  <li x-h-menu-item data-variant="negative">Log out</li>
</ul>
```

More examples in the docs site: Contextmenu.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
