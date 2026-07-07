# Date Time Picker

Lets users pick a date and a time together. The date is chosen from a calendar and the time is set in a compact segmented editor below it, all within a single popover so it stays usable on narrow screens.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-datetime-picker` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-datetime-picker x-data="{ dt: '' }">
  <input type="text" id="datetime-input-1" />
  <button x-h-datetime-picker-trigger aria-label="Choose date and time"></button>
  <div x-h-datetime-picker-popup x-model="dt"></div>
</div>
```

More examples in the docs site: 12-hour with seconds.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
