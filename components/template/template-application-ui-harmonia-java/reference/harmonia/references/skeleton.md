# Skeleton

A placeholder component used to indicate that content is loading. Skeletons provide a visual cue to users, reducing perceived wait times and improving the overall experience.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use skeletons to temporarily fill the space of content that is being fetched or rendered. Make sure the placeholder visually resembles the final content layout to maintain context. Avoid overusing skeletons for static or instant-loading content, as this can create unnecessary visual noise.

## Directive

- `x-h-skeleton`

## API

### Attributes

| Attribute | Type                          | Required | Description                                                                   |
| --------- | ----------------------------- | -------- | ----------------------------------------------------------------------------- |
| data-size | `sm`<br />`md`<br />`default` | false    | Height of the skeleton. Works only when combined with the `control` modifier. |

### Modifiers

| Modifier | Description                                             |
| -------- | ------------------------------------------------------- |
| control  | Takes the shape of a control (like inputs and buttons). |
| card     | Takes the shape of a card or tile.                      |
| avatar   | Takes the shape of an avatar component.                 |

## Examples

```html
<div class="flex flex-col gap-2">
  <div x-h-skeleton.avatar></div>
  <div x-h-skeleton.card class="tile-sm"></div>
  <div x-h-skeleton.control class="w-1/2"></div>
  <div x-h-skeleton class="h-12 w-full"></div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/skeleton.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
