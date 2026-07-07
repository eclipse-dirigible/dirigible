# Pagination

Divides content into discrete pages, allowing users to navigate large datasets or collections more easily. Pagination improves readability and helps users focus on manageable portions of content.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use pagination for tables, card grids, lists, or other content-heavy interfaces where displaying all items at once would overwhelm the user. Avoid excessive page counts without additional navigation aids, such as "jump to page" or filtering options.

## Directives

`x-h-pagination` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-pagination`
- `x-h-pagination-content`
- `x-h-pagination-item`
- `x-h-pagination-link`
- `x-h-pagination-link-label`
- `x-h-pagination-ellipsis`

## API

### Attributes

#### x-h-pagination-link

| Attribute | Type    | Required | Description                                            |
| --------- | ------- | -------- | ------------------------------------------------------ |
| `self`    | boolean | false    | Set to true when the link is the currently active one. |

### Modifiers

#### x-h-pagination-link

| Modifier | Description                                                                  |
| -------- | ---------------------------------------------------------------------------- |
| previous | Used when the link will lead to the previous page instead of a specific one. |
| next     | Used when the link will lead to the next page instead of a specific one.     |

## Examples

```html
<nav x-h-pagination>
  <ul x-h-pagination-content>
    <li x-h-pagination-item>
      <a x-h-pagination-link.previous href="#">
        <i x-h-lucide role="img" data-lucide="chevron-left"></i>
        <span x-h-pagination-link-label>Previous</span>
      </a>
    </li>
    <li x-h-pagination-item>
      <a x-h-pagination-link="false" href="#">1</a>
    </li>
    <li x-h-pagination-item>
      <a x-h-pagination-link="true" href="#">2</a>
    </li>
    <li x-h-pagination-item>
      <a x-h-pagination-link="false" href="#">3</a>
    </li>
    <li x-h-pagination-item>
      <span x-h-pagination-ellipsis></span>
    </li>
    <li x-h-pagination-item>
      <a x-h-pagination-link.next href="#">
        <span x-h-pagination-link-label>Next</span>
        <i x-h-lucide role="img" data-lucide="chevron-right"></i>
      </a>
    </li>
  </ul>
</nav>
```

Full docs: https://www.codbex.com/harmonia/components/pagination.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
