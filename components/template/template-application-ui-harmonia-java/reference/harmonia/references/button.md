# Button

Buttons can trigger an action or navigate the user to another location. They communicate intent through labeling, iconography and semantic styling.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-button`

## API

### Attributes

| Attribute    | Type                                                                                                                      | Required | Description                                                                                                |
| ------------ | ------------------------------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline`<br />`transparent`<br />`link` | false    | Changes the color/shape of the button. Can be used to indicate different states.                           |
| data-size    | `sm`<br />`md`<br />`icon-sm`<br />`icon-md`<br />`icon`<br />`default`                                                   | false    | Changes the size of the button. When the button contains only an icon, the `icon-*` values should be used. |
| data-toggled | boolean                                                                                                                   | false    | Set the toggle state.                                                                                      |

### Modifiers

| Modifier | Description                                         |
| -------- | --------------------------------------------------- |
| addon    | Used when the button is inside an input group addon |

## Example

```html
<button x-h-button>Default</button>
```

More examples in the docs site: Primary, Positive, Negative, Warning, Information, Outline, Transparent, Link, Toggle button, Button with icons, Button with spinner, Icon button, Small, Medium.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
