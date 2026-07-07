# Navigation Menu

A horizontal navigation bar where items are either direct links or triggers that open a dropdown Menu. Intended for top-level site or application navigation.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-nav` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

## Example

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

More examples in the docs site: Basic, Show Menu On Hover, Underline, Outline, With submenus and icons.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
