# Popover

Displays supplementary information or content in a compact overlay without navigating away from the current page. Popovers provide contextual details or actions while maintaining focus on the primary interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-popover` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

## Example

```html
<button x-h-button x-h-popover-trigger>Popover</button>
<div class="w-64 p-4" x-h-popover>Popover content</div>
```

More examples in the docs site: Constrained width, Disable closing on inner click events, Bind the open state (two-way), Manually open and close the popover, Chevron, Alignment.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
