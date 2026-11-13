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
`field`, `table`, `image`, `qrcode`, `line`, `space`, `total`, `if`, `for`

## Attributes

`id`, `width`, `height`, `flex`, `align`, `style`, `gap`, `padding`, `margin`, `label`,
`source`, `src`, `filter`, `match`, `groupBy`, `fontSize`, `value`, `size`, `level`, `repeatHeader`, `pageBreak` — all
stored as plain strings.

- **Alignment** (`align`): `left`, `center`, `right`, `justify`.
- **Widths/heights**: `100` (px), `100px`, `50%`, `*` (fraction weight 1), `2*`, `3*`;
  missing or `auto` sizes to content. `flex="2"` on a child of `row`/`column`/`stack` is
  shorthand for `width="2*"` (an explicit `width` wins).

## Extending with new tags

Unknown tags are a parse error — a typo like `<colum>` in a printable document must fail
fast rather than silently drop content. Extending the DSL is one line, no parser change:

```java
TagRegistry registry = TagRegistry.builtIn();
registry.register("stamp");                        // parses to CustomNode
registry.register("barcode", BarcodeNode::new);    // or a CustomNode subclass
Node root = new DocumentParser(registry).parse(source);
```

A tag whose content is line-oriented by nature registers as **line-preserving**
(`registry.registerLinePreserving("address", AddressNode::new)`): its text keeps its line breaks
instead of collapsing to one line - the rule the built-in `qrcode` uses, spelled out under
[QR codes](#qr-codes-qrcode-rendererqrcodeencoder).

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
- Text whitespace is normalized: runs collapse to a single space, ends are trimmed. The one
  exception is a line-preserving tag - the built-in `qrcode` - whose text keeps its lines (see
  [QR codes](#qr-codes-qrcode-rendererqrcodeencoder)). There is no preformatted *text* (yet).

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

**Alternative operands** — a placeholder may list several paths separated by `|`, and the first one
resolving to a **non-blank** value wins, left to right:

```xml
<field label="Customer">{{document.Customer.NameLocal|document.Customer.Name}}</field>
```

"Blank" is null, missing, or whitespace-only; every operand obeys the same `document.` / `items.`
path and row-scope rules as a single path; and the **last** operand is rendered whatever it holds,
so all-blank renders empty exactly as a lone unresolved path does (which is what keeps every
existing template byte-identical). That is the whole grammar addition — no literals, no
expressions. It exists because an optional twin field is the normal shape of business data: reading
a locally registered name beside the canonical one must not leave a hole in a legal document when
only one of the two is filled.

**Amount in words** — the format `words(<currency>)` spells a money amount out in the template's
language, the one of the `Print/<lang>/` folder it was read from (`new DataBinder(language)`):

```xml
<text>Словом: {{document.Total:words(document.Currency.Code)}}</text>
<!-- ПЕТ ХИЛЯДИ ДВЕСТА ШЕСТДЕСЕТ И ЧЕТИРИ ЕВРО И 44 ЕВРОЦЕНТА -->
```

The argument is a path to the ISO 4217 currency code, or the code itself (`words(EUR)`) for a
template of one currency. The whole amount is in words, the cents are two digits, both currency
nouns are spelled out, in capitals. Two languages so far:

- **Bulgarian** (`bg`) names the euro and the lev, with the grammar a Bulgarian invoice needs:
  gender agreement (едно евро / един лев / две хиляди) and `и` before the last word of each
  three-digit group.
- **English** (`en`) names the euro, the lev, the US dollar and the pound sterling -
  `FIVE THOUSAND TWO HUNDRED SIXTY-FOUR EUROS AND 44 CENTS`: hyphenated tens (sixty-four), short
  scales, and no `and` inside the number, so the only one joins the cents.

The placeholder renders
**empty**, never throws, in a language with no spelling, for a currency the language cannot name, for
a non-numeric value and at a trillion or more. It combines with alternative operands like any other
format.

**Row filtering** stays declarative — a value match, not an expression language: a `table`/`for`
with `filter="kind"` keeps only the elements whose `kind` resolves truthy (in the row's scope),
and adding `match="CONTRIBUTION | TAX"` narrows that to the listed `|`-separated literals. The
same `match` on an `if` compares the resolved `source` against the listed values instead of
testing truthiness. One fed collection can this way render into several purpose-grouped tables —
a payslip's earnings vs deductions, a journal entry's debit vs credit side — without pre-splitting
the data feed:

```xml
<row gap="12">
    <stack>
        <text style="subtitle">Earnings</text>
        <table source="items" filter="kind" match="BASE | ENTRY">
            <column width="3*">{{name}}</column>
            <column width="*" align="right">{{amount}}</column>
        </table>
    </stack>
    <stack>
        <text style="subtitle">Deductions</text>
        <table source="items" filter="kind" match="CONTRIBUTION | TAX">
            <column width="3*">{{name}}</column>
            <column width="*" align="right">{{amount}}</column>
        </table>
    </stack>
</row>
```
**Row grouping** - `groupBy="<path>"` on a `table`/`for` expands one row (or one pass of the
loop) per distinct value of the path over the rows the filter kept, ordered by that value
(numerically when every value is a number, else by its text; rows without it come last, and
numbers group by value, so `20` and `20.0` are one group). Inside a group a plain placeholder
reads the group's FIRST row, so the key prints as itself, and two aggregates read all of its rows:
`{{sum(<path>)}}` adds the path's numbers exactly (`BigDecimal`, skipping rows where it is missing
or not a number) and `{{count()}}` counts the rows. The grouping key and the summed path are read
from each row itself - a row lacking the field never picks it up from the enclosing context. Both take formats and alternatives like any
other operand; outside a group they render empty. The totals block of an invoice with lines at
several VAT rates - a legal requirement wherever rates are mixed - needs nothing on the model:

```xml
<table source="items" groupBy="VatRate">
    <column label="Rate">{{VatRate:0.##}}%</column>
    <column label="Tax base" align="right">{{sum(Net)}}</column>
    <column label="VAT" align="right">{{sum(Vat)}}</column>
</table>
<!-- 9%  | 22.80 | 2.05
     20% | 25.00 | 5.00 -->
```

The VAT of each rate is the sum of its lines' VAT, so per-line rounding carries through to the
cent.

**Row ordinal** - inside a `table`/`for` the reserved `{{__index}}` is the row's 1-based position
among the rows **actually rendered**: it counts after the row filter, so a filtered table numbers
the rows it kept 1, 2, 3 with no gaps, and with `groupBy` it is the ordinal of the group. The "№"
column every invoice items table starts with is one cell:

```xml
<table source="items">
    <column width="24" label="№">{{__index}}</column>
    <column width="4*" label="Item">{{Name}}</column>
    <column width="*" label="Amount" align="right">{{Amount}}</column>
</table>
<!-- 1 | Widget | 100.00
     2 | Gadget |  23.45 -->
```

It is a number like any other, so it takes a format (`{{__index:00}}`) and composes with text
(`{{__index}}.`). Where loops nest it reads the innermost row, and each `table`/`for` counts from 1
again - also once per pass of an enclosing loop. Outside a row it renders empty. The name is
reserved, double underscore like `__label`: a row field called `__index` is not reachable.

**Floating-point values** (`Double`/`Float`/`BigDecimal`) print in the generated forms' money
pattern `### ### ### ##0.00` (space-grouped thousands, two decimals, locale-independent);
integral numbers print unformatted.

## XSL-FO rendering (`renderer.XslFoRenderer`)

`renderBound(boundAst)` emits a self-contained XSLT/XSL-FO stylesheet (all data already merged,
so it transforms any XML input, e.g. `<data/>`) — the exact input of the platform's FOP-based
`PDFFacade`. Tables get proportional/percent/px column widths and a bold header row when any
column carries a `label`; a table's font is the 10pt body size when its columns' content fits them,
else the largest size at which it does, never below 8pt (a figure has to fit whole, other text and
the bold labels word by word; inside a `row`, where the table's width is not known, a point smaller
per column beyond three), unless the table sets `fontSize` (`<table source="items" fontSize="9">`,
points; `9pt`/`9px` accepted, anything but a positive number is a layout error);
fields render as **Label:** value; `text` styles map `title`/`subtitle`/
`caption` to font presets. v1 limits (deliberate, documented in the class): header/footer render
once in-flow, `repeatHeader`/`pageBreak` are ignored, 1 px = 1 pt, and a table with no data rows
is skipped (FOP rejects an empty table body).

## Images (`renderer.ImageResolver`)

`<image src="..." width="120" height="60"/>` renders an `fo:external-graphic`; `width` and
`height` are resize hints, each emitted only when it is an absolute measurement, so a single
one scales proportionally.

The library reads no storage, so it cannot fetch a source itself — and it must not, because
the rendered stylesheet is self-contained and carries no session, credentials or tenant scope
to fetch anything later. The host therefore supplies an `ImageResolver`
(`new XslFoRenderer(resolver)`), which turns a bound `src` into the source to emit — in
practice a `data:` URI carrying the bytes inline. The default, `ImageResolver.PASS_THROUGH`,
emits every source as authored.

A resolver returning `null` renders **nothing at all** — no block, no placeholder: a printed
business document whose logo is missing is correct output, a broken-image box is not. An
absent or blank `src` never reaches the resolver.

FOP reads a `data:` URI natively (`InternalResourceResolver`), so an inlined image needs no
configuration; `PDFFacadeTest.generatePdfWithInlineImageTest` pins that contract.

## QR codes (`<qrcode>`, `renderer.QrCodeEncoder`)

`<qrcode>` prints a QR code of a text payload - the payment code of an invoice (EPC069-12, "SEPA
credit transfer": scanned in a banking app, the IBAN, amount and remittance text are prefilled), a
link to the document, a verification code:

```xml
<if source="document.BankAccount.Iban">
    <qrcode size="90" level="M" align="right">
        BCD
        002
        1
        SCT
        {{document.BankAccount.Bic}}
        {{document.Company.LegalName}}
        {{document.BankAccount.Iban}}
        EUR{{document.Balance:0.00}}


        Invoice {{document.Number}}
    </qrcode>
</if>

<qrcode value="https://example.com/invoices/{{document.Number}}"/>
```

| attribute | meaning |
| --- | --- |
| `value` | the payload on one line - the short form; when it is not blank it wins over the text |
| `size` | the side of the printed square in points, quiet zone included (`90`, `90pt`, `90px`); default `90` (about 32 mm). Anything but a positive number is a layout error |
| `level` | error correction: `L` (7%), `M` (15%, the default and what EPC069-12 prescribes), `Q` (25%), `H` (30%), case-insensitive. Anything else is a layout error |
| `align` | `left` / `center` / `right` within the space the element has |
| `width` | the element's column inside a `row`, as for any other row child - `size` is the symbol, not the column |

**The payload is the element's text with its line breaks kept** - placeholders, alternative
operands and format specifiers resolve exactly as in `<text>`. Line-oriented payloads count their
lines (the remittance text of a payment code is line 11, whatever lines 9 and 10 hold), so the text
is normalized only as far as the template's own formatting goes:

- line endings (`\r\n`, `\r`) become a line feed;
- every line loses its leading and trailing spaces and tabs - the indentation of the template;
  spaces inside a line stay;
- the first line is dropped when it is blank, and so is the last one: the line break after
  `<qrcode ...>` and the indentation before `</qrcode>`. Exactly one at each end - a second blank
  line there is yours and is kept;
- every other line stays, **the empty ones included**.

This happens when the template is parsed, before any data is merged, so a line that holds only a
placeholder is a line whatever the placeholder resolves to - `{{document.Missing}}` leaves an empty
line, it does not swallow one. A resolved value is inserted as it is: one that itself carries a line
break adds a line. The five entities are decoded as everywhere else, so a `<` in a payload is
`&lt;`.

**Fail-soft, like `<image>`.** A payload that is blank after binding - a `value` whose placeholders
resolved empty, an element with no text - renders **nothing**: no block, no placeholder. So does a
payload too long for a QR code at the requested level. A payment code is never blank as a whole
(its fixed lines are always there), so the "no bank account / not in EUR / nothing left to pay"
cases are an `<if>` around it, as above. It prints wherever an `<image>` does - in the flow, in a
`row`, a `stack`, the `header` or `footer` - but not inside a `table` cell, which is text.

**The encoding is the host's.** The library carries no dependency, so it models the element,
keeps the lines and lays the square out, and hands the payload to a `QrCodeEncoder`
(`new XslFoRenderer(imageResolver, qrCodeEncoder)`), which returns the image to embed - a `data:`
URI - or `null` for nothing. The default, `QrCodeEncoder.NONE`, renders no QR code;
`engine-document` supplies the real one (`PrintQrCodeEncoder`, UTF-8, so Cyrillic names scan back).

## Example templates

One per supported document type under
[`src/test/resources/templates/`](src/test/resources/templates/): `sales-invoice.print`,
`purchase-order.print`, `quote.print`, `delivery-note.print`, `receipt.print`,
`statement.print`, `credit-note.print`.
