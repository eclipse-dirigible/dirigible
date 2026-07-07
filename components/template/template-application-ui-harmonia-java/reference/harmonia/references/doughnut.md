# Doughnut Chart

`x-h-chart-doughnut` draws a doughnut chart (a pie with a hole in the middle) from a single reactive configuration object. Charts inherit the active theme colors and adapt to light and dark mode automatically.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-chart-doughnut`

## API

### Configuration

| Key             | Type                              | Default       | Description                                                                         |
| --------------- | --------------------------------- | ------------- | ----------------------------------------------------------------------------------- |
| `slices`        | `{ label, value, color? }[]`      | required      | The slices to draw. Only positive values are shown.                                 |
| `series`        | `{ data: number[] }[]` + `labels` | required      | Alternative to `slices`: the first series' values become slices, named by `labels`. |
| `cutout`        | number                            | `0.6`         | Hole size as a fraction of the radius (clamped to `0.2`-`0.9`).                     |
| `legend`        | boolean                           | `true`        | Show the color/label key.                                                           |
| `tooltip`       | boolean                           | `true`        | Show a tooltip on hover and emit interaction events.                                |
| `dataLabels`    | boolean                           | `true`        | Draw each slice's percentage on the ring (hidden for slices under 5%).              |
| `labelPosition` | `'inside'` \| `'outside'`         | `'inside'`    | Place the percentage labels on the ring or just outside the edge.                   |
| `valueFormat`   | `(value) => string`               | locale number | Formats values in tooltips.                                                         |
| `palette`       | string[]                          | theme tokens  | Color tokens cycled for slices without an explicit `color`.                         |

A slice `color` (and the `palette` entries) is one of the standard color names: `red`, `orange`, `yellow`, `green`, `teal`, `blue`, `indigo`, `purple`, `pink`, `gray`, `white`, or `black`.

### Accessibility

The chart is exposed to assistive technologies as a `figure` with a visually-hidden data table of its segments and values, so screen-reader users get the underlying numbers (the visual ring and legend are marked decorative). It defaults to the accessible name "Doughnut chart". Set an `aria-label` attribute on the element to give it a more meaningful name.

### Events

When `tooltip` is enabled, hovering and clicking slices emit bubbling `CustomEvent`s on the chart element. See the events reference for the shared `detail` shape.

## Example

```html
<div
  class="aspect-square h-full"
  style="max-height: 20rem"
  x-h-chart-doughnut="{
    slices: [
      { label: 'Direct', value: 40 },
      { label: 'Referral', value: 25 },
      { label: 'Social', value: 20 },
      { label: 'Other', value: 15 }
    ]
  }"
></div>
```

More examples in the docs site: Thinner ring.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
