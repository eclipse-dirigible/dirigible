# Rating

Lets users view and set a star rating. Supports half-star precision, a read-only display mode, keyboard input, and binds to a number through `x-model`.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use a Rating to capture or display a subjective score, such as a product review or a satisfaction level. Keep the scale small and consistent (five stars is the familiar default) and label what is being rated. Use the read-only mode to show an existing average or a score the user cannot change, and reserve the interactive mode for collecting input. For choosing one option from a set that is not a score, use Radio instead.

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

## Keyboard Handling

When interactive, the rating is focusable and behaves like a slider:

- `Right` / `Up` - Increase the rating by one step.
- `Left` / `Down` - Decrease the rating by one step.
- `Home` - Clear the rating (set to 0).
- `End` - Set the maximum rating.

The step is half a star by default, or a whole star when `data-precision="full"`.

## Accessibility

Interactive ratings expose `role="slider"` with `aria-valuemin` / `aria-valuemax` / `aria-valuenow` and a descriptive `aria-valuetext` (e.g. "3.5 of 5 stars"). Read-only ratings render as `role="img"` with the score as the accessible name. The star icons themselves are decorative. Set an accessible name with `data-label` (or `aria-label`) describing what is being rated.

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Default (half-star)

```html
<div x-data="{ score: 2.5 }" class="flex items-center gap-3">
  <div x-h-rating x-model="score"></div>
  <span x-text="score"></span>
</div>
```

### Whole stars

```html
<div x-data="{ score: 3 }">
  <div x-h-rating x-model="score" data-precision="full"></div>
</div>
```

### Custom color

```html
<div x-data="{ score: 4 }">
  <div x-h-rating x-model="score" data-color="red"></div>
</div>
```

### Read-only

```html
<div x-h-rating data-readonly data-value="4.5" data-max="5" data-label="Average rating"></div>
```

### Larger, ten stars

```html
<div x-data="{ score: 7 }">
  <div x-h-rating x-model="score" data-max="10" data-size="lg"></div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/rating.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
