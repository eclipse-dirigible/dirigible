# Pie Chart

`x-h-chart-pie` draws a pie chart from a single reactive configuration object. Charts inherit the active theme colors and adapt to light and dark mode automatically.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Give the chart a container with an explicit height (charts fill their parent). Provide the `slices` to draw, each with a `label` and a `value`. Use a pie chart to show how parts make up a whole; to compare discrete categories use a Bar Chart, and for trends over an ordered sequence use a Line Chart.

## Directive

- `x-h-chart-pie`

## API

### Configuration

| Key             | Type                              | Default       | Description                                                                         |
| --------------- | --------------------------------- | ------------- | ----------------------------------------------------------------------------------- |
| `slices`        | `{ label, value, color? }[]`      | required      | The slices to draw. Only positive values are shown.                                 |
| `series`        | `{ data: number[] }[]` + `labels` | required      | Alternative to `slices`: the first series' values become slices, named by `labels`. |
| `legend`        | boolean                           | `true`        | Show the color/label key.                                                           |
| `tooltip`       | boolean                           | `true`        | Show a tooltip on hover and emit interaction events.                                |
| `dataLabels`    | boolean                           | `true`        | Draw each slice's percentage on the slice (hidden for slices under 5%).             |
| `labelPosition` | `'inside'` \| `'outside'`         | `'inside'`    | Place the percentage labels inside each slice or just outside the slice edge.       |
| `valueFormat`   | `(value) => string`               | locale number | Formats values in tooltips.                                                         |
| `palette`       | string[]                          | theme tokens  | Color tokens cycled for slices without an explicit `color`.                         |

A slice `color` (and the `palette` entries) is one of the standard color names: `red`, `orange`, `yellow`, `green`, `teal`, `blue`, `indigo`, `purple`, `pink`, `gray`, `white`, or `black`.

### Accessibility

The chart is exposed to assistive technologies as a `figure` with a visually-hidden data table of its segments and values, so screen-reader users get the underlying numbers (the visual slices and legend are marked decorative). It defaults to the accessible name "Pie chart". Set an `aria-label` attribute on the element to give it a more meaningful name.

### Events

When `tooltip` is enabled, hovering and clicking data emit bubbling `CustomEvent`s on the chart element. These events are shared by all chart types.

| Event         | Fired when                            |
| ------------- | ------------------------------------- |
| `chart-hover` | The pointer enters a bar/point/slice. |
| `chart-leave` | The pointer leaves a bar/point/slice. |
| `chart-click` | A bar/point/slice is clicked.         |

Each event's `detail` is:

```js
{
  type: 'bar' | 'point' | 'slice',
  seriesName: string | undefined,
  seriesIndex: number,
  categoryIndex: number, // data index (bar/line) or slice index (pie)
  label: string,
  value: number,
  color: string          // standard color name
}
```

Clicking or tapping a data point pins its tooltip open (useful on touchscreens, where there is no hover). The pinned tooltip stays until another point is clicked or a press lands elsewhere.

## Examples

### Basic

```html
<div
  class="aspect-square h-full"
  style="max-height: 20rem"
  x-h-chart-pie="{
    slices: [
      { label: 'Direct', value: 40 },
      { label: 'Referral', value: 25 },
      { label: 'Social', value: 20 },
      { label: 'Other', value: 15 }
    ]
  }"
></div>
```

### Labels outside the slices

```html
<div
  class="aspect-square h-full"
  style="max-height: 20rem"
  x-h-chart-pie="{
    labelPosition: 'outside',
    slices: [
      { label: 'Direct', value: 40 },
      { label: 'Referral', value: 25 },
      { label: 'Social', value: 20 },
      { label: 'Other', value: 15 }
    ]
  }"
></div>
```

Full docs: https://www.codbex.com/harmonia/charts/pie.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
