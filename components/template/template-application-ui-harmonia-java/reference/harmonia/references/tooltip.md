# Tooltip

A small pop-up that provides additional information or context about an interface element, displayed on hover. Tooltips offer brief guidance without taking up permanent space in the layout.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use tooltips to clarify controls, explain icons, or provide contextual hints. Keep the content concise and relevant, and avoid placing critical information exclusively in tooltips, as they may be inaccessible on touch devices or overlooked by users.

## Directives

`x-h-tooltip` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-tooltip`
- `x-h-tooltip-trigger`

## Examples

```html
<button x-h-button x-h-tooltip-trigger>Button</button>
<div x-h-tooltip>
  Buttons are clickable
  <div x-h-tag-group>
    <div x-h-tag>Ctrl</div>
    <span>+</span>
    <div x-h-tag>B</div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/tooltip.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
