# Menu

A structured list of options, optionally including headers, used to create navigational menus, context menus, or dropdowns. Menus organize actions or links in a clear and accessible way.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use menus to present a set of related actions or navigation links. Menu items should be clearly labeled and grouped logically.

## Directives

`x-h-menu` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

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

## Keyboard Handling

The user can use the following keyboard shortcuts in order to navigate through the menu:

- `Up` / `Down` - Moves focus to the previous or next menu item.
- `Right` - Opens a submenu (if present) and moves focus to its first item.
- `Left` - Closes the current submenu and moves focus to its parent item.
- `Home` / `PageUp` - Moves focus to the first item in the menu.
- `End` / `PageDown` - Moves focus to the last item in the menu.
- `Enter` / `Space` - Activates the focused menu item.
- `Esc` - Closes the menu or submenu and returns focus to the controlling element.
- `Tab` - Closes the menu and submenus and sets focus to the next element.
- `Character keys (A-Z)` - Moves focus to the next item whose label starts with the typed character.

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Dropdown

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

### Contextmenu

```html
<div x-h-menu-trigger class="flex items-center justify-center p-12">Right click for context menu</div>
<ul x-h-menu aria-label="context menu" x-data="{ checkbox: { autosave: true }, radioItems: [{ label: 'Radio 1', value: 'r1' }, { label: 'Radio 2', value: 'r2' }], radioSelected: 'r1' }">
  <li x-h-menu-item>
    <i x-h-lucide role="img" data-lucide="save"></i>
    <span>Save</span>
    <span x-h-menu-item-secondary>Ctrl+S</span>
  </li>
  <li x-h-menu-item data-variant="negative">
    <i x-h-lucide role="img" data-lucide="trash"></i>
    <span>Delete</span>
    <span x-h-menu-item-secondary>Del</span>
  </li>
  <div x-h-menu-separator></div>
  <div x-h-menu-label data-inset="false">Other items</div>
  <li x-h-menu-item data-inset="true">Menu Item 1</li>
  <li x-h-menu-sub data-inset="true">
    <span>Submenu</span>
    <ul x-h-menu.sub>
      <li x-h-menu-item>Subitem 1</li>
      <li x-h-menu-item>Subitem 2</li>
      <li x-h-menu-item>Subitem 3</li>
      <li x-h-menu-sub>
        <span>Sub-submenu</span>
        <ul x-h-menu.sub>
          <li x-h-menu-item>Subitem 1</li>
          <li x-h-menu-item>Subitem 2</li>
          <li x-h-menu-item>Subitem 3</li>
        </ul>
      </li>
    </ul>
  </li>
  <div x-h-menu-separator></div>
  <div x-h-menu-label data-inset="true">Checkbox Items</div>
  <div x-h-menu-checkbox-item x-model="checkbox.autosave">Auto-Save</div>
  <div x-h-menu-separator></div>
  <div x-h-menu-label data-inset="true">Radio Items</div>
  <template x-for="radio in radioItems">
    <li x-h-menu-radio-item="radio.value" name="rg1" x-model="radioSelected" x-text="radio.label"></li>
  </template>
</ul>
```

Full docs: https://www.codbex.com/harmonia/components/menu.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
