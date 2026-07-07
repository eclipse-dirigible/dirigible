# Progress

Visually represents the completion status of an ongoing operation, providing users with feedback on progress and expected duration.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-progress`

## API

### Attributes

| Attribute    | Type                                                         | Required | Description                                                            |
| ------------ | ------------------------------------------------------------ | -------- | ---------------------------------------------------------------------- |
| `self`       | number                                                       | true     | Sets the progress. Used as %.                                          |
| data-type    | `line`<br />`circle`                                         | false    | Renders the progress as a horizontal bar (default) or a circular ring. |
| data-loading | boolean                                                      | false    | Shows an indefinite loading animation instead of the value.            |
| data-variant | `positive`<br />`negative`<br />`warning`<br />`information` | false    | Semantic color state                                                   |

## Example

```html
<div x-h-progress="40" data-type="circle" style="width:6rem"></div>
<div x-h-progress="70" data-type="circle" data-variant="positive" style="width:6rem"></div>
```

More examples in the docs site: Examples, Circle with text, Loading.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
