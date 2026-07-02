# Parsers - Document Template

A lightweight, declarative document-template engine for printable business documents —
Sales Invoice, Purchase Order, Quote, Delivery Note, Receipt, Statement, Credit Note.
Philosophically a widget tree (think Flutter / Jetpack Compose) targeted at paper, not
screens: templates are a tiny XML-like DSL with **only business-oriented tags** — no HTML,
no CSS, no scripting, no programming language of any kind inside templates.

The module covers the full template-side pipeline: **parsing → document model → data binding →
layout → XSL-FO**. The PDF bytes themselves come from the platform's FOP-based `PDFFacade`
(`engine-document` wires the two together).

```xml
<document>
    <header>
        <text align="right" style="title">Sales Invoice</text>
    </header>
    <section>
        <field label="Invoice No">{{invoice.number}}</field>
        <field label="Date">{{invoice.date}}</field>
    </section>
    <table source="invoice.items">
        <column width="45%">{{description}}</column>
        <column width="15%" align="center">{{quantity}}</column>
        <column width="20%" align="right">{{price}}</column>
        <column width="20%" align="right">{{amount}}</column>
    </table>
    <total align="right">{{invoice.total}}</total>
</document>
```

Mustache placeholders (`{{...}}`) are **not evaluated** by the parser — they survive verbatim
in text and attribute values; the `DataBinder` (below) is the layer that replaces them.

## Usage

```java
DocumentParser parser = new DocumentParser();
DocumentNode document = parser.parseDocument(source);   // requires a <document> root

// data binding: {{path}} substitution + table/for/if expansion against a Map context
Node bound = new DataBinder().bind(document, data);

// layout normalization + XSL-FO for the platform's FOP PDFFacade
String xslFo = new XslFoRenderer().renderBound(bound);
byte[] pdf = PDFFacade.generate(xslFo, "<data/>");      // PDFFacade lives in api-pdf
```

Or stop at any stage: `new LayoutEngine().layout(node)` gives the typed layout tree
(measurements + alignment, no geometry) for a custom `DocumentRenderer<T>`.

Every AST node carries its tag name, attributes (raw strings — validation happens later),
children in document order, normalized text content, and its source line/column. The
hierarchy is a sealed interface (`Node`) with one record per built-in tag, so consumers can
pattern-match exhaustively:

```java
switch (node) {
    case TableNode table -> layoutTable(table);
    case TextNode text -> layoutText(text);
    case CustomNode custom -> handleExtension(custom);
    // ... the compiler enforces the rest
}
```

## Built-in tags

`document`, `page`, `header`, `footer`, `section`, `row`, `column`, `stack`, `text`,
`field`, `table`, `image`, `line`, `space`, `total`, `if`, `for`

## Attributes

`id`, `width`, `height`, `flex`, `align`, `style`, `gap`, `padding`, `margin`, `label`,
`source`, `src`, `repeatHeader`, `pageBreak` — all stored as plain strings.

- **Alignment** (`align`): `left`, `center`, `right`, `justify`.
- **Widths/heights**: `100` (px), `100px`, `50%`, `*` (fraction weight 1), `2*`, `3*`;
  missing or `auto` sizes to content. `flex="2"` on a child of `row`/`column`/`stack` is
  shorthand for `width="2*"` (an explicit `width` wins).

## Extending with new tags

Unknown tags are a parse error — a typo like `<colum>` in a printable document must fail
fast rather than silently drop content. Extending the DSL is one line, no parser change:

```java
TagRegistry registry = TagRegistry.builtIn();
registry.register("qrcode");                       // parses to CustomNode
registry.register("barcode", BarcodeNode::new);    // or a CustomNode subclass
Node root = new DocumentParser(registry).parse(source);
```

## Grammar (EBNF)

