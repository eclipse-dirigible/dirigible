# Avatar

Represents a person, entity, or object using an image, icon, or text, such as a user photo, initials, or symbolic graphic. Avatars help provide visual context and identity within the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use avatars to visually identify users or related entities in lists, profiles, or collaborative features. Choose the appropriate variant based on available data - images for personal recognition, initials or icons as fallbacks.

## Directives

`x-h-avatar` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-avatar`
- `x-h-avatar-image`
- `x-h-avatar-fallback`

## API

### Attributes

#### x-h-avatar

| Attribute    | Type                                                                                                                                                 | Required | Description                                                                                                               |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------- |
| data-variant | `primary`<br />`positive`<br />`negative`<br />`warning`<br />`information`                                                                          | false    | Semantic color state                                                                                                      |
| data-color   | `white`<br />`black`<br />`red`<br />`orange`<br />`yellow`<br />`green`<br />`teal`<br />`blue`<br />`indigo`<br />`purple`<br />`pink`<br />`gray` | false    | Fills the avatar solid with a standard palette color. Overrides `data-variant`. |

## Examples

### Default

```html
<div x-h-avatar>
  <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
  <div x-h-avatar-fallback>HM</div>
</div>
```

### Big avatar

```html
<div x-h-avatar class="size-12">
  <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
  <div x-h-avatar-fallback>HM</div>
</div>
```

### Square avatar

You can change the avatar shape by using the `rounded-` classes.

```html
<div x-h-avatar class="rounded-md">
  <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
  <div x-h-avatar-fallback>HM</div>
</div>
```

### Text-only

```html
<div x-h-avatar>HM</div>
```

### Icon-only

```html
<div x-h-avatar aria-label="camera">
  <i x-h-lucide role="img" data-lucide="camera"></i>
</div>
```

### Variants

```html
<div x-h-avatar data-variant="primary">
  <svg x-h-icon data-icon="circle-user" role="img" aria-label="user"></svg>
</div>

<div x-h-avatar data-variant="information">
  <svg x-h-icon data-icon="circle-info" role="img" aria-label="info"></svg>
</div>

<div x-h-avatar data-variant="warning">
  <svg x-h-icon data-icon="circle-warning" role="img" aria-label="warning"></svg>
</div>

<div x-h-avatar data-variant="positive">
  <svg x-h-icon data-icon="circle-success" role="img" aria-label="success"></svg>
</div>

<div x-h-avatar data-variant="negative">
  <svg x-h-icon data-icon="circle-error" role="img" aria-label="error"></svg>
</div>

<div x-h-avatar data-variant="primary">PR</div>

<div x-h-avatar data-variant="information">IN</div>

<div x-h-avatar data-variant="warning">WA</div>

<div x-h-avatar data-variant="positive">PO</div>

<div x-h-avatar data-variant="negative">NE</div>
```

### Colors

Use `data-color` to fill the avatar with one of Harmonia's standard palette colors. This is independent of the semantic `data-variant` and is well suited to color-coded initials.

```html
<div x-h-avatar data-color="red">RE</div>
<div x-h-avatar data-color="orange">OR</div>
<div x-h-avatar data-color="yellow">YE</div>
<div x-h-avatar data-color="green">GR</div>
<div x-h-avatar data-color="teal">TE</div>
<div x-h-avatar data-color="blue">BL</div>
<div x-h-avatar data-color="indigo">IN</div>
<div x-h-avatar data-color="purple">PU</div>
<div x-h-avatar data-color="pink">PI</div>
<div x-h-avatar data-color="gray">GY</div>
<div x-h-avatar data-color="white">WH</div>
<div x-h-avatar data-color="black">BK</div>
```

### Interactive

To make an avatar interactive, use the `button` HTML element instead of a `span`.

```html
<button x-h-avatar>HM</button>
```

Full docs: https://www.codbex.com/harmonia/components/avatar.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
