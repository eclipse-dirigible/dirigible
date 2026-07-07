# Slot Picker

An inline calendar that shows three consecutive days, each with a grid of selectable time slots. Designed for touch-friendly interaction and works on all screen sizes.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the Slot Picker when users need to book or choose one or more time slots from an upcoming schedule, for example booking appointments, selecting meeting windows, or configuring availability.

Navigate between days with the previous/next buttons (which move three days at a time) or jump straight to any date with the calendar button in the toolbar; the chosen date becomes the first of the three visible days, which avoids paging far ahead one step at a time.

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

## Accessibility

The picker is a labeled `group` (default name "Time slot picker"; set an `aria-label` attribute to override). Each day is its own `group` labeled by its header, so the day is announced for the slots inside it. Available slots are toggle `button`s with a day + time `aria-label` and `aria-pressed` reflecting selection; unavailable slots are marked `aria-disabled` with a hidden "Not available" note. Selecting a slot updates the cell in place rather than re-rendering, so keyboard focus stays on the chosen slot. The calendar button opens a `dialog` containing a fully keyboard-navigable date grid; picking a date moves the visible range and returns focus to the button, and `Esc` closes it.

## Binding

Binds through Alpine `x-model`. See the Examples for the expected value shape.

## Examples

### Basic (single select)

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: null,
    init() {
      const today = new Date().toISOString().slice(0, 10);
      this.config = { date: today, start: '09:00', end: '17:00', step: 60 };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

### Multi-select with 30-minute slots

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: [],
    init() {
      const today = new Date().toISOString().slice(0, 10);
      this.config = { date: today, start: '08:00', end: '12:00', step: 30, multiple: true };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

### Explicit slots with availability and icon badges

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: null,
    init() {
      const dateIn = (days) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toISOString().slice(0, 10);
      };
      this.config = {
        date: dateIn(0),
        multiple: true,
        slots: [
          { date: dateIn(0), start: '09:00', end: '09:30', available: true },
          { date: dateIn(0), start: '09:30', end: '10:00', available: false },
          { date: dateIn(0), start: '10:00', end: '10:30', available: true, icon: { url: '/harmonia/logo/harmonia-circle.svg', alt: 'Harmonia' } },
          { date: dateIn(0), start: '10:30', end: '11:00', available: true },
          { date: dateIn(1), start: '09:00', end: '09:30', available: true },
          { date: dateIn(1), start: '09:30', end: '10:00', available: true, icon: { url: '/harmonia/logo/harmonia-circle.svg', alt: 'Harmonia' } },
          { date: dateIn(1), start: '10:00', end: '10:30', available: false },
          { date: dateIn(1), start: '10:30', end: '11:00', available: true },
          { date: dateIn(2), start: '09:00', end: '09:30', available: false },
          { date: dateIn(2), start: '09:30', end: '10:00', available: true },
          { date: dateIn(2), start: '10:00', end: '10:30', available: true },
          { date: dateIn(2), start: '10:30', end: '11:00', available: false },
        ],
      };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

### Default schedule with per-day overrides

Provide `start`, `end`, and `step` for the default daily schedule, list `slots` only for the days you want to customize, and set `fillEmptyDays: true` so every other day still shows the default slots. A day that appears in `slots` shows only its explicit slots (it is not merged with the default schedule).

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: null,
    init() {
      const dateIn = (days) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toISOString().slice(0, 10);
      };
      this.config = {
        date: dateIn(0),
        start: '09:00',
        end: '17:00',
        step: 60,
        fillEmptyDays: true,
        slots: [
          { date: dateIn(0), start: '10:00', end: '10:30', available: true },
          { date: dateIn(0), start: '10:30', end: '11:00', available: true },
          { date: dateIn(0), start: '11:00', end: '11:30', available: false },
        ],
      };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

### Disabled weekdays and date ranges

Use `disabledDays` to block recurring days (e.g. weekends) and `disabledDates` for specific dates or ranges.

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: null,
    init() {
      const dateIn = (days) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toISOString().slice(0, 10);
      };
      this.config = {
        date: dateIn(0),
        start: '09:00',
        end: '17:00',
        step: 60,
        disabledDays: [0, 6],
        disabledDates: [
          dateIn(5),
          { from: dateIn(5), to: dateIn(10) },
        ],
      };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

### Start and end day bounds

Set `minDate` to a start day and/or `maxDate` to an end day to stop the user paging outside a window. The two options are independent, so you can set just one. The previous/next buttons disable at the edges, and jumping via the calendar is clamped so the visible range always stays within the bounds.

```html
<div
  x-h-slot-picker="config"
  x-data="{
    config: {},
    selected: null,
    init() {
      const dateIn = (days) => {
        const d = new Date();
        d.setDate(d.getDate() + days);
        return d.toISOString().slice(0, 10);
      };
      this.config = {
        date: dateIn(0),
        start: '09:00',
        end: '17:00',
        step: 60,
        minDate: dateIn(0),
        maxDate: dateIn(10),
      };
    }
  }"
  x-model="selected"
  class="rounded-md"
></div>
```

Full docs: https://www.codbex.com/harmonia/components/slot-picker.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
