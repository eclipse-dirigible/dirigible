# Badge

Displays a short label used to convey the semantic status of an object. Badges use color and, optionally, an icon to provide quick visual cues and reinforce meaning at a glance.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use badges to highlight status, category, or state in a compact and non-intrusive way. Select the appropriate semantic variant to match the context and intent. Avoid overusing badges or relying on color alone and ensure the meaning remains clear through text or iconography. The outline variant can be used when a more subtle visual emphasis is required.

## Directives

`x-h-badge` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-badge`
- `x-h-badge-indicator`

## API

### Attributes

#### x-h-badge

| Attribute    | Type                                                                                       | Required | Description    |
| ------------ | ------------------------------------------------------------------------------------------ | -------- | -------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`<br />`outline` | false    | Semantic color |

#### x-h-badge-indicator

| Attribute     | Type                                                                        | Required | Description                                                           |
| ------------- | --------------------------------------------------------------------------- | -------- | --------------------------------------------------------------------- |
| data-variant  | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information` | false    | Semantic color (defaults to `primary`)                                |
| data-size     | `default`<br />`sm`                                                         | false    | Indicator size (defaults to `default`)                                |
| data-position | `top-right`<br />`top-left`<br />`bottom-left`<br />`bottom-right`          | false    | Corner of the host the indicator anchors to (defaults to `top-right`) |
| data-dot      | `true`                                                                      | false    | Renders a small dot without content instead of a labelled badge       |
| data-ping     | `true`                                                                      | false    | Adds a pulsing ping animation behind the indicator                    |

## Examples

### Badge

```html
<div x-h-badge>Badge</div>
<div x-h-badge data-variant="primary">Primary</div>
<div x-h-badge data-variant="positive"><i x-h-lucide role="img" data-lucide="check"></i>Positive</div>
<div x-h-badge data-variant="negative"><i x-h-lucide role="img" data-lucide="x"></i>Negative</div>
<div x-h-badge data-variant="warning"><i x-h-lucide role="img" data-lucide="siren"></i>Warning</div>
<div x-h-badge data-variant="information"><i x-h-lucide role="img" data-lucide="info"></i>Information</div>
<div x-h-badge data-variant="outline">Outline</div>
<a x-h-badge href="#">Link</a>
```

### Badge Indicator

The badge indicator can be used on any element as long as it that element's position is relative (or has the `relative` class).

- Button badge with text

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with badge">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator>121</span>
</button>
```

- Button badge as dot

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with badge dot">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-dot="true"></span>
</button>
```

- Avatar with badge

```html
<div x-h-avatar class="relative">
  <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
  <div x-h-avatar-fallback>HM</div>
  <span x-h-badge-indicator data-dot="true"></span>
</div>
```

- Small avatar with small badge

```html
<div x-h-avatar class="relative size-5!">
  <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
  <div x-h-avatar-fallback>HM</div>
  <span x-h-badge-indicator data-dot="true" data-size="sm"></span>
</div>
```

- Dot badge with ping animation

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with badge dot ping">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-dot="true" data-ping="true"></span>
</button>
```

- Negative variant badge indicator

```html
<button class="relative" x-h-button data-variant="outline">
  Tasks
  <span x-h-badge-indicator data-variant="negative">4</span>
</button>
```

- Small size

The `data-size="sm"` variant renders a more compact indicator, both for labelled badges and dots.

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with small badge">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-size="sm">9</span>
</button>
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Icon button with small badge dot">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-size="sm" data-dot="true"></span>
</button>
```

- Positions

Use `data-position` to anchor the indicator to any corner of the host.

```html
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Badge top-right">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-position="top-right" data-dot="true"></span>
</button>
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Badge top-left">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-position="top-left" data-dot="true"></span>
</button>
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Badge bottom-left">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-position="bottom-left" data-dot="true"></span>
</button>
<button class="relative" x-h-button data-variant="outline" data-size="icon" aria-label="Badge bottom-right">
  <svg x-h-icon data-icon="bell" role="img" aria-label="bell"></svg>
  <span x-h-badge-indicator data-position="bottom-right" data-dot="true"></span>
</button>
```

Full docs: https://www.codbex.com/harmonia/components/badge.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
