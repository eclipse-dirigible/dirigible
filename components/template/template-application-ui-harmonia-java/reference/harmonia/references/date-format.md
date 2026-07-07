# Date Format

A behavior-only directive that renders a date value as a locale-aware date string into the element's text content. It shares the same formatting engine used by the date picker and date-time picker, so display output is consistent across the library.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Bind a date value through the directive expression, or place the directive on an element that already contains an ISO date and it will reformat that text in place. Formatting is controlled with `data-*` attributes, and the output updates reactively when the bound value or any formatting attribute changes. On a `<time>` element a machine-readable `datetime` attribute is also kept in sync.

## Directive

- `x-h-date-format`

## API

### Arguments

| Attribute              | Type                                    | Required | Description                                                                                                                                                |
| ---------------------- | --------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| expression             | Date \| string \| number \| {start,end} | false    | The value to format (a `Date`, timestamp, ISO `YYYY-MM-DD` string, or a range object). When omitted, the element's own text content is used as the source. |
| `data-locale`          | string                                  | false    | BCP-47 locale tag (e.g. `en-GB`). Defaults to the runtime locale.                                                                                          |
| `data-order`           | string                                  | false    | Field order expressed with `Y`/`M`/`D`, e.g. `DMY`. Defaults to the locale's order.                                                                        |
| `data-delimiter`       | string                                  | false    | Overrides the locale's field separator (e.g. `-`).                                                                                                         |
| `data-options`         | string (JSON)                           | false    | An `Intl.DateTimeFormat` options object, as JSON (e.g. `{"dateStyle":"long"}`).                                                                            |
| `data-range`           | boolean                                 | false    | When `"true"`, the value is treated as a `{ start, end }` object.                                                                                          |
| `data-range-separator` | string                                  | false    | String placed between start and end in range output. Defaults to `-`.                                                                                      |

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Format a bound date

```html
<div x-data="{ date: '2026-06-19' }" class="vbox gap-2">
  <input x-h-input type="date" x-model="date" />
  <p>Formatted: <time x-h-date-format="date" data-locale="en-GB"></time></p>
</div>
```

### Reformat existing content in place

With no expression, the element's current text is used as the source value.

```html
<time x-h-date-format data-locale="fr-FR">2026-06-19</time>
```

### Custom order and delimiter

```html
<span x-h-date-format="'2026-06-19'" data-order="DMY" data-delimiter="<"></span>
```

### Intl options

```html
<span x-h-date-format="'2026-06-19'" data-options='{ "dateStyle": "long" }'></span>
```

### Full ISO date-time string

Any value the `Date` constructor understands is accepted, including a full ISO 8601 timestamp. Pair it with `data-options` to show the time as well as the date.

```html
<p>Meeting: <time x-h-date-format="'2026-06-19T14:30:00'" data-locale="en-GB" data-options='{ "dateStyle": "long", "timeStyle": "short" }'></time></p>
```

### Format a range

```html
<span x-h-date-format="{ start: '2026-06-01', end: '2026-06-07' }" data-range="true" data-locale="en-US"></span>
```

Full docs: https://www.codbex.com/harmonia/utilities/date-format.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
