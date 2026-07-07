# Range

Allows users to select a value, or a range of values, by dragging a handle along a track. The component is based on [noUiSlider](https://github.com/leongersen/noUiSlider) and an interactive control over numeric inputs.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-range`

## API

### Attributes

Please refer to the [noUiSlider documentation](https://refreshless.com/nouislider/)

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-range="config" x-data="rangeData" auto-hide-tips="true" x-model="range"></div>
<script>
  Alpine.data('rangeData', () => ({
    range: [20, 80],
    config: {
      orientation: 'horizontal',
      start: [3, 6],
      connect: true,
      range: { min: 1, max: 8 },
      step: 1,
      tooltips: true,
      pips: { mode: 'steps' },
    },
  }));
</script>
```

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
