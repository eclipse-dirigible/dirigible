# List

A container that displays a collection of related items in a structured format. Lists help organize content clearly and improve readability by grouping similar elements together.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-list` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-list`
- `x-h-list-item`
- `x-h-list-header`

## API

### Modifiers

#### x-h-list-item

| Modifier    | Description                                     |
| ----------- | ----------------------------------------------- |
| interactive | Makes the list item interactive and selectable. |

## Example

```html
<ul x-h-list>
  <li x-h-list-item>List Item 1</li>
  <li x-h-list-item>List Item 2</li>
  <li x-h-list-item>List Item 3</li>
</ul>
```

More examples in the docs site: Interactive, With header, With icons and buttons.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
