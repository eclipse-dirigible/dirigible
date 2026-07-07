# Line Chart

`x-h-chart-line` draws a line chart from a single reactive configuration object. Multiple series are overlaid on a shared axis. Charts inherit the active theme colors and adapt to light and dark mode automatically.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Give the chart a container with an explicit height (charts fill their parent). Provide one or more `series`, each a list of numeric `data` points, and a matching `labels` array naming the points along the axis. Use a line chart to show a trend across an ordered sequence; to compare discrete categories use a Bar Chart, and for parts of a whole use a Pie Chart.

## Directive

- `x-h-chart-line`

## API

### Configuration

| Key           | Type                                  | Default       | Description                                                 |
| ------------- | ------------------------------------- | ------------- | ----------------------------------------------------------- |
| `series`      | `{ name?, color?, data: number[] }[]` | `[]`          | One entry per line. Multiple series are overlaid.           |
| `labels`      | string[]                              | `[]`          | Label for each data index.                                  |
| `legend`      | boolean                               | `true`        | Show the color/label key.                                   |
| `axes`        | boolean                               | `true`        | Show the numeric axis ticks and labels.                     |
| `gridlines`   | boolean                               | `true`        | Show gridlines behind the lines.                            |
| `tooltip`     | boolean                               | `true`        | Show a tooltip on hover and emit interaction events.        |
| `dataLabels`  | boolean                               | `false`       | Draw each point's value next to it.                         |
| `tickCount`   | number                                | `5`           | Target number of numeric axis ticks.                        |
| `valueFormat` | `(value) => string`                   | locale number | Formats values in tooltips and numeric axis ticks.          |
| `palette`     | string[]                              | theme tokens  | Color tokens cycled for series without an explicit `color`. |

A series `color` (and the `palette` entries) is one of the standard color names: `red`, `orange`, `yellow`, `green`, `teal`, `blue`, `indigo`, `purple`, `pink`, `gray`, `white`, or `black`.

### Accessibility

The chart is exposed to assistive technologies as a `figure` with a visually-hidden data table of its values, so screen-reader users get the underlying numbers (the visual lines, axes, and legend are marked decorative). It defaults to the accessible name "Line chart". Set an `aria-label` attribute on the element to give it a more meaningful name.

### Events

When `tooltip` is enabled, hovering and clicking points emit bubbling `CustomEvent`s on the chart element: `chart-hover`, `chart-leave`, and `chart-click`. See the events reference for the shared `detail` shape.

## Examples

### Basic

```html
<div style="height: 20rem" x-h-chart-line="{ labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'], series: [{ name: 'Visitors', data: [120, 200, 150, 280, 240] }] }"></div>
```

### Multiple series

```html
<div
  style="height: 20rem"
  x-h-chart-line="{
    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
    series: [
      { name: 'Visitors', data: [120, 200, 150, 280, 240] },
      { name: 'Signups', data: [40, 60, 55, 90, 80] }
    ]
  }"
></div>
```

Full docs: https://www.codbex.com/harmonia/charts/line.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
