# Date Format

A behavior-only directive that renders a date value as a locale-aware date string into the element's text content. It shares the same formatting engine used by the date picker and date-time picker, so display output is consistent across the library.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

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

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<time x-h-date-format data-locale="fr-FR">2026-06-19</time>
```

More examples in the docs site: Format a bound date, Custom order and delimiter, Intl options, Full ISO date-time string, Format a range.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
