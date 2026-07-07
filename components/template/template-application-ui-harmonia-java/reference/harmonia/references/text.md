# Text

Applies consistent typography styles to headings, paragraphs, single lines, and code blocks, ensuring a cohesive visual hierarchy and readability across the interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directive

- `x-h-text`

## API

### Modifiers

| Modifier    | Description                                                      |
| ----------- | ---------------------------------------------------------------- |
| h\{1-6\}    | Applies a heading style to the element. Sizes 1 to 6.            |
| blockquote  | Applies a quotation style to the element.                        |
| code-inline | Applies an inline code block style (single line) to the element. |
| code        | Applies a code block style (multiline) to the element.           |
| lead        | Applies a lead style to the element.                             |
| lg          | Large text                                                       |
| sm          | Small text                                                       |
| xs          | Extra small text                                                 |
| muted       | Applies a muted style to the element. Can be used on a label.    |

## Example

```html
<h1 x-h-text.h1>Heading 1</h1>
<h2 x-h-text.h2>Heading 2</h2>
<h3 x-h-text.h3>Heading 3</h3>
<h4 x-h-text.h4>Heading 4</h4>
<h5 x-h-text.h5>Heading 5</h5>
<h6 x-h-text.h6>Heading 6</h6>
<p x-h-text.blockquote>A quote from a comment</p>
<p x-h-text.code-inline>console.log('Hello, Harmonia!');</p>
<p
  x-h-text.code
  x-text="`function sayHello() {
  console.log('Hello, Harmonia!');
}`"
></p>
<p x-h-text.lead>Lead text</p>
<p x-h-text.lg>Large text</p>
<p x-h-text>Normal text</p>
<p x-h-text.sm>Small text</p>
<p x-h-text.xs>Extra small text</p>
<p x-h-text.muted>Muted text</p>
```

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
