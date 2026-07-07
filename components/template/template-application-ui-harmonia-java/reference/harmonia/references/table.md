# Table

Organizes data into rows and columns, with each row representing a single item and each column representing a specific attribute. Tables provide a structured way to display complex or tabular information clearly.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-table-container` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

- `x-h-table-container`
- `x-h-table`
- `x-h-table-header`
- `x-h-table-head`
- `x-h-table-cell`
- `x-h-table-cell-button`
- `x-h-table-body`
- `x-h-table-row`
- `x-h-table-caption`
- `x-h-table-footer`

## API

### Attributes

#### x-h-table-container

| Attribute   | Type    | Required | Description                 |
| ----------- | ------- | -------- | --------------------------- |
| data-border | boolean | false    | Adds a border to the table. |

#### x-h-table

| Attribute    | Type                              | Required | Description                                        |
| ------------ | --------------------------------- | -------- | -------------------------------------------------- |
| data-borders | `rows`<br />`columns`<br />`both` | false    | Adds borders between rows, columns or both.        |
| data-fixed   | boolean                           | false    | Fixed table layout. Incompatible with scroll mode. |

#### x-h-table-header

| Attribute     | Type    | Required | Description                      |
| ------------- | ------- | -------- | -------------------------------- |
| data-bordered | boolean | false    | Adds a border around the header. |

#### x-h-table-row

| Attribute      | Type       | Required | Description                       |
| -------------- | ---------- | -------- | --------------------------------- |
| data-state     | `selected` | false    | Sets a selected state to the row. |
| data-hoverable | boolean    | false    | Makes the row hoverable.          |
| data-activable | boolean    | false    | Makes the row activable.          |

#### x-h-table-head

| Attribute      | Type    | Required | Description                      |
| -------------- | ------- | -------- | -------------------------------- |
| data-hoverable | boolean | false    | Makes the header cell hoverable. |
| data-activable | boolean | false    | Makes the header cell activable. |

#### x-h-table-cell

| Attribute      | Type    | Required | Description               |
| -------------- | ------- | -------- | ------------------------- |
| data-hoverable | boolean | false    | Makes the cell hoverable. |
| data-activable | boolean | false    | Makes the cell activable. |

### Modifiers

#### x-h-table-container

| Modifier | Description                           |
| -------- | ------------------------------------- |
| scroll   | Adds scroll ability to the container. |

## Binding

Binds through Alpine `x-model`. See the Example for the expected value shape.

## Example

```html
<div x-h-table-container data-border="true">
  <table x-h-table data-borders="both" data-fixed="true">
    <thead x-h-table-header>
      <tr x-h-table-row>
        <th x-h-table-head scope="col">Header 1</th>
        <th x-h-table-head scope="col">Header 2</th>
      </tr>
    </thead>
    <tbody x-h-table-body>
      <tr x-h-table-row>
        <td x-h-table-cell>Normal cell</td>
        <td x-h-table-cell>
          <button x-h-table-cell-button>Cell button</button>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

More examples in the docs site: Table with scroll, Table with caption, Table with row borders, Table with column borders, Table with no borders, Table with bordered header, Table with inner borders and no container, Table with inputs.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
