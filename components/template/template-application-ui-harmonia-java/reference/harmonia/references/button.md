# Button

Buttons can trigger an action or navigate the user to another location. They communicate intent through labeling, iconography and semantic styling.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use buttons to represent clear, intentional actions. Select the appropriate semantic variant to match the action’s intent, and use outline, transparent, or link variants for lower-emphasis actions. The primary button should be used for the main or suggested action. For example, the "Create" button on a "New File" dialog should be the primary one. Avoid overloading interfaces with too many primary buttons.

## Directive

- `x-h-button`

## API

### Attributes

| Attribute    | Type                                                                                                                      | Required | Description                                                                                                |
| ------------ | ------------------------------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline`<br />`transparent`<br />`link` | false    | Changes the color/shape of the button. Can be used to indicate different states.                           |
| data-size    | `sm`<br />`md`<br />`icon-sm`<br />`icon-md`<br />`icon`<br />`default`                                                   | false    | Changes the size of the button. When the button contains only an icon, the `icon-*` values should be used. |
| data-toggled | boolean                                                                                                                   | false    | Set the toggle state.                                                                                      |

### Modifiers

| Modifier | Description                                         |
| -------- | --------------------------------------------------- |
| addon    | Used when the button is inside an input group addon |

## Examples

### Default

<br />

```html
<button x-h-button>Default</button>
```

### Primary

<br />

```html
<button x-h-button data-variant="primary">Primary</button>
```

### Positive

<br />

```html
<button x-h-button data-variant="positive">Positive</button>
```

### Negative

<br />

```html
<button x-h-button data-variant="negative">Negative</button>
```

### Warning

<br />

```html
<button x-h-button data-variant="warning">Warning</button>
```

### Information

<br />

```html
<button x-h-button data-variant="information">Information</button>
```

### Outline

<br />

```html
<button x-h-button data-variant="outline">Outline</button>
```

### Transparent

<br />

```html
<button x-h-button data-variant="transparent">Transparent</button>
```

### Link

<br />

```html
<a x-h-button data-variant="link" href="#">Link</a>
```

### Toggle button

```html
<div x-data="{ toggled: true }">
  <button x-h-button :data-toggled="toggled" @click="toggled = !toggled">Toggle</button>
</div>
```

### Button with icons

You can include an icon directly inside the button.

```html
<button x-h-button>
  <i x-h-lucide role="img" data-lucide="chevron-left"></i>
  Left-aligned
</button>
<button x-h-button>
  <i x-h-lucide role="img" data-lucide="chevron-right"></i>
  Right-aligned
</button>
<button x-h-button>
  <svg x-h-icon data-icon="search" role="img" aria-label="search"></svg>
  Search
</button>
```

### Button with spinner

You can include a spinner directly inside the button. The spinner will adjust its color based on the button variant.

```html
<button x-h-button>
  <span x-h-spinner></span>
  <span>Saving...</span>
</button>
<button x-h-button data-variant="primary">
  <span x-h-spinner></span>
  <span>Saving...</span>
</button>
```

### Icon button

```html
<button x-h-button data-size="icon" aria-label="Icon button">
  <i x-h-lucide role="img" data-lucide="save"></i>
</button>
```

### Button sizes

#### Small

```html
<button x-h-button data-size="sm">
  <i x-h-lucide role="img" data-lucide="save"></i>
  Save
</button>
<button x-h-button data-size="icon-sm" aria-label="Icon button">
  <i x-h-lucide role="img" data-lucide="save"></i>
</button>
```

#### Medium

```html
<button x-h-button data-size="md">
  <i x-h-lucide role="img" data-lucide="save"></i>
  Save
</button>
<button x-h-button data-size="icon-md" aria-label="Icon button">
  <i x-h-lucide role="img" data-lucide="save"></i>
</button>
```

Full docs: https://www.codbex.com/harmonia/components/button.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
