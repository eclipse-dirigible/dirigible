# Inline Calendar

A compact calendar for selecting a single date or a date range within a monthly context. The component provides navigation between months and years.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the inline calendar when users need to choose specific dates, such as scheduling events but do not need a date input or a fullscreen calendar. For filtering data by date, use a Date Picker. Make sure it is paired with clear labels and context to prevent confusion, especially when selecting critical dates.

## Directive

- `x-h-calendar-inline`

## API

### Attributes

| Attribute            | Values | Required | Description                                                          |
| -------------------- | ------ | -------- | -------------------------------------------------------------------- |
| data-aria-prev-year  | string | false    | Sets the `aria-label` attribute value for the previous year button.  |
| data-aria-prev-month | string | false    | Sets the `aria-label` attribute value for the previous month button. |
| data-aria-next-month | string | false    | Sets the `aria-label` attribute value for the next month button.     |
| data-aria-next-year  | string | false    | Sets the `aria-label` attribute value for the next year button.      |

### Model

When using `x-model`, the calendar reads and writes dates as `YYYY-MM-DD` strings (e.g. `"2025-06-09"`). Set the bound variable to a `YYYY-MM-DD` string to pre-select a date, or to an empty string for no initial selection. On every selection the model is updated to the newly selected date in the same `YYYY-MM-DD` format.

Full ISO datetime strings (e.g. from `new Date().toISOString()`) are also accepted as input, but initialising with `YYYY-MM-DD` is recommended to avoid timezone-related date drift.

In range mode the model is an object `{ start, end }` instead of a single string.

### Events

| Event  | Description                                                                                                                                                                                              |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| change | Triggered when the selection changes. In single mode the selected `Date` is passed in `event.detail.date`; in range mode the `Date` endpoints are passed in `event.detail.start` and `event.detail.end`. |

### Configuration

You can pass a configuration object to the calendar as an expression or as a value.

Example:

```html
<div x-h-calendar-inline="calConfig"></div>
<script>
  Alpine.data('controller', () => ({
    calConfig: { locale: 'en-US', firstDay: 1, min: '2025-01-10', max: '2025-12-20' },
  }));
</script>
```

| Key       | Description                                                                                                                                                                                                          |
| --------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| locale    | The locale of the calendar as a BCP 47 language tag. If not provided, it's automatically set from the user preferences.                                                                                              |
| firstDay  | The start day of the week. `0` is Sunday.                                                                                                                                                                            |
| min       | The earliest date selectable. Must be provided in the standard ISO 8601 format - `YYYY-MM-DD`.                                                                                                                       |
| max       | The latest date selectable. Must be provided in the standard ISO 8601 format - `YYYY-MM-DD`.                                                                                                                         |
| options   | [Intl.DateTimeFormat](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat/DateTimeFormat#options) options.                                                          |
| delimiter | Custom separator character between day, month, and year in the display format (e.g. `"-"`). Does not affect the model value.                                                                                         |
| order     | Custom display order of the date parts as a three-character string of `Y` (year), `M` (month), `D` (day) (e.g. `"MDY"` for month-day-year). Defaults to the locale's natural order. Does not affect the model value. |
| range     | When `true`, the calendar selects a start-and-end date range instead of a single date. See Range selection.                                                                               |

## Keyboard Handling

The user can use the following keyboard shortcuts in order to navigate through the calendar:

- `Up` / `Down` - Moves focus to the day above/below the current day.
- `Right` - Moves focus to the next day.
- `Left` - Moves focus to the previous day.
- `Enter` / `Space` - Selects the focused day.
- `Home` - Selects the first day of the month.
- `End` - Selects the last day of the month.
- `PageUp` - Selects the same or closest day of the previous month.
- `PageDown` - Selects the same or closest day of the next month.

## Accessibility

The calendar is exposed as an ARIA date grid: the month/year heading names the grid (announced via `aria-live` as it changes), weekday columns are column headers, and each day is a grid cell with roving focus and `aria-selected` / `aria-disabled` / `aria-current="date"` (today) state. Navigation buttons are labeled (override the defaults with the `data-aria-*` attributes below).

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Change event

```html
<div x-data>
  <div x-h-calendar-inline @change="console.log('Selected:', $event.detail.date)"></div>
</div>
```

### Locale and first day config

```html
<div
  x-data="{
  caldate: '',
  init() {
    const d = new Date();
    this.caldate = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
  }
}"
>
  <div x-h-calendar-inline="{ locale: 'en-US', firstDay: 1 }" x-model="caldate"></div>
</div>
```

### Range selection {#range-selection-inline}

Set `range: true` to let the user pick a date range. The first selection sets the start, the second completes the range (picks are ordered automatically). With the keyboard, press `Enter` once to set the start and again to set the end.

In range mode the `x-model` value is an object with `start` and `end` keys (each a `YYYY-MM-DD` string), and the `change` event detail is `{ start, end }` (`Date` objects):

```js
{ start: '2025-06-09', end: '2025-06-16' }
```

```html
<div x-data="{ dateRange: { start: '', end: '' } }">
  <div x-h-calendar-inline="{ range: true, firstDay: 1 }" x-model="dateRange"></div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/inline-calendar.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
