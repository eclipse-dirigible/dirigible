# Template

The template directive makes it easy to insert and initialize an Alpine.js snippet inside a referenced `<template>` element. When used in combination with Alpine’s `x-for` directive, it enables recursive template rendering on the client side.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use the template directive to reuse HTML snippets and generate repeated or nested content efficiently. The template should be clearly structured and maintainable. Avoid overcomplicating recursive structures, as deeply nested templates can impact performance and readability.

> **Note:**
> Similar to Alpine's `x-for` directive, the referenced `<template>` element MUST contain only one root element.

## Directive

- `x-h-template`

## API

### Arguments

| Attribute | Type              | Required | Description                                                                                      |
| --------- | ----------------- | -------- | ------------------------------------------------------------------------------------------------ |
| `self`    | `$ref.<template>` | true     | Reference to the template that contains the code that should be inserted and initialized.        |
| x-data    | object            | true     | The properties defined in an x-data directive will be available to the rendered tempate element. |

## Examples

### Tree with recursive rendering

```html
<div x-data="app">
  <ul style="list-style-type: disc">
    <template x-for="listNode in tree" :key="listNode.id">
      <template x-h-template="$refs.listItemTemplate" x-data="{ node: listNode }"></template>
    </template>
  </ul>

  <template x-ref="listItemTemplate">
    <li>
      <span x-text="node.label"></span>

      <template x-if="node.children && node.children.length">
        <ul class="pl-8" style="list-style-type: disc">
          <template x-for="listNode in node.children" :key="listNode.id">
            <template x-h-template="$refs.listItemTemplate" x-data="{ node: listNode }"></template>
          </template>
        </ul>
      </template>
    </li>
  </template>
</div>
<script>
  Alpine.data('app', () => ({
    tree: [
      {
        id: 'docs',
        label: 'Documents',
        children: [
          {
            id: 'harmonia',
            label: 'Harmonia.pdf',
          },
          {
            id: 'alpine',
            label: 'alpine.pdf',
          },
          {
            id: 'tailwind',
            label: 'tailwind.pdf',
          },
        ],
      },
      {
        id: 'photos',
        label: 'Photos',
        children: [
          {
            id: 'holiday',
            label: 'Holiday',
            children: [
              {
                id: 'p1',
                label: 'Photo 1.jpg',
              },
              {
                id: 'p2',
                label: 'Photo 2.jpg',
              },
              {
                id: 'p3',
                label: 'Photo 3.jpg',
              },
            ],
          },
          {
            id: 'img1',
            label: 'Image 1.jpg',
          },
          {
            id: 'img2',
            label: 'Image 2.jpg',
          },
          {
            id: 'img3',
            label: 'Image 3.jpg',
          },
          {
            id: 'img4',
            label: 'Image 4.jpg',
          },
        ],
      },
      {
        id: 'hjs',
        label: 'harmonia.js',
      },
      {
        id: 'hcss',
        label: 'harmonia.css',
      },
    ],
  }));
</script>
```

Full docs: https://www.codbex.com/harmonia/utilities/template.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
