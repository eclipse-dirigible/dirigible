# Breadcrumb

Displays the current page's location within a navigational hierarchy, helping users understand where they are and navigate back to parent pages.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-breadcrumb` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-breadcrumb`
- `x-h-breadcrumb-list`
- `x-h-breadcrumb-item`
- `x-h-breadcrumb-link`
- `x-h-breadcrumb-page`

## API

### Attributes

| Attribute     | Values                      | Required | Description                                                                                                                                 |
| ------------- | --------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| data-variant  | `outline`<br/>`default`     | false    | Changes the visual style of the breadcrumb.                                                                                                 |
| data-size     | `sm`<br/>`md`<br/>`default` | false    | Changes the size of the breadcrumb. Only applied when the `outline` variant is set.                                                         |
| data-overflow | `scroll`<br/>`nowrap`       | false    | `scroll` - enables horizontal scrolling, last item visible on load. `nowrap` - prevents wrapping without scrolling. Wraps items by default. |

## Example

```html
<nav x-h-breadcrumb>
  <ol x-h-breadcrumb-list>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#"><svg x-h-icon data-icon="home" role="img" aria-label="home"></svg>Home</a>
    </li>
    <li x-h-breadcrumb-item>
      <a x-h-breadcrumb-link href="#">Components</a>
    </li>
    <li x-h-breadcrumb-item>
      <span x-h-breadcrumb-page>Breadcrumb</span>
    </li>
  </ol>
</nav>
```

More examples in the docs site: Links as buttons, Outline Variant, In a Toolbar, Sizes, Scroll Overflow, Popover Overflow With `nowrap`, Dynamic items with `x-for`.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