```ebnf
template       = [ bom ] , [ prolog ] , { misc } , element , { misc } ;
prolog         = "<?xml" , { char } , "?>" ;
misc           = comment | whitespace ;
comment        = "<!--" , { char } , "-->" ;
element        = self-closing | container ;
self-closing   = "<" , name , { attribute } , [ S ] , "/>" ;
container      = open-tag , content , close-tag ;
open-tag       = "<" , name , { attribute } , [ S ] , ">" ;
close-tag      = "</" , name , [ S ] , ">" ;              (* name must match the open tag *)
content        = { text | element | comment } ;
attribute      = S , name , [ S ] , "=" , [ S ] , value ;
value          = '"' , { vchar - '"' } , '"'
               | "'" , { vchar - "'" } , "'" ;
vchar          = entity | char - "<" ;
text           = { entity | char - "<" }- ;
entity         = "&lt;" | "&gt;" | "&amp;" | "&quot;" | "&apos;" ;
name           = ( letter | "_" ) , { letter | digit | "-" | "_" | "." } ;
S              = whitespace ;

measurement    = number , [ "px" ] | number , "%" | [ number ] , "*" | "auto" ;
number         = digit , { digit } , [ "." , digit , { digit } ] ;
alignment      = "left" | "center" | "right" | "justify" ;   (* case-insensitive *)
```

### Deliberate leniencies vs strict XML

Templates are written by business users, so the parser is forgiving where strict XML is
hostile:

- A raw `&` that does not form one of the five named entities is **literal text** —
  `Fish & Chips` just works. Only `&lt; &gt; &amp; &quot; &apos;` are decoded; numeric
  character references (`&#65;`) are unsupported and stay literal.
- Comments are skipped anywhere, the optional XML prolog is skipped unparsed, attribute
  values may use single or double quotes.
- No CDATA, no DOCTYPE, no processing instructions, no namespaces.
- `{{` and `}}` have no lexical meaning — which is exactly why Mustache placeholders
  survive parsing untouched.

### Restrictions

- Tag names are case-sensitive lowercase.
- Duplicate attributes on one element are an error (determinism).
- A literal `<` in text must be written `&lt;`.
- Text whitespace is normalized: runs collapse to a single space, ends are trimmed.
  There is no preformatted-text escape hatch (yet).

Every syntax error reports its exact 1-based line and column; an unclosed element points
at its **opening** tag.

## Layout model

`LayoutEngine.layout(ast)` produces a `LayoutNode` tree with typed `Measurement`s
(`ABSOLUTE_PX` / `PERCENT` / `FRACTION` / `AUTO`) and `Alignment` per node, and resolves
table column widths (`LayoutEngine.resolveColumnWidths` + `fractionShares`). It computes
**no geometry** — coordinates, pagination, text measurement and `{{...}}` merging belong
to the data-binding layer and the concrete `DocumentRenderer` (e.g. the XSL-FO/PDF
backend).

## Data binding (`binding.DataBinder`)

The context is plain `Map<String, Object>` / `List<Object>` data (e.g. a JSON-decoded entity).
Paths walk nested maps (`customer.name`); a `table`/`for` node's `source` list expands into one
row per element, and inside a row a bare path (`quantity`) resolves against the row first, then
the enclosing document context; `if` keeps or drops its children by the truthiness of `source`.
Unresolved placeholders render as **empty strings** — a printout never shows raw braces.
**Floating-point values** (`Double`/`Float`/`BigDecimal`) print in the generated forms' money
pattern `### ### ### ##0.00` (space-grouped thousands, two decimals, locale-independent);
integral numbers print unformatted.

## XSL-FO rendering (`renderer.XslFoRenderer`)

`renderBound(boundAst)` emits a self-contained XSLT/XSL-FO stylesheet (all data already merged,
so it transforms any XML input, e.g. `<data/>`) — the exact input of the platform's FOP-based
`PDFFacade`. Tables get proportional/percent/px column widths and a bold header row when any
column carries a `label`; fields render as **Label:** value; `text` styles map `title`/`subtitle`/
`caption` to font presets. v1 limits (deliberate, documented in the class): header/footer render
once in-flow, `repeatHeader`/`pageBreak` are ignored, 1 px = 1 pt, and a table with no data rows
is skipped (FOP rejects an empty table body).

## Example templates

One per supported document type under
[`src/test/resources/templates/`](src/test/resources/templates/): `sales-invoice.print`,
`purchase-order.print`, `quote.print`, `delivery-note.print`, `receipt.print`,
`statement.print`, `credit-note.print`.
