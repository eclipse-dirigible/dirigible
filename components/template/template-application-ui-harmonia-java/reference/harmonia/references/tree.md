# Tree

Displays hierarchical data in a structured, expandable format, allowing users to explore nested items efficiently. Tree components provide a clear and intuitive way to navigate complex relationships or grouped content.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-tree` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-tree`
- `x-h-tree-item`
- `x-h-tree-button`

## API

### Attributes

#### x-h-tree

| Attribute   | Type    | Required | Description                                                               |
| ----------- | ------- | -------- | ------------------------------------------------------------------------- |
| data-border | boolean | false    | Adds a right border to the subtree, indicating which items are inside it. |

#### x-h-button

| Attribute      | Type                                                         | Required | Description                                        |
| -------------- | ------------------------------------------------------------ | -------- | -------------------------------------------------- |
| data-indicator | `positive`<br />`negative`<br />`warning`<br />`information` | false    | Adds an indicator on the right side of the button. |

### Modifiers

#### x-h-tree

| Modifier | Description                   |
| -------- | ----------------------------- |
| sub      | Used when the tree is nested. |

#### x-h-tree-item

| Attribute | Type    | Required | Description                      |
| --------- | ------- | -------- | -------------------------------- |
| expanded  | boolean | false    | Sets the default expanded state. |

## Example

```html
<ul x-h-tree>
  <li x-h-tree-item.expanded="true">
    <button x-h-tree-button data-indicator="positive">
      <i x-h-lucide role="img" data-lucide="folder"></i>
      <span>Folder 1</span>
    </button>
    <ul x-h-tree.sub data-border="true">
      <li x-h-tree-item>
        <button x-h-tree-button>
          <i x-h-lucide role="img" data-lucide="file-text"></i>
          <span>File 1</span>
        </button>
      </li>
      <li x-h-tree-item.expanded="true">
        <button x-h-tree-button>
          <i x-h-lucide role="img" data-lucide="folder"></i>
          <span>Folder 2</span>
        </button>
        <ul x-h-tree.sub data-border="true">
          <li x-h-tree-item>
            <button x-h-tree-button>
              <i x-h-lucide role="img" data-lucide="file-text"></i>
              <span>File 2</span>
            </button>
          </li>
          <li x-h-tree-item>
            <button x-h-tree-button>
              <i x-h-lucide role="img" data-lucide="file-text"></i>
              <span>File 3</span>
            </button>
          </li>
        </ul>
      </li>
    </ul>
  </li>
  <li x-h-tree-item.expanded="true">
    <button x-h-tree-button data-indicator="negative">
      <i x-h-lucide role="img" data-lucide="folder"></i>
      <span>Folder 3</span>
    </button>
    <ul x-h-tree.sub data-border="true">
      <li x-h-tree-item>
        <button x-h-tree-button>
          <i x-h-lucide role="img" data-lucide="file-text"></i>
          <span>File 4</span>
        </button>
      </li>
      <li x-h-tree-item.expanded="true">
        <button x-h-tree-button>
          <i x-h-lucide role="img" data-lucide="folder"></i>
          <span>Folder 4</span>
        </button>
        <ul x-h-tree.sub data-border="true">
          <li x-h-tree-item>
            <button x-h-tree-button>
              <i x-h-lucide role="img" data-lucide="file-text"></i>
              <span>File 5</span>
            </button>
          </li>
          <li x-h-tree-item>
            <button x-h-tree-button>
              <i x-h-lucide role="img" data-lucide="file-text"></i>
              <span>File 6</span>
            </button>
          </li>
        </ul>
      </li>
    </ul>
  </li>
</ul>
```

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
