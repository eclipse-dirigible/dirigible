# Toolbar

A container that groups actions (like buttons, inputs, or popovers) relevant to the current view. Toolbars are typically positioned at the top or bottom of a section, providing convenient access to context-specific functionality.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use toolbars to organize controls that operate on the current content or view, such as formatting actions, filters, or navigation shortcuts. Ensure actions are clearly labeled, logically grouped, and do not overwhelm the interface. Toolbars can also serve as footers when contextual actions are needed at the bottom of a view.

## Directives

`x-h-toolbar` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-toolbar`
- `x-h-toolbar-image`
- `x-h-toolbar-title`
- `x-h-toolbar-subtitle`
- `x-h-toolbar-branding`
- `x-h-toolbar-spacer`
- `x-h-toolbar-separator`

## API

### Attributes

#### x-h-toolbar

| Attribute       | Type                          | Required | Description                                               |
| --------------- | ----------------------------- | -------- | --------------------------------------------------------- |
| data-variant    | `default`<br />`transparent`  | false    | Transparent background color. Does not remove the border. |
| data-size       | `default`<br />`md`<br />`sm` | false    | Make the toolbar smaller.                                 |
| data-floating   | boolean                       | false    | Floating style toolbar.                                   |
| data-borderless | boolean                       | false    | Removes toolbar borders.                                  |

### Modifiers

#### x-h-toolbar

| Modifier | Description          |
| -------- | -------------------- |
| footer   | Footer-style toolbar |

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Default

```html
<div x-h-toolbar>
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Borderless

```html
<div x-h-toolbar data-borderless="true">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Floating

```html
<div x-h-toolbar data-floating="true">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Transparent

```html
<div x-h-toolbar data-variant="transparent">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Floating Transparent

```html
<div x-h-toolbar data-variant="transparent" data-floating="true">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Small

```html
<div x-h-toolbar data-size="sm">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-size="sm" data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-size="sm" data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Medium

```html
<div x-h-toolbar data-size="md">
  <span x-h-toolbar-title>Title</span>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-size="md" data-variant="transparent"><i x-h-lucide role="img" data-lucide="save"></i>Save</button>
  <div x-h-toolbar-separator></div>
  <button x-h-button data-size="md" data-variant="transparent"><i x-h-lucide role="img" data-lucide="plus"></i>Add</button>
</div>
```

### Footer

```html
<div x-h-toolbar.footer>
  <div x-h-toolbar-spacer></div>
  <button x-h-button data-size="md" data-variant="primary">Apply</button>
  <button x-h-button data-size="md" data-variant="transparent">Cancel</button>
</div>
```

### As page header (shellbar)

```html
<header x-h-toolbar x-data="{ showClear: false, search: '' }">
  <img x-h-toolbar-image src="/harmonia/logo/harmonia.svg" alt="@harmonia" />
  <span x-h-toolbar-branding>
    <h1 x-h-toolbar-title>Harmonia</h1>
    <h2 x-h-toolbar-subtitle>by codbex</h2>
  </span>
  <div x-h-toolbar-spacer></div>
  <div x-h-input-group style="max-width:50%">
    <input x-h-input.group placeholder="Search..." x-model="search" @keyup="(event) => { showClear = event.originalTarget.value !== '' }" />
    <div x-h-input-group-addon data-align="inline-start">
      <i x-h-lucide role="img" data-lucide="search"></i>
    </div>
    <div x-h-input-group-addon data-align="inline-end">
      <button x-h-button.addon aria-label="clear" x-show="showClear" @click="showClear = false; search=''">
        <i x-h-lucide role="img" data-lucide="x"></i>
      </button>
    </div>
  </div>
  <div x-h-toolbar-spacer></div>
  <button x-h-avatar x-h-menu-trigger.dropdown class="bg-secondary text-secondary-foreground">U</button>
  <ul x-h-menu aria-label="user dropdown" data-align="bottom-end">
    <div x-h-menu-label>Profile</div>
    <li x-h-menu-item>Set yourself as away</li>
    <li x-h-menu-sub id="pnsm">
      Pause notifications
      <ul x-h-menu.sub aria-labelledby="pnsm">
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
</header>
```

Full docs: https://www.codbex.com/harmonia/components/toolbar.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
