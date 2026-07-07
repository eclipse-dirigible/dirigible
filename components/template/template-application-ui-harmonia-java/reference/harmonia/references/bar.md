# Bar Chart

`x-h-chart-bar` draws a bar chart from a single reactive configuration object. Bars can be oriented as columns or rows, grouped, or stacked. Charts inherit the active theme colors and adapt to light and dark mode automatically.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-chart-bar`

## API

### Configuration

| Key           | Type                                  | Default       | Description                                                   |
| ------------- | ------------------------------------- | ------------- | ------------------------------------------------------------- |
| `series`      | `{ name?, color?, data: number[] }[]` | `[]`          | One entry per series. Multiple series render as grouped bars. |
| `labels`      | string[]                              | `[]`          | Category label for each data index.                           |
| `orientation` | `'vertical'` \| `'horizontal'`        | `'vertical'`  | `vertical` draws columns, `horizontal` draws rows.            |
| `stacked`     | boolean                               | `false`       | Stack series on top of one another instead of grouping them.  |
| `legend`      | boolean                               | `true`        | Show the color/label key.                                     |
| `axes`        | boolean                               | `true`        | Show the numeric axis ticks and category labels.              |
| `gridlines`   | boolean                               | `true`        | Show gridlines behind the bars.                               |
| `tooltip`     | boolean                               | `true`        | Show a tooltip on hover and emit interaction events.          |
| `dataLabels`  | boolean                               | `false`       | Draw each bar's value on the bar.                             |
| `tickCount`   | number                                | `5`           | Target number of numeric axis ticks.                          |
| `valueFormat` | `(value) => string`                   | locale number | Formats values in tooltips and numeric axis ticks.            |
| `palette`     | string[]                              | theme tokens  | Color tokens cycled for series without an explicit `color`.   |

A series `color` (and the `palette` entries) is one of the standard color names: `red`, `orange`, `yellow`, `green`, `teal`, `blue`, `indigo`, `purple`, `pink`, `gray`, `white`, or `black`.

### Accessibility

The chart is exposed to assistive technologies as a `figure` with a visually-hidden data table of its values, so screen-reader users get the underlying numbers (the visual bars, axes, and legend are marked decorative). It defaults to the accessible name "Bar chart". Set an `aria-label` attribute on the element to give it a more meaningful name.

### Events

When `tooltip` is enabled, hovering and clicking bars emit bubbling `CustomEvent`s on the chart element: `chart-hover`, `chart-leave`, and `chart-click`. See the events reference for the shared `detail` shape.

## Example

```html
<div style="height: 20rem" x-h-chart-bar="{ labels: ['Jan', 'Feb', 'Mar', 'Apr'], series: [{ name: 'Revenue', data: [12, 19, 7, 15] }] }"></div>
```

More examples in the docs site: Grouped, Stacked, Horizontal, Value labels, Handling events.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
