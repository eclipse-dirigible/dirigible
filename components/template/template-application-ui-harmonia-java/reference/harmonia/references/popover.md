# Popover

Displays supplementary information or content in a compact overlay without navigating away from the current page. Popovers provide contextual details or actions while maintaining focus on the primary interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use popovers to show contextual help, tips, or additional options related to a specific element. Avoid using popovers as dropdowns and use the Menu component for selectable lists or navigation. The trigger should be clearly indicated.

## Directives

`x-h-popover` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-popover`
- `x-h-popover-trigger`

## API

### Attributes

#### x-h-popover-trigger

| Attribute | Type    | Required | Description                                                                                                                                                                                                                                                                                                                                           |
| --------- | ------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `self`    | boolean | false    | Optional boolean variable bound to the open state. On its own it stays in sync two-way: the popover still opens and closes automatically (toggle on trigger click, dismiss on outside click), and you can also show or hide it by setting the variable. Add your own `@click` handler on the trigger to take full manual control instead (see below). |

#### x-h-popover

| Attribute        | Type                                                                                                                                                                          | Required | Description                                                                                                                                                                                                                                                             |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| data-align       | `bottom-start`<br/>`bottom`<br/>`bottom-end`<br/>`right-start`<br/>`right`<br/>`right-end`<br/>`left-start`<br/>`left`<br/>`left-end`<br/>`top-start`<br/>`top`<br/>`top-end` | false    | Aligns the popover body relative to the trigger.                                                                                                                                                                                                                        |
| data-innerclicks | boolean                                                                                                                                                                       | false    | Prevents the popover from closing when there is a click inside it.<br />This is not a dynamic attribute. Its value is read on initialization and it is not monitored for changes.                                                                                       |
| data-max-w       | `3xs`<br/>`2xs`<br/>`xs`<br/>`sm`<br/>`md`<br/>`lg`<br/>`xl`<br/>`2xl`<br/>`3xl`<br/>`4xl`<br/>`5xl`<br/>`6xl`<br/>`7xl`<br/>`8xl`<br/>`9xl`<br/>`10xl`                       | false    | Sets the popover's maximum width to the matching container size (for example `md` sets `max-width: var(--container-md)`). The popover still sizes to its content. This only limits how wide it can get. Without it, the maximum is the width available in the viewport. |

### Modifiers

#### x-h-popover-trigger

| Modifier | Description                                              |
| -------- | -------------------------------------------------------- |
| chevron  | Rotates the last icon inside the trigger at 180 degrees. |

#### x-h-popover

| Modifier  | Description                                 |
| --------- | ------------------------------------------- |
| no-scroll | Used when the popover body must not scroll. |

## Examples

### Popover

```html
<button x-h-button x-h-popover-trigger>Popover</button>
<div class="w-64 p-4" x-h-popover>Popover content</div>
```

### Constrained width

The popover sizes to its content, up to a maximum width. That maximum defaults to the width available in the viewport. Set `data-max-w` to a container size to cap it smaller (here `sm`), so long content wraps instead of stretching the popover wider.

```html
<button x-h-button x-h-popover-trigger>Popover</button>
<div class="p-4" x-h-popover data-max-w="sm">Long content wraps once the popover reaches the small container width instead of getting any wider.</div>
```

### Disable closing on inner click events

```html
<button x-h-button x-h-popover-trigger>Popover</button>
<div class="w-64 p-4" x-h-popover data-innerclicks="true">
  <button x-h-button data-variant="primary">Click</button>
</div>
```

### Bind the open state (two-way)

Bind a variable to the trigger to read or control the open state while keeping the automatic behavior. The popover still toggles on click and dismisses on an outside click, and setting the variable elsewhere shows or hides it. This is useful to close the popover from a button inside it (for example a "confirm" action) while other inner controls leave it open.

```html
<div x-data="{ open: false }">
  <button x-h-button x-h-popover-trigger="open">Popover</button>
  <div class="vbox w-64 gap-2 p-4" x-h-popover data-innerclicks="true">
    <span>Clicking confirm closes the popover.</span>
    <button x-h-button data-variant="primary" data-size="sm" @click="open = false">Confirm</button>
  </div>
</div>
```

### Manually open and close the popover

Add your own `@click` handler on the trigger, alongside the bound variable, to take full manual control. The directive then leaves open/close entirely to you, so the automatic outside-click dismiss is disabled.

```html
<div x-data="{ open: false }">
  <button x-h-button x-h-popover-trigger="open" @click="open = !open">Popover</button>
  <div class="w-64 p-4" x-h-popover>Popover content</div>
</div>
```

### Chevron

In order to use the chevron modifier, the trigger label must be placed inside a nested element (usually a `span`).

```html
<button x-h-button x-h-popover-trigger.chevron>
  <span>Popover</span>
  <i x-h-lucide role="img" data-lucide="chevron-down"></i>
</button>
<div class="p-4" x-h-popover>With chevron</div>
```

### Alignment

```html
<div class="flex flex-col" style="gap: 4rem">
  <div class="flex items-center justify-between gap-4">
    <button x-h-button x-h-popover-trigger>Bottom start</button>
    <div class="w-64 p-4" x-h-popover data-align="bottom-start">Bottom start content</div>
    <button x-h-button x-h-popover-trigger>Bottom</button>
    <div class="w-64 p-4" x-h-popover data-align="bottom">Bottom center content</div>
    <button x-h-button x-h-popover-trigger>Bottom end</button>
    <div class="w-64 p-4" x-h-popover data-align="bottom-end">Bottom end content</div>
  </div>
  <div class="flex items-center justify-between gap-4">
    <button x-h-button x-h-popover-trigger>Right start</button>
    <div class="w-64 p-4" x-h-popover data-align="right-start">Right start content</div>
    <button x-h-button x-h-popover-trigger>Left start</button>
    <div class="w-64 p-4" x-h-popover data-align="left-start">Left start content</div>
  </div>
  <div class="flex items-center justify-between gap-4">
    <button x-h-button x-h-popover-trigger>Right</button>
    <div class="w-64 p-4" x-h-popover data-align="right">Right center content</div>
    <button x-h-button x-h-popover-trigger>Left</button>
    <div class="w-64 p-4" x-h-popover data-align="left">Left center content</div>
  </div>
  <div class="flex items-center justify-between gap-4">
    <button x-h-button x-h-popover-trigger>Right end</button>
    <div class="w-64 p-4" x-h-popover data-align="right-end">Right end content</div>
    <button x-h-button x-h-popover-trigger>Left end</button>
    <div class="w-64 p-4" x-h-popover data-align="left-end">Left end content</div>
  </div>
  <div class="flex items-center justify-between gap-4">
    <button x-h-button x-h-popover-trigger>Top start</button>
    <div class="w-64 p-4" x-h-popover data-align="top-start">Top start content</div>
    <button x-h-button x-h-popover-trigger>Top</button>
    <div class="w-64 p-4" x-h-popover data-align="top">Top center content</div>
    <button x-h-button x-h-popover-trigger>Top end</button>
    <div class="w-64 p-4" x-h-popover data-align="top-end">Top end content</div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/popover.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
