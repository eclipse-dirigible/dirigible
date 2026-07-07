# Accordion

Organizes related content into expandable and collapsible sections, allowing users to reveal or hide information as needed while keeping the interface clean and compact.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use accordions to group related content that doesn’t need to be visible all at once, such as FAQs, settings panels, or detailed lists. Section headers must be clear and descriptive. Avoid nesting too many accordions, as excessive collapsible layers can reduce usability and overwhelm users.

## Directives

`x-h-accordion` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-accordion`
- `x-h-accordion-item`
- `x-h-accordion-trigger`
- `x-h-accordion-content`

## API

### Attributes

#### x-h-accordion

| Attribute | Type                          | Required | Description                           |
| --------- | ----------------------------- | -------- | ------------------------------------- |
| data-size | `default`<br />`md`<br />`sm` | false    | Height of the accordion header items. |

#### x-h-accordion-item

| Attribute | Type   | Required | Description                                                             |
| --------- | ------ | -------- | ----------------------------------------------------------------------- |
| `self`    | string | false    | Sets the ID of the item. Useful when setting the default expanded item. |

#### x-h-accordion-trigger

| Attribute | Type   | Required | Description                                                                        |
| --------- | ------ | -------- | ---------------------------------------------------------------------------------- |
| `self`    | string | true     | Sets the title of the item. Expects a string literal or a reference to a variable. |

### Modifiers

#### x-h-accordion

| Modifier | Type   | Required | Description                                                                                                                                 |
| -------- | ------ | -------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| single   | string | false    | Used when the accordion must show only one section at a time. Optionally, the id of the item that should be expanded by default can be set. |

#### x-h-accordion-item

| Modifier | Type   | Required | Description                                                                                                                 |
| -------- | ------ | -------- | --------------------------------------------------------------------------------------------------------------------------- |
| default  | string | false    | Accordion items are collapsed by default. If included, the item will be expanded by default. There can be only one default. |

## Examples

### Show only one section at a time

```html
<div x-h-accordion.single="itemId2">
  <div x-h-accordion-item="itemId1">
    <h3 x-h-accordion-trigger="'Accordion Item 1'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
  <div x-h-accordion-item="itemId2">
    <h3 x-h-accordion-trigger="'Accordion Item 2'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
</div>
```

### Default section

```html
<div x-h-accordion>
  <div x-h-accordion-item>
    <h3 x-h-accordion-trigger="'Accordion Item 1'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
  <div x-h-accordion-item.default>
    <h3 x-h-accordion-trigger="'Accordion Item 2'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
</div>
```

### Medium size

```html
<div x-h-accordion data-size="md">
  <div x-h-accordion-item>
    <h3 x-h-accordion-trigger="'Accordion Item 1'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
  <div x-h-accordion-item>
    <h3 x-h-accordion-trigger="'Accordion Item 2'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
</div>
```

### Small size

```html
<div x-h-accordion data-size="sm">
  <div x-h-accordion-item>
    <h3 x-h-accordion-trigger="'Accordion Item 1'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
  <div x-h-accordion-item>
    <h3 x-h-accordion-trigger="'Accordion Item 2'"></h3>
    <div x-h-accordion-content>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
      Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    </div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/accordion.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
