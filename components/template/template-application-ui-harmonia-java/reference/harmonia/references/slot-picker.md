# Slot Picker

An inline calendar that shows three consecutive days, each with a grid of selectable time slots. Designed for touch-friendly interaction and works on all screen sizes.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-slot-picker`

## API

### Attributes

| Attribute              | Values | Required | Description                                                                           |
| ---------------------- | ------ | -------- | ------------------------------------------------------------------------------------- |
| data-aria-prev         | string | false    | Sets the `aria-label` for the previous button.                                        |
| data-aria-next         | string | false    | Sets the `aria-label` for the next button.                                            |
| data-aria-calendar     | string | false    | Sets the `aria-label` for the calendar button and popover (default: `"Choose date"`). |
| data-today-label       | string | false    | Overrides the label of the "Today" navigation button.                                 |
| data-unavailable-label | string | false    | Overrides the "Not available" label shown for fully disabled days.                    |

### Configuration

Pass a configuration object as an Alpine expression.

```html
<div x-h-slot-picker="myConfig"></div>
```

| Key           | Default     | Description                                                                                                                                                                                                               |
| ------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| date          | today       | The starting date of the 3-day window. Accepts a `YYYY-MM-DD` string or a `Date` object.                                                                                                                                  |
| start         | `'08:00'`   | The first time slot of the day as `HH:MM`. Used in shorthand mode (when `slots` is not provided).                                                                                                                         |
| end           | `'18:00'`   | The exclusive end time as `HH:MM`. Used in shorthand mode.                                                                                                                                                                |
| step          | `60`        | Duration of each slot in minutes. Used in shorthand mode.                                                                                                                                                                 |
| slots         | -           | Explicit array of slot objects (see below). When provided, it overrides `start`, `end`, and `step` on a per-day basis.                                                                                                    |
| fillEmptyDays | `false`     | When `true`, days that have no entry in `slots` fall back to the generated `start`/`end`/`step` schedule instead of showing nothing. Use it to mix explicit per-day slots with a default schedule for the remaining days. |
| multiple      | `false`     | When `true`, multiple slots can be selected simultaneously.                                                                                                                                                               |
| locale        | user locale | BCP 47 language tag for day names and the date display (e.g. `'en-US'`, `'de-DE'`).                                                                                                                                       |
| disabledDates | `[]`        | Array of `'YYYY-MM-DD'` strings and/or `{ from, to }` range objects. Matching days show "Not available" instead of slots.                                                                                                 |
| disabledDays  | `[]`        | Array of weekday numbers to always disable (0 = Sunday, 6 = Saturday).                                                                                                                                                    |
| minDate       | -           | Start day. When set, the user cannot page to any day before it. Accepts a `YYYY-MM-DD` string or a `Date`. Independent of `maxDate`.                                                                                      |
| maxDate       | -           | End day. When set, the user cannot page to any day after it. Accepts a `YYYY-MM-DD` string or a `Date`. Independent of `minDate`.                                                                                         |

#### Slot object (explicit mode)

| Key       | Type             | Description                                                                                                                          |
| --------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| date      | string           | The date of the slot in `YYYY-MM-DD` format.                                                                                         |
| start     | string           | Start time in `HH:MM` format.                                                                                                        |
| end       | string           | End time in `HH:MM` format.                                                                                                          |
| available | boolean          | When `false`, the slot is shown as unavailable and unclickable.                                                                      |
| icon      | `{ url, alt }`   | An image rendered as a badge in the top-right corner of the cell. `url` is the image path; `alt` is the alt text (defaults to `''`). |
| icons     | `{ url, alt }[]` | An array of badge images. Takes precedence over `icon` when both are set.                                                            |

### Model

When used with `x-model`, the bound value follows the selection mode:

- **Single mode** (`multiple: false`): a `'YYYY-MM-DDTHH:MM'` string (e.g. `'2026-06-22T09:00'`), or `null` when nothing is selected.
- **Multiple mode** (`multiple: true`): an array of `'YYYY-MM-DDTHH:MM'` strings, or an empty array.

### Events

| Event      | Description                                                                                                                                                               |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| slot-click | Dispatched on every slot click (including deselection). `event.detail.slot` contains `date`, `start`, `end`, `available`, and `selected` (the new state after the click). |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-slot-picker="myConfig"></div>
```

More examples in the docs site: Basic (single select), Multi-select with 30-minute slots, Explicit slots with availability and icon badges, Default schedule with per-day overrides, Disabled weekdays and date ranges, Start and end day bounds.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
