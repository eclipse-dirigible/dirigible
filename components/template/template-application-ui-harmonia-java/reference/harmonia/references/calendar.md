# Calendar

A full multi-view event calendar with month, week, day, and year views. Events are supplied through a reactive config object.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use `x-h-calendar` when users need to view and navigate a schedule - appointments, team calendars, project timelines, and so on.

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

## Keyboard Handling

In the month view (and within each year-view mini-month) the day cells form an ARIA grid with roving focus:

- `Up` / `Down` - Move focus a week earlier/later.
- `Left` / `Right` - Move focus to the previous/next day (crossing month boundaries).
- `Home` / `End` - Move focus to the first/last day of the month.
- `PageUp` / `PageDown` - Move focus to the previous/next month.
- `Enter` / `Space` - Fire `date-click` for the focused day (year view: open that day in day view).

Events are buttons in the tab order; activate them to fire `event-click`. In the month view, the "+N more" overflow opens a dialog that moves focus to its event list and returns focus to the trigger on `Escape`.

## Accessibility

The calendar is a labeled `group` (default name "Calendar"; set an `aria-label` attribute to override). The toolbar period heading is an `aria-live` region; the month grid uses `role="grid"`/`row`/`gridcell` with `aria-current="date"` on today and full keyboard navigation; events are `button`s whose accessible label includes the title, time (or "all day"), and status (e.g. "unconfirmed"). The week/day time grid's empty-slot "click to pick a time" is a pointer-only convenience.

## Examples

### Month view

```html
<div
  x-data="{
  cal: {},
  init() {
    const today = new Date().toISOString().slice(0, 10);
    const tomorrow = new Date(new Date().setDate(new Date().getDate() + 1)).toISOString().slice(0, 10);
    this.cal = {
      view: 'month',
      events: [
        { id: '1', title: 'Team Sync', start: today + 'T10:00:00', end: today + 'T11:00:00', color: 'blue' },
        { id: '2', title: 'Company Meeting', start: today + 'T10:00:00', end: today + 'T11:00:00', status: 'unconfirmed', color: 'blue' },
        { id: '3', title: 'All Hands', start: today, allDay: true, color: 'green' },
        { id: '4', title: 'Off-site', start: today + 'T08:00:00', end: tomorrow + 'T18:00:00', color: 'purple' },
      ],
    };
  }
}"
  x-h-calendar="cal"
  style="height: 560px"
  @event-click="console.log('event clicked:', $event.detail.event)"
  @date-click="console.log('date clicked:', $event.detail.date)"
></div>
```

### Week view

```html
<div
  x-data="{
  cal: {},
  init() {
    const today = new Date().toISOString().slice(0, 10);
    const tomorrow = new Date(new Date().setDate(new Date().getDate() + 1)).toISOString().slice(0, 10);
    this.cal = {
      view: 'week',
      events: [
        { id: '1', title: 'Team Sync', start: today + 'T09:00:00', end: today + 'T10:00:00', color: 'blue' },
        { id: '2', title: 'Design Review', start: today + 'T09:30:00', end: today + 'T10:30:00', color: 'purple' },
        { id: '3', title: 'Lunch with Client', start: today + 'T12:00:00', end: today + 'T13:30:00', color: 'green' },
        { id: '4', title: 'Off-site', start: today, end: tomorrow, allDay: true, color: 'orange' },
        { id: '5', title: 'Budget Review', start: today + 'T15:00:00', end: today + 'T16:00:00', color: 'red', status: 'unconfirmed' },
      ],
    };
  }
}"
  x-h-calendar="cal"
  style="height: 560px"
  @event-click="console.log('event clicked:', $event.detail.event)"
  @date-click="console.log('date clicked:', $event.detail.date, $event.detail.time)"
></div>
```

### Day view

```html
<div
  x-data="{
  cal: {},
  init() {
    const today = new Date().toISOString().slice(0, 10);
    this.cal = {
      view: 'day',
      events: [
        { id: '1', title: 'Stand-up', start: today + 'T09:00:00', end: today + 'T09:15:00', color: 'blue' },
        { id: '2', title: 'Sprint Planning', start: today + 'T10:00:00', end: today + 'T12:00:00', color: 'indigo' },
        { id: '3', title: 'Lunch', start: today + 'T12:00:00', end: today + 'T13:00:00', color: 'green' },
        { id: '4', title: '1:1 with Manager', start: today + 'T14:00:00', end: today + 'T14:30:00', color: 'teal' },
        { id: '5', title: 'Code Review', start: today + 'T14:00:00', end: today + 'T15:00:00', color: 'orange' },
        { id: '6', title: 'Release Call', start: today + 'T16:00:00', end: today + 'T17:00:00', color: 'red', status: 'unconfirmed' },
      ],
    };
  }
}"
  x-h-calendar="cal"
  style="height: 560px"
  @event-click="console.log('event clicked:', $event.detail.event)"
  @date-click="console.log('date clicked:', $event.detail.date, $event.detail.time)"
></div>
```

### Year view

```html
<div
  x-data="{
  cal: {},
  init() {
    const today = new Date().toISOString().slice(0, 10);
    this.cal = {
      view: 'year',
      events: [
        { id: '1', title: 'Team Sync', start: today + 'T10:00:00', end: today + 'T11:00:00', color: 'blue' },
        { id: '2', title: 'All Hands', start: today, allDay: true, color: 'green' },
      ],
    };
  }
}"
  x-h-calendar="cal"
  style="height: 560px"
></div>
```

Full docs: https://www.codbex.com/harmonia/components/calendar.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
