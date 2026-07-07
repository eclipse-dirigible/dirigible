# Rating

Lets users view and set a star rating. Supports half-star precision, a read-only display mode, keyboard input, and binds to a number through `x-model`.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-rating`

## API

### Attributes

| Attribute       | Values                      | Required | Description                                                                                                                  |
| --------------- | --------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------- |
| data-max        | number                      | false    | Number of stars (default: `5`).                                                                                              |
| data-precision  | `half`<br/>`full`           | false    | Smallest selectable increment (default: `half`).                                                                             |
| data-size       | `sm`<br/>`default`<br/>`lg` | false    | Size of the stars (default: `default`).                                                                                      |
| data-color      | string                      | false    | Star color, one of Harmonia's standard colors (e.g. `red`, `green`, `blue`). Defaults to `yellow`. |
| data-value      | number                      | false    | Initial value when no `x-model` is bound.                                                                                    |
| data-readonly   | boolean                     | false    | Renders a non-interactive display of the value.                                                                              |
| disabled        | boolean                     | false    | Renders a dimmed, non-interactive rating.                                                                                    |
| data-label      | string                      | false    | Accessible name for the rating (default: `"Rating"`).                                                                        |
| data-aria-empty | string                      | false    | Text announced when the value is 0 (default: `"No rating"`).                                                                 |

### Model

Bind a number with `x-model`. The value updates on selection (click, drag, or keyboard) and is rounded to the configured precision. Clicking the current value again clears the rating to `0`.

### Events

| Event  | Description                                                             |
| ------ | ----------------------------------------------------------------------- |
| change | Fired when the value changes. The new value is in `event.detail.value`. |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-rating data-readonly data-value="4.5" data-max="5" data-label="Average rating"></div>
```

More examples in the docs site: Default (half-star), Whole stars, Custom color, Larger, ten stars.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
