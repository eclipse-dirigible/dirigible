# Chip

A compact, interactive element used to represent an applied filter, a selected item, or a categorization. Chips support semantic color variants and an optional close button for dismissal.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use chips to display user-generated input or active selections that can be reviewed and removed. Always apply `x-h-chip` to a `<button>` element - this ensures proper keyboard interaction and press states. The close button (`x-h-chip-close`) must be placed on a `<span>` element inside the chip and requires an accessible label via `aria-label` or `aria-labelledby`. Chips can optionally include an icon before the label text. When a chip controls a popover or menu dropdown, the close button will automatically allow click events to propagate when the chip is in its expanded state, so the overlay can respond correctly.

## Directives

`x-h-chip` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-chip`
- `x-h-chip-close`

## API

### Attributes

#### x-h-chip

> Must be applied to a `<button>` element.

| Attribute    | Type                                                                                       | Required | Description    |
| ------------ | ------------------------------------------------------------------------------------------ | -------- | -------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline` | false    | Semantic color |

#### x-h-chip-close

> Must be applied to a `<span>` element.

| Attribute       | Type   | Required | Description                                              |
| --------------- | ------ | -------- | -------------------------------------------------------- |
| aria-label      | string | true\*   | Accessible label for the close action                    |
| aria-labelledby | string | true\*   | References an element whose text labels the close action |

> **Note:** \* One of `aria-label` or `aria-labelledby` is required.

## Examples

### Text-Only

```html
<button x-h-chip>Chip</button>
```

### Truncate Text

To enable text truncation, wrap the label in a `<span>`, `<p>`, or `<div>` element.

```html
<button x-h-chip style="max-width:4rem">
  <span>Truncate text</span>
</button>
```

### Icon & Text

```html
<button x-h-chip>
  <svg x-h-icon data-icon="mail" role="img" aria-label="mail"></svg>
  <span>Chip</span>
</button>
```

### Icon, Text & Close Button

```html
<button x-h-chip>
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Outline Variant

```html
<button x-h-chip data-variant="outline">
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Primary Variant

```html
<button x-h-chip data-variant="primary">
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Information Variant

```html
<button x-h-chip data-variant="information">
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="information"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Warning Variant

```html
<button x-h-chip data-variant="warning">
  <svg x-h-icon data-icon="circle-warning" role="img" aria-label="warning"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Positive Variant

```html
<button x-h-chip data-variant="positive">
  <svg x-h-icon data-icon="circle-success" role="img" aria-label="success"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### Negative Variant

```html
<button x-h-chip data-variant="negative">
  <svg x-h-icon data-icon="circle-error" role="img" aria-label="error"></svg>
  <span>Chip</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
```

### With Popover

```html
<button x-h-chip x-h-popover-trigger data-variant="information">
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="information"></svg>
  <span class="text-muted-foreground">Chip:</span>
  <span>Enabled</span>
  <span x-h-chip-close aria-label="remove chip"></span>
</button>
<div class="w-64 p-4" x-h-popover>Chip Popover</div>
```

Full docs: https://www.codbex.com/harmonia/components/chip.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
