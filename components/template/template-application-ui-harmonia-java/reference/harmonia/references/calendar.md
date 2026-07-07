# Calendar

A full multi-view event calendar with month, week, day, and year views. Events are supplied through a reactive config object.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-calendar`

## API

### Attributes

| Attribute        | Values | Required | Description                                                                  |
| ---------------- | ------ | -------- | ---------------------------------------------------------------------------- |
| data-aria-prev   | string | false    | Sets the `aria-label` for the previous-period navigation button.             |
| data-aria-next   | string | false    | Sets the `aria-label` for the next-period navigation button.                 |
| data-aria-views  | string | false    | Sets the `aria-label` for the view switcher menu (default: `"Change view"`). |
| data-today-label | string | false    | Sets the text label for the Today button (default: `"Today"`).               |
| data-day-label   | string | false    | Sets the label for the Day view option (default: `"Day"`).                   |
| data-week-label  | string | false    | Sets the label for the Week view option (default: `"Week"`).                 |
| data-month-label | string | false    | Sets the label for the Month view option (default: `"Month"`).               |
| data-year-label  | string | false    | Sets the label for the Year view option (default: `"Year"`).                 |

### Events

| Event       | Description                                                                                                                                                                                      |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| event-click | Fired when the user clicks an event. The original event object is passed in `$event.detail.event`.                                                                                               |
| date-click  | Fired when the user clicks an empty date cell or time slot. The clicked `Date` is in `$event.detail.date`; for time-grid views the slot time string (`"HH:MM"`) is also in `$event.detail.time`. |

### Configuration

Pass a configuration object to the directive as an expression.

```html
<div x-h-calendar="calConfig" style="height: 600px"></div>
```

| Key              | Description                                                                                                                                                  |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| events           | Array of event objects. See Event object below.                                                                                             |
| view             | Initial view: `"month"` (default), `"week"`, `"day"`, or `"year"`.                                                                                           |
| date             | Initial focus date in `YYYY-MM-DD` format. Defaults to today.                                                                                                |
| locale           | BCP 47 language tag for formatting. Defaults to the user's browser locale.                                                                                   |
| firstDay         | First day of the week. `0` = Sunday (default), `1` = Monday.                                                                                                 |
| showNowIndicator | Show the current-time indicator in week and day views. Defaults to `true`. Set to `false` to hide it.                                                        |
| views            | Show the view-switcher button group in the toolbar. Defaults to `true`. Set to `false` to lock the calendar to the view set in `view` and hide the switcher. |

### Event object

Each item in the `events` array supports the following fields:

| Field       | Type    | Required | Description                                                                                                  |
| ----------- | ------- | -------- | ------------------------------------------------------------------------------------------------------------ |
| id          | string  | false    | Unique identifier for the event. Auto-generated if omitted.                                                  |
| title       | string  | true     | Display title of the event.                                                                                  |
| start       | string  | true     | Start datetime as an ISO string (`"YYYY-MM-DDTHH:MM:SS"`) or date (`"YYYY-MM-DD"` for all-day).              |
| end         | string  | false    | End datetime. Defaults to `start`. For all-day events, defaults to end of the start day.                     |
| allDay      | boolean | false    | When `true`, the event appears in the all-day strip of week/day views. Defaults to `false`.                  |
| color       | string  | false    | Color key: `blue` (default), `red`, `green`, `yellow`, `purple`, `pink`, `indigo`, `orange`, `gray`, `teal`. |
| status      | string  | false    | Pill style: `confirmed` (default) renders a filled pill; `unconfirmed` renders an outlined pill.             |
| description | string  | false    | Shown as a tooltip on event pills.                                                                           |

## Example

```html
<div x-h-calendar="calConfig" style="height: 600px"></div>
```

More examples in the docs site: Month view, Week view, Day view, Year view.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
