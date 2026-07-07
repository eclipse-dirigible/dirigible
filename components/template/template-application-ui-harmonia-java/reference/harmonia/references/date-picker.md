# Date Picker

Allows users to enter a date either by typing it directly or by selecting it from a calendar popover. The component combines text input flexibility with a visual calendar to simplify accurate date selection.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-date-picker` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-date-picker`
- `x-h-date-picker-trigger`
- `x-h-date-picker-popup`

## API

### Attributes

#### x-h-date-picker

| Attribute | Values             | Required | Description                          |
| --------- | ------------------ | -------- | ------------------------------------ |
| data-size | `sm`<br/>`default` | false    | Changes the size of the date picker. |

#### x-h-date-picker-popup

| Attribute            | Values                                                                                                                                                                        | Required | Description                                                          |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | -------------------------------------------------------------------- |
| data-align           | `bottom-start`<br/>`bottom`<br/>`bottom-end`<br/>`right-start`<br/>`right`<br/>`right-end`<br/>`left-start`<br/>`left`<br/>`left-end`<br/>`top-start`<br/>`top`<br/>`top-end` | false    | Aligns the calendar popup relative to the date picker trigger.       |
| data-aria-prev-year  | string                                                                                                                                                                        | false    | Sets the `aria-label` attribute value for the previous year button.  |
| data-aria-prev-month | string                                                                                                                                                                        | false    | Sets the `aria-label` attribute value for the previous month button. |
| data-aria-next-month | string                                                                                                                                                                        | false    | Sets the `aria-label` attribute value for the next month button.     |
| data-aria-next-year  | string                                                                                                                                                                        | false    | Sets the `aria-label` attribute value for the next year button.      |

### Modifiers

| Modifier | Description                          |
| -------- | ------------------------------------ |
| table    | Use when the input is inside a table |

### Configuration

You can pass a configuration object to the popup as an expression or as a value.

| Key            | Description                                                                                                                                                                                                          |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| locale         | The locale of the calendar as a BCP 47 language tag. If not provided, it's automatically set from the user preferences.                                                                                              |
| firstDay       | The start day of the week. `0` is Sunday.                                                                                                                                                                            |
| min            | The earliest date selectable. Must be provided in the standard ISO 8601 format - `YYYY-MM-DD`.                                                                                                                       |
| max            | The latest date selectable. Must be provided in the standard ISO 8601 format - `YYYY-MM-DD`.                                                                                                                         |
| options        | [Intl.DateTimeFormat](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#options) options.                                                          |
| delimiter      | Custom separator character between day, month, and year in the display format (e.g. `"-"`). Does not affect the model value.                                                                                         |
| order          | Custom display order of the date parts as a three-character string of `Y` (year), `M` (month), `D` (day) (e.g. `"MDY"` for month-day-year). Defaults to the locale's natural order. Does not affect the model value. |
| range          | When `true`, the picker selects a start-and-end date range instead of a single date. See Range selection.                                                                                        |
| rangeSeparator | Text placed between the two dates in the display input when `range` is enabled. Defaults to `" - "`.                                                                                                                 |

### Model

The date picker reads and writes dates as `YYYY-MM-DD` strings (e.g. `"2025-06-09"`), matching the value format of a native `<input type="date">`. The display format shown in the text input is separate and can be customised via the `options` key in the calendar config.

### Display format

By default the input displays the date using the user's locale. To customise it, pass [Intl.DateTimeFormat options](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#options) via the `options` key on `x-h-date-picker-popup`. The model value always remains `YYYY-MM-DD` regardless of the display format.

```html
<div x-h-date-picker ...>
  <input type="text" />
  <button x-h-date-picker-trigger aria-label="Choose date"></button>
  <div x-h-date-picker-popup="{ options: { day: '2-digit', month: '2-digit', year: 'numeric' } }" x-model="date"></div>
</div>
```

Manual input typed by the user is parsed using the configured display format. For formats where the month appears as a word rather than a number, parsing falls back to the browser's native `Date` constructor.

### Range selection

Set `range: true` on the popup config to let the user pick a start-and-end date range. The first click selects the start, the second completes the range (picks are ordered automatically, so clicking an earlier day second still produces a valid range). With the keyboard, press `Enter` once to set the start and again to set the end.

In range mode the model value is an object with `start` and `end` keys (each a `YYYY-MM-DD` string), and the input displays both dates joined by the `rangeSeparator` (default `" - "`):

```js
{ start: '2025-06-09', end: '2025-06-16' }
```

```html
<div x-h-date-picker x-data="{ range: { start: '', end: '' } }">
  <input type="text" id="date-input-range" />
  <button x-h-date-picker-trigger aria-label="Choose date range"></button>
  <div x-h-date-picker-popup="{ range: true }" x-model="range"></div>
</div>
```

### Validation timing

By default this control shows native-constraint errors (for example `required`) only after the user interacts with it or attempts to submit, not on page load. To validate on load instead, set `data-validate="immediate"` on a wrapping `x-h-fieldset`, `x-h-field`, or any ancestor element. Setting `aria-invalid="true"` yourself always shows the error immediately. See Fieldset for details.

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-date-picker ...>
  <input type="text" />
  <button x-h-date-picker-trigger aria-label="Choose date"></button>
  <div x-h-date-picker-popup="{ options: { day: '2-digit', month: '2-digit', year: 'numeric' } }" x-model="date"></div>
</div>
```

More examples in the docs site: Range selection, Examples, With locale, With custom display format.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
