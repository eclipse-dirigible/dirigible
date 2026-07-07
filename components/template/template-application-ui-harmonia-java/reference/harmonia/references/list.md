# List

A container that displays a collection of related items in a structured format. Lists help organize content clearly and improve readability by grouping similar elements together.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use lists to present multiple related items, such as options, tasks, or entries. Avoid using lists for grouping unrelated content.

## Directives

`x-h-list` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-list`
- `x-h-list-item`
- `x-h-list-header`

## API

### Modifiers

#### x-h-list-item

| Modifier    | Description                                     |
| ----------- | ----------------------------------------------- |
| interactive | Makes the list item interactive and selectable. |

## Examples

```html
<ul x-h-list>
  <li x-h-list-item>List Item 1</li>
  <li x-h-list-item>List Item 2</li>
  <li x-h-list-item>List Item 3</li>
</ul>
```

### Interactive

```html
<ul x-h-list>
  <li x-h-list-item.interactive>List Item 1</li>
  <li x-h-list-item.interactive>List Item 2</li>
  <li x-h-list-item.interactive aria-selected="true">List Item 3</li>
</ul>
```

### With header

```html
<ul x-h-list>
  <li x-h-list-header>Group 1</li>
  <li x-h-list-item>List Item 1</li>
  <li x-h-list-item>List Item 2</li>
  <li x-h-list-item>List Item 3</li>
</ul>
```

### With icons and buttons

```html
<ul x-h-list>
  <li x-h-list-item>
    <svg x-h-icon class="size-6" data-link="/harmonia/logo/harmonia-symbolic.svg" role="presentation"></svg>
    List Item 1
    <div class="flex-1"></div>
    <button x-h-button data-variant="outline" data-size="icon-sm" aria-label="Save button">
      <i x-h-lucide role="img" data-lucide="save"></i>
    </button>
  </li>
  <li x-h-list-item>
    <svg x-h-icon class="size-6" data-link="/harmonia/logo/harmonia-symbolic.svg" role="presentation"></svg>
    List Item 2
  </li>
  <li x-h-list-item aria-selected="true">
    <svg x-h-icon class="size-6" data-link="/harmonia/logo/harmonia-symbolic.svg" role="presentation"></svg>
    List Item 3
  </li>
</ul>
```

Full docs: https://www.codbex.com/harmonia/components/list.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
