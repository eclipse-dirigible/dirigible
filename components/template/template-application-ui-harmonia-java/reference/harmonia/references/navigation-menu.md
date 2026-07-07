# Navigation Menu

A horizontal navigation bar where items are either direct links or triggers that open a dropdown Menu. Intended for top-level site or application navigation.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Place `x-h-nav-trigger` items next to a `x-h-menu` to get dropdown navigation. Place `x-h-nav-link` items for plain links (`<a>`) or toplevel navigational items (`<button>`). All dropdowns are powered by the existing Menu component, so all menu features - submenus, labels, separators, checkbox and radio items - are available inside them.

> **Note:** aria-label is required
> `x-h-nav` requires an `aria-label` attribute to distinguish it from other navigation landmarks on the page.

## Directives

`x-h-nav` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-nav`
- `x-h-nav-list`
- `x-h-nav-item`
- `x-h-nav-trigger`
- `x-h-nav-link`

## API

### Attributes

#### x-h-nav

| Attribute          | Type    | Required | Description                                                                                                            |
| ------------------ | ------- | -------- | ---------------------------------------------------------------------------------------------------------------------- |
| aria-label         | string  | true     | Labels the navigation landmark. Required for ARIA compliance.                                                          |
| data-variant       | string  | false    | Visual style applied to all triggers and links. One of `default`, `clear`, `underline`, `outline`. Default: `default`. |
| data-open-on-hover | boolean | false    | When present, all triggers in the nav open their menu on hover instead of click.                                       |

#### x-h-nav-link

| Attribute   | Type    | Required | Description                                                                                |
| ----------- | ------- | -------- | ------------------------------------------------------------------------------------------ |
| data-active | boolean | false    | Marks the link as the current page. Sets `aria-current="page"` and applies active styling. |

## Keyboard Handling

Top-level items follow standard Tab navigation. Once a dropdown is open, the full Menu keyboard handling applies:

- `Up` / `Down` - Moves focus to the previous or next menu item.
- `Right` - Opens a submenu (if present) and moves focus to its first item.
- `Left` - Closes the current submenu and moves focus to its parent item.
- `Home` / `PageUp` - Moves focus to the first item in the menu.
- `End` / `PageDown` - Moves focus to the last item in the menu.
- `Enter` / `Space` - Activates the focused menu item.
- `Esc` - Closes the menu or submenu and returns focus to the trigger.
- `Tab` - Closes the menu and moves focus to the next element.
- `Character keys (A-Z)` - Moves focus to the next item whose label starts with the typed character.

## Examples

### Basic

```html
<nav x-h-nav aria-label="Main navigation">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <button x-h-nav-link data-active="true">Home</button>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Products</button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-item>Product C</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Solutions</button>
      <ul x-h-menu>
        <div x-h-menu-label>Enterprise</div>
        <li x-h-menu-item>Analytics</li>
        <li x-h-menu-item>Automation</li>
        <div x-h-menu-separator></div>
        <div x-h-menu-label>Small Business</div>
        <li x-h-menu-item>Starter Plan</li>
        <li x-h-menu-item>Growth Plan</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">Docs</a>
    </li>
  </ul>
</nav>
```

### Show Menu On Hover

Hover over the menu item to trigger the popover.

```html
<nav x-h-nav aria-label="Main navigation" data-open-on-hover="true">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <a x-h-nav-link href="#" data-active="true">Home</a>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Products</button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-item>Product C</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Solutions</button>
      <ul x-h-menu>
        <div x-h-menu-label>Enterprise</div>
        <li x-h-menu-item>Analytics</li>
        <li x-h-menu-item>Automation</li>
        <div x-h-menu-separator></div>
        <div x-h-menu-label>Small Business</div>
        <li x-h-menu-item>Starter Plan</li>
        <li x-h-menu-item>Growth Plan</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">Docs</a>
    </li>
  </ul>
</nav>
```

### Clear

No hover background. The active item is highlighted in the primary color.

```html
<nav x-h-nav aria-label="Clear navigation" data-variant="clear">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <button x-h-nav-link data-active="true">Home</button>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Products</button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-item>Product C</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">Docs</a>
    </li>
  </ul>
</nav>
```

### Underline

Same as `clear` but items show a text underline on hover.

```html
<nav x-h-nav aria-label="Underline navigation" data-variant="underline">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <button x-h-nav-link data-active="true">Home</button>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Products</button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-item>Product C</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">Docs</a>
    </li>
  </ul>
</nav>
```

### Outline

Hover and active states show a border outline instead of a filled background.

```html
<nav x-h-nav aria-label="Outline navigation" data-variant="outline">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <button x-h-nav-link data-active="true">Home</button>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>Products</button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-item>Product C</li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">Docs</a>
    </li>
  </ul>
</nav>
```

### With submenus and icons

```html
<nav x-h-nav aria-label="Main navigation">
  <ul x-h-nav-list>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">
        <svg x-h-icon data-icon="home" role="img" aria-label="home"></svg>
        Home
      </a>
    </li>
    <li x-h-nav-item>
      <button x-h-nav-trigger>
        <i x-h-lucide role="img" data-lucide="library-big"></i>
        <span>Products</span>
      </button>
      <ul x-h-menu>
        <li x-h-menu-item>Product A</li>
        <li x-h-menu-item>Product B</li>
        <li x-h-menu-sub>
          <span>More</span>
          <ul x-h-menu.sub>
            <li x-h-menu-item>Product C</li>
            <li x-h-menu-item>Product D</li>
            <li x-h-menu-item>Product E</li>
          </ul>
        </li>
      </ul>
    </li>
    <li x-h-nav-item>
      <a x-h-nav-link href="#">
        <i x-h-lucide role="img" data-lucide="gem"></i>
        Pricing
      </a>
    </li>
  </ul>
</nav>
```

Full docs: https://www.codbex.com/harmonia/components/navigation-menu.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
