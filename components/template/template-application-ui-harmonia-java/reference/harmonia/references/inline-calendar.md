# Inline Calendar

A compact calendar for selecting a single date or a date range within a monthly context. The component provides navigation between months and years.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

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

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-data>
  <div x-h-calendar-inline @change="console.log('Selected:', $event.detail.date)"></div>
</div>
```

More examples in the docs site: Configuration, Locale and first day config, Range selection {#range-selection-inline}.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
