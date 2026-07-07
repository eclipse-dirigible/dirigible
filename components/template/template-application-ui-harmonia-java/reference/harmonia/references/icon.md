# Icon

Renders an SVG graphic, either from a link or a set of built-in icons, that can represent an action, status, or decorative element. By default, it applies the fill-current class, allowing the icon to inherit the current text color.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-icon`

## API

### Attributes

| Attribute | Type                     | Required | Description                                                                                                                                                                                                                                                                                                                                                                     |
| --------- | ------------------------ | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-icon | string                   | false    | Name of a built-in icon to render (see the list below). Reactive: bind it with `x-bind:data-icon` and the rendered icon updates whenever the value changes. Ignored when `data-link` is set.                                                                                                                                                                                    |
| data-link | url                      | false    | URL to the svg icon.                                                                                                                                                                                                                                                                                                                                                            |
| role      | `img`<br/>`presentation` | true     | The role of the icon. This is required as it affects the accessibility.<br />The `presentation` role excludes the icon from being visible to assistive technologies.<br />When using the `img` role, either `aria-label` or `aria-labelledby` attribute must also be provided. If not, assistive technologies will have trouble conveying to the user what the icon represents. |

### Built-in icons

Harmonia includes several built-in icons. Instead of using the `data-link` attribute, set `data-icon` to one of the names below to render it.

| Name           | Description                            |
| -------------- | -------------------------------------- |
| calendar       | Calendar icon                          |
| check          | Check icon                             |
| chevron-down   | Chevron down icon                      |
| chevron-left   | Chevron left icon                      |
| chevron-right  | Chevron right icon                     |
| chevrons-left  | Chevrons left icon                     |
| chevrons-right | Chevrons right icon                    |
| clock          | Clock icon                             |
| search         | Search icon                            |
| ellipsis       | Ellipsis icon                          |
| minus          | Minus icon                             |
| plus           | Plus icon                              |
| close          | Close/Cancel icon                      |
| bell           | Bell icon                              |
| trash          | Delete/Trash icon                      |
| mail           | Mail icon                              |
| send           | Send icon                              |
| export         | Export/Download icon                   |
| import         | Import/Upload icon                     |
| edit           | Edit icon                              |
| menu           | Menu icon                              |
| reply          | Reply icon                             |
| refresh        | Refresh icon                           |
| circle-info    | Information icon in a circle           |
| circle-warning | Warning icon in a circle               |
| circle-error   | Error icon in a circle                 |
| circle-success | Success icon in a circle               |
| circle-unknown | Unknown/Question mark icon in a circle |
| circle-user    | User icon in a circle                  |
| home           | Home icon                              |
| star           | Filled star icon                       |
| star-hollow    | Hollow/outline star icon               |
| star-half      | Half-filled star icon                  |

## Example

```html
<svg x-h-icon data-link="/harmonia/logo/harmonia.svg" role="img" aria-label="Harmonia logo"></svg>
```

More examples in the docs site: SVG icon, SVG icon with custom fill color, Harmonia Icons.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
