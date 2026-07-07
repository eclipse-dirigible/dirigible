# Date Time Picker

Lets users pick a date and a time together. The date is chosen from a calendar and the time is set in a compact segmented editor below it, all within a single popover so it stays usable on narrow screens.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the Date Time Picker when a single value needs both a calendar date and a time of day (e.g. scheduling an appointment). For a date only, use the Date Picker; for a time only, use the Time Picker.

The date is selected in the calendar; the time is set by focusing a segment (hour, minute, optional second, optional AM/PM) and pressing the arrow keys, like a native time field. The popover stays open while you adjust both date and time, and closes on Escape or an outside click.

## Directives

`x-h-datetime-picker` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-datetime-picker`
- `x-h-datetime-picker-trigger`
- `x-h-datetime-picker-popup`

## API

### Attributes

#### x-h-datetime-picker

| Attribute | Values             | Required | Description                               |
| --------- | ------------------ | -------- | ----------------------------------------- |
| data-size | `sm`<br/>`default` | false    | Changes the size of the date time picker. |

#### Modifiers

| Modifier | Description                          |
| -------- | ------------------------------------ |
| table    | Use when the input is inside a table |

#### x-h-datetime-picker-popup

| Attribute           | Values | Required | Description                                                              |
| ------------------- | ------ | -------- | ------------------------------------------------------------------------ |
| data-align          | string | false    | Aligns the popover relative to the trigger (e.g. `bottom-start`, `top`). |
| data-label-time     | string | false    | `aria-label` for the time editor group (default `"Time"`).               |
| data-label-hours    | string | false    | `aria-label` for the hour segment (default `"Hour"`).                    |
| data-label-minutes  | string | false    | `aria-label` for the minute segment (default `"Minute"`).                |
| data-label-seconds  | string | false    | `aria-label` for the second segment (default `"Second"`).                |
| data-label-meridiem | string | false    | `aria-label` for the AM/PM segment (default `"AM/PM"`).                  |
| data-label-now      | string | false    | Text label for the Now button (default `"Now"`).                         |

### Model

When using `x-model`, the picker reads and writes a single ISO date-time string with a `T` separator, matching a native `<input type="datetime-local">`:

```js
'2025-06-25T14:30'; // without seconds
'2025-06-25T14:30:00'; // with seconds
```

The time is always stored in 24-hour form regardless of the display format. The model is set only when both a date and a complete time have been chosen; until then it is an empty string.

### Configuration

You can pass a configuration object to the popup as an expression or as a value. It accepts every Date Picker calendar key (`locale`, `firstDay`, `min`, `max`, `options`, `delimiter`, `order`) plus the time keys below.

| Key      | Description                                                                                  |
| -------- | -------------------------------------------------------------------------------------------- |
| is12Hour | When `true`, the time editor shows a 12-hour clock with an AM/PM segment.                    |
| seconds  | When `true`, the time editor includes a seconds segment. Inferred from the model when unset. |

### Validation timing

By default this control shows native-constraint errors (for example `required`) only after the user interacts with it or attempts to submit, not on page load. To validate on load instead, set `data-validate="immediate"` on a wrapping `x-h-fieldset`, `x-h-field`, or any ancestor element. Setting `aria-invalid="true"` yourself always shows the error immediately. See Fieldset for details.

## Keyboard Handling

In the calendar grid:

- `Up` / `Down` / `Left` / `Right` - Move the focused day.
- `Enter` / `Space` - Select the focused day.
- `Home` / `End` - First / last day of the month.
- `PageUp` / `PageDown` - Previous / next month.

In the time editor:

- `Tab` / `Left` / `Right` - Move between the hour, minute, second and AM/PM segments.
- `0`-`9` - Type the value directly; the segment fills and focus auto-advances to the next one, just like a native time input. On touch devices the numeric keyboard is shown.
- `A` / `P` - Set the AM/PM segment (or tap it to toggle).
- `Up` / `Down` - Increase / decrease the focused segment (wraps around).
- `Home` / `End` - Set the focused segment to its minimum / maximum.
- `Backspace` / `Delete` - Clear the focused segment.
- `Esc` - Close the popover.

## Accessibility

The calendar is exposed as an ARIA date grid (see the Inline Calendar accessibility notes). Each time segment is a `spinbutton` with its own label and `aria-valuemin` / `aria-valuemax` / `aria-valuenow` / `aria-valuetext`, so assistive technology announces it and its current value. The trigger advertises `aria-haspopup="dialog"` and its expanded state.

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

```html
<div x-h-datetime-picker x-data="{ dt: '' }">
  <input type="text" id="datetime-input-1" />
  <button x-h-datetime-picker-trigger aria-label="Choose date and time"></button>
  <div x-h-datetime-picker-popup x-model="dt"></div>
</div>
```

### 12-hour with seconds

```html
<div x-h-datetime-picker x-data="{ dt: '' }">
  <input type="text" id="datetime-input-2" />
  <button x-h-datetime-picker-trigger aria-label="Choose date and time"></button>
  <div x-h-datetime-picker-popup="{ is12Hour: true, seconds: true }" x-model="dt"></div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/datetime-picker.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
