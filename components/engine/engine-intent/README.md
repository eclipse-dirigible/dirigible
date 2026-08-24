# Intent DSL - supported constructs

One `app.intent` YAML file per project is the source of truth one altitude above the model files.
The Intent Editor (double-click any `*.intent`) parses it live and its **Generate** button writes
the model files (`.edm`/`.model`, `.bpmn`, `.form`, `.report`, `.roles`, `.csvim`, `.glue`,
`.print`, `.test`) into the workspace project; model-to-code then produces the runnable app (Java
DAO/REST + Harmonia UI). The intent is an **authoring artifact** - no synchronizer, nothing touches
the registry until normal publish.

This README is the quick index: one line + one snippet per construct. The authoritative reference
(rules, edge cases, validation messages) is
[`src/main/resources/intent-assistant-guide.md`](src/main/resources/intent-assistant-guide.md);
contributor internals live in [`CLAUDE.md`](CLAUDE.md). Enforcement- and behaviour-bearing constructs
below are covered end to end (generated token + runtime behavior where the construct has a server-side
effect) by `IntentEmissionCoverageIT`; presentation-only constructs (calendar views, `where` option
filters) are covered at the generation layer by `IntentEngineIT` and the parser / EDM unit tests.

## Index

| Construct | What it gives you |
|---|---|
| [`entities`](#entities) | tables + CRUD UI + generated Java repository/REST |
| [field/relation attributes](#field--relation-attributes) | uniqueness, layout, read-only, dropdown filtering, cascades |
| [`function`](#function--presentation-role) | explicit presentation role (Document, Setting, ...) |
| [`checks`](#checks--declarative-validations) | cross-field / cross-line validations |
| [`immutableWhen` / `immutable`](#immutablewhen--immutable---user-write-immutability) | 409 on user writes in a status / append-only snapshots |
| [`period` / `immutableInPeriod`](#period--immutableinperiod---date-based-immutability) | 409 on user writes to a record dated in a closed fiscal period |
| [`lifecycle`](#lifecycle---the-legal-status-graph) | the whole legal status graph, enforced on every status write |
| [`hierarchy` / `leafOnly`](#hierarchy--leafonly--tree-entities) | tree entities, leaf-only references |
| [`multilingual` / `languages`](#multilingual--translated-master-data) | `_LANG` tables + read-time translation overlay |
| [calculated fields](#calculated-fields--actions) | server+UI-evaluated expressions, date functions, Java call-outs |
| [`view`](#view--calendar-range-slots) | calendar / range / slot-booking pages |
| [`uses`](#uses--cross-model-references) | reuse entities owned by another intent model |
| [`processes`](#processes--workflows) | BPM workflows with user tasks, decisions, delegates |
| [`forms`](#forms--task-ui) | task data-entry pages |
| [`actions`](#actions--custom-buttons) | developer-defined buttons opening custom pages |
| [`generates`](#generates--create-from) | one-click document-from-document cloning |
| [`postings`](#postings--source-document-to-ledger) | declarative source-document -> balanced document posting |
| [`expansions`](#expansions--child-rows-from-a-date-span) | generated child rows per day/week/month |
| [`rollups`](#rollups--denormalised-parent-totals) | counts, sums, balance + status maintenance, transitive chains |
| [`settlements`](#settlements--payment-allocation) | auto-allocation of payments across open invoices |
| [`reports`](#reports--read-only-aggregations) | aggregations, charts, dashboard KPI tiles, balance reports |
| [`widgets`](#widgets--custom-dashboard-tiles) | custom KPI / embedded-page dashboard tiles |
| [`seeds`](#seeds--initial-data) | initial data, CSV-backed sets, translations |
| [`notifications`](#notifications--email-on-change) | email on create/update/delete |
| [`schedules`](#schedules--cron) | cron: notify or generate records per matching row |
| [`integrations`](#integrations--outbound-http) | outbound HTTP on a data change |
| [`inbound`](#inbound--webhooks-queues-and-drop-folders) | an arrival that creates records, optionally gated and mapped |
| [`outbound`](#outbound--emit-on-a-queue-or-topic) | emit a record on a queue or topic on an event |
| [`permissions`](#permissions--roles) | roles |
| [Planned](#planned--recognised-but-not-yet-implemented) | recognised, not yet implemented |

## entities

The data model - every entity becomes a table, a generated Java repository + REST controller, and a
Harmonia page. Integer PKs only; composition is opt-in.

```yaml
entities:
  - name: Member
    icon: user
    audit: true                # adds CreatedAt/CreatedBy/UpdatedAt/UpdatedBy
    group: master-data         # nav group in the shared application shell
    fields:
      - { name: id,   type: integer, primaryKey: true, generated: true }
      - { name: name, type: string,  required: true, length: 200 }
    relations:
      - { name: loans, kind: oneToMany, to: Loan }
  - name: Loan
    fields:
      - { name: id,    type: integer, primaryKey: true, generated: true }
      - { name: dueOn, type: date }
    relations:
      - { name: member, kind: manyToOne, to: Member, composition: true }  # detail of Member
```

## Field / relation attributes

```yaml
- { name: code,  type: string, unique: true, length: 30 }              # UNIQUE constraint
- { name: uuid,  type: uuid, major: false }                            # off the list table
- { name: total, type: decimal, precision: 18, scale: 2, readOnly: true }
- { name: number, type: string, function: DocumentTitle }              # the document title/number
- { name: Currency, kind: manyToOne, to: Currency, size: 4 }           # form width (12-col grid)
- { name: Payment, kind: manyToOne, to: Payment, show: [date, number] }  # extra read-only lookup columns
- { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }  # managed badge, seeded default
# Depends-On - cascade, narrow-to-referenced, auto-populate:
- { name: City,  kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: Country } }
- { name: UoM,   kind: manyToOne, to: UoM,  dependsOn: { relation: Product, valueFrom: UoM } }
- { name: price, type: decimal,             dependsOn: { relation: Product, valueFrom: price } }
# Static option filter - e.g. only stock-tracked products:
- { name: Product, kind: manyToOne, to: Product, where: { Type: 1 } }
```

Entity-level extras: `order: [Id, Product, Quantity, ...]` sequences form controls/list columns;
`duplicable: true` adds a Duplicate button on a document (clones header + items through the normal
create path); `imports: |` injects Java import lines into the generated repository (pairs with
calculated actions); `aggregate: true` on a document master's numeric field keeps it equal to the
sum of the items' same-named field (the totals footer).

## function - presentation role

Optional and authoritative when set; inferred from structure otherwise.

```yaml
- name: ProjectTimesheet
  function: Document           # header + line items + status pill + totals
- name: EmployeeTimesheet
  function: DocumentItem       # its line items (no "*Item" naming needed)
```

Values: `Document`, `DocumentItem`, `Master`, `Detail`, `List`, `Setting` (entity);
`DocumentTitle` (field); `EntityStatus` (relation).

## checks - declarative validations

Row-level `exactlyOne` on every user write; document-level `itemsMin` / `itemsSumEqual` gated on a
status transition (drafting stays unconstrained; the failing transition aborts with the authored
message).

```yaml
- name: JournalEntry
  checks:
    - { kind: itemsMin,      count: 1, status: 2, message: "An entry needs at least one line" }
    - { kind: itemsSumEqual, over: [debit, credit], status: 2, message: "Debits must equal credits" }
- name: JournalEntryItem
  checks:
    - { kind: exactlyOne, fields: [debit, credit], message: "Exactly one of debit/credit" }
```

## immutableWhen / immutable - user-write immutability

```yaml
- name: JournalEntry
  immutableWhen: "Status == 2"   # while Status holds seed id 2 (POSTED), REST update/delete return 409
# or, unconditionally append-only from creation:
- name: SentSnapshot
  immutable: true                # every record is read-only to user writes the moment it is created
```

`immutableWhen` requires a `function: EntityStatus` relation and takes a boolean expression over its
seed ids (terms joined with `||`). `immutable: true` is the unconditional append-only variant, mutually
exclusive with `immutableWhen`. Workflow/system writes through the repository stay possible -
corrections are flow-generated reversals, never edits.

## period / immutableInPeriod - date-based immutability

```yaml
- name: AccountingPeriod
  period:
    start: startDate
    end: endDate                       # inclusive
    closedWhen: "Status == CLOSED"     # the immutableWhen grammar; seeded names or ids
  fields:
    - { name: id,        type: integer, primaryKey: true, generated: true }
    - { name: startDate, type: date }
    - { name: endDate,   type: date }
  relations:
    - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: OPEN }

- name: JournalEntry
  immutableInPeriod: { period: AccountingPeriod, date: entryDate }
```

`immutableWhen` freezes a record for what it IS; this freezes it for WHEN it falls. Once the period
covering the named date is closed, that record can no longer be created, edited or deleted through
the REST surface (409) - including a CREATE dated inside the closed window and an update that would
MOVE a record into one. Workflow / system writes through the repository stay possible, exactly as for
the status guard: a correction to a closed period is a reversal booked in an open one.

The register is an ordinary entity, so closing a period is a plain status transition (a
`transitions:` button, a `lifecycle:` edge, a workflow step) - `period:` only names the two bounds
and what CLOSED means, once, where the period lives.

A date covered by **no** period is open (periods are opened as they are needed, and an undeclared
month must not freeze what is booked into it), and a record whose date is unset falls in none. The
lock reaches a composition CHILD of a guarded master, as the status lock does, since a line write
recomputes the document's totals. The register must be an entity of the SAME model - the guard is
generated into this model's controllers, which can only query a repository generated alongside them.

## lifecycle - the legal status graph

```yaml
- name: SalesInvoice
  lifecycle:
    edges:
      - { from: DRAFT,  to: [ISSUED, CANCELLED] }
      - { from: ISSUED, to: [PAID, VOIDED] }
  relations:
    - { name: status, kind: manyToOne, to: SalesInvoiceStatus, function: EntityStatus, init: DRAFT }
```

The whole set of legal status moves, declared once. One entry per SOURCE status; either side accepts
a seeded status name or its id. The graph is always over the entity's `function: EntityStatus`
relation (so it names no column), and the nomenclature must be seeded in this model.

Enforced in the generated **repository** - the one choke point every status write passes through - so
an unmodeled move is rejected with 400 wherever it came from: the REST update, the transition
controller's targeted write, a workflow `setRelationField`, a hand-written action. With `init:`
declared, a record must also be created in that status. At authoring time the graph is what
`transitions:`, a status-setting workflow step and a check's rejection are validated against, so the
buttons and the graph cannot disagree.

## hierarchy / leafOnly - tree entities

```yaml
- name: Account
  hierarchy: Parent                                        # the tree edge (self-relation)
  relations:
    - { name: Parent, kind: manyToOne, to: Account }
# elsewhere - only leaf accounts are referenceable (server-enforced):
- { name: Account, kind: manyToOne, to: Account, model: accounts, leafOnly: true }
```

The list renders as an expandable tree; the server rejects cycles and leaf-only references to a
node with children.

## label - the stored display name

```yaml
- name: SalesInvoice
  label: "{number} - {date|yyyy MMMM} - {Customer.name}"
```

Generates a stored, read-only `Name` property recomputed by the repository on **every write path**
(save / update / workflow writes), so lookups, dropdowns and lists show a meaningful display name
everywhere - dropdown label resolution prefers `Name` automatically, including cross-model. Tokens
are own fields or ONE-hop to-one relation properties, `|format` applies a date pattern to temporal
values. Compose across hops by referencing the related entity's own generated label
(`{ProjectTimesheet.Name}` - e.g. "2026 July - My Project - Ivan Georgiev"). Rejected when the
entity already declares a `name` field or a token references a sensitive field.

## identity / personal / sensitive - the personal (my) surface

```yaml
- name: Employee
  identity: email              # this entity maps the logged-in user (matched by this field)
# elsewhere - the owner relation of a personal record:
- name: VacationRequest
  fields:
    - { name: dailyRate, type: decimal, sensitive: true }  # never on the personal surface
  relations:
    - { name: Employee, kind: manyToOne, to: Employee, model: employees, personal: true }
```

A `personal` owner relation makes the entity get an ADDITIONAL generated REST controller
(`<Entity>MyController`) scoped to the logged-in user: reads are filtered to the user's mapped
identity record, writes force the owner FK server-side, foreign records are a 404, and
`sensitive` fields are stripped from responses and ignored on writes. Composition children
inherit the scope through their DIRECT parent (ancestor-ownership guard). The regular (power)
controller is unaffected. One `personal` relation per entity; the target must declare
`identity` (a string field, conventionally the unique e-mail/username). No identity row mapped
to the user = an empty personal surface, never an error.

## multilingual - translated master data

```yaml
languages: [en, bg]            # the languages this module PROVIDES translations for
entities:
  - name: UoM
    kind: setting
    multilingual: true         # sibling <TABLE>_LANG table; reads overlay per Accept-Language
```

Translations are seeds with a `language:` code (below). The platform's supported set is
`DIRIGIBLE_APPLICATION_LANGUAGES`.

## Calculated fields / actions

Neutral arithmetic expressions run on the server AND preview live in the UI; date functions
included. For logic beyond an expression, a hand-written `CalculatedField` component is called out.

```yaml
- { name: net, type: decimal, calculatedOnCreate: "Quantity * Price", calculatedOnUpdate: "Quantity * Price" }
- { name: days, type: decimal, readOnly: true,
    calculatedOnCreate: "businessDaysBetween(FromDate, ToDate)" }     # also daysBetween, monthsBetween
- { name: number, type: string, calculatedActionOnCreate: SalesInvoiceNumberAction }  # + entity imports:
```

## view - calendar, range, slots

```yaml
- name: EmployeeDayAllocation
  view: calendar                                   # month/week calendar of records
  calendar: { start: day, title: note }            # start (date/timestamp) required; end/title/color optional
- name: VacationRequest
  view: range                                      # from-to bars (leave calendar)
  calendar: { start: fromDate, end: toDate }
- name: Appointment
  view: slots                                      # slot-picker booking page
  slots: { start: startTime }
```

Every `view:` on a **top-level entity** adds a page **alongside** that entity's own layout - it never
replaces it. The view becomes the entity's landing browse page (`/<Entity>`), the layout's own browse
page moves to `/<Entity>/list`, and both carry a switch to the other; create / edit / preview stay the
layout's own, so a **document master browsed on a calendar (or booked on a slot picker) still edits on
its document page** (line items, Print, inline process tasks included). Same on the personal surface:
`/my/<Entity>` is the calendar, `/my/<Entity>/list` the list.

`view: calendar` on a **composition child** renders an **embedded calendar panel inside its master's
page** (and the master's edit form) instead of the detail table: the same master-filtered rows become
events, event-click edits the child, an empty-day click creates one with the master FK and the
clicked date preset. The child keeps everything a detail has (registry, filtered controller, form
pages) - the calendar is just how its panel renders. When that child is the document's **line-items**
entity, the document's items **pane** is the calendar (see below); `range` works the same way.

`view: slots` works the same way: the picker is how a booking is CREATED (pick a free slot → the
layout's create page, prefilled with the chosen datetime), and the list / document page is how it is
worked with afterwards - an author needs both, so the picker is additional too.

## generate children - collection-driven scheduled generation

```yaml
schedules:
  - name: monthly-project-timesheets
    cron: "0 0 2 1 * *"
    entity: Project
    generate:
      to: ProjectTimesheet
      children:
        - to: EmployeeTimesheet
          parent: ProjectTimesheet
          forEach: { entity: EmployeeProjectAssignment, match: { Project: id } }
          map: { Employee: Employee }
          children:
            - to: EmployeeDayAllocation
              parent: EmployeeTimesheet
              forEach: { days: workingDays }
              dayField: day
              defaults: { hours: 8 }
```

A scheduled generation may create CHILD rows under each generated record: one per matching row of
a LOCAL collection entity (`forEach: {entity, match}`) or one per working day (Mon-Fri) of the
month the job runs in (`forEach: {days: workingDays}` + `dayField`). `parent:` names the child's
to-one back to the generated record; children nest one more level (depth two). Numeric literal
defaults render as decimals.

## uses - cross-model references

Entities owned by another intent model are referenced read-only (a PROJECTION + FK + dropdown - no
local table/DAO). Generate leaf-first so the owner's model exists.

```yaml
uses:
  - { model: countries }
entities:
  - name: Supplier
    relations:
      - { name: Country, kind: manyToOne, to: Country, model: countries }
```

## manyToMany - the intermediate entity, materialized

An n:m is always an intermediate (link) entity - one row per link. `kind: manyToMany` writes that
entity for you:

```yaml
entities:
  - name: Order
    relations:
      - { name: products, kind: manyToMany, to: Product }        # through: OrderLine to name it
```

materializes, before validation and generation:

```yaml
  - name: OrderProduct                       # <Declaring><Target>, or the authored `through:`
    fields:
      - { name: id, type: integer, primaryKey: true, generated: true }
    relations:
      - { name: Order,   kind: manyToOne, to: Order, composition: true, required: true }
      - { name: Product, kind: manyToOne, to: Product, required: true }
```

so the link gets a real table, a detail grid under the declaring entity's page (dropdown for the
target), and can be seeded, reported on and referenced like any other entity. The target may be
cross-model (`model:`); the target-picker attributes (`where` / `show` / `major` / `size` /
`leafOnly`) travel onto the link's target relation.

**Author the intermediate entity yourself** (composition to one side + `manyToOne` to the other,
exactly as above) when the link carries **bridge fields** - a quantity, a partial `amount`, a
valid-from date - or a lifecycle of its own; then drop the `manyToMany`. Declare an n:m on **one**
side only, and note that a relation attribute describing a hand-authored to-one (`composition`,
`function`, `init`, `dependsOn`, a calculated action, `personal`, `partner`) is rejected on a
`manyToMany` rather than silently dropped.

## processes - workflows

```yaml
processes:
  - name: OrderApproval
    trigger: { onCreate: Order }
    steps:
      - { name: review,   kind: userTask, args: { assignee: manager, form: ApproveOrder } }
      - { name: decide,   kind: decision, args: { if: "action == 'approve'", then: activate, else: cancel } }
      - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: end } }
      - name: number
        kind: serviceTask
        args: { delegate: custom.orders.NumberDelegate, fields: { type: "Order" }, next: end }
      - { name: cancel,   kind: serviceTask, args: { setRelationField: Status, value: 3, next: end } }
      - { name: end,      kind: end }
```

Service-task shapes: `setField` / `setRelationField` (generated handlers), `call` (TS handler,
deprecated), `delegate` (a reusable hand-written client `JavaDelegate` with injected `fields`).
Decisions may test `relation.field` paths (`customer.creditLimit > 10000`) - resolvers are
generated. Tasks surface in the Inbox and inline on the record's page.

## forms - task UI

```yaml
forms:
  - name: ApproveOrder
    forEntity: Order
    fields: [orderDate, total, customer.name]     # fields or one-hop relation.field
    actions: [approve, reject]                    # complete the BPM task
```

## actions - custom buttons

```yaml
actions:
  - name: OpenPortal
    forEntity: Order
    scope: entity            # per-record; 'page' = whole-view toolbar
    page: /services/web/myapp/custom/portal.html
```

## generates - create-from

```yaml
generates:
  - name: invoice-from-timesheet
    from: ProjectTimesheet
    to: SalesInvoice
    uses: sales                       # model alias when the target is cross-model
    map: { Customer: Customer }
    defaults: { InvoiceDate: now }
    items: { from: ProjectTimesheetItem, to: SalesInvoiceItem, map: { Description: Description } }
    sourceStatus: 3                   # optional completion hook: the SOURCE's EntityStatus after creation
    sourceStatusOnRetire: 2           # optional INVERSE: where the SOURCE returns when the target is retired
```

`items:` has two mutually-exclusive shapes. As an OBJECT (above) it MIRRORS each source child row
1:1. As a LIST (below, #6555) it builds COMPUTED synthetic lines whose cells are expressions over the
SOURCE record - the target's line-items child is resolved automatically:

```yaml
    items:                            # computed synthetic lines over the SOURCE record
      - name: "Services for {period}"   # string: {field} interpolation (or a source-field copy / literal)
        quantity: 1                     # numeric: a Calc arithmetic expression (PascalCase source idents)
        price: BillableAmount           #   rounded to the target field's scale (a literal is trivial)
        when: "BillableAmount != 0"     # optional guard: `<SourceField> ==|!= <number>` (Calc, null-safe)
```

A numeric cell is `Calc.eval(...)` (like posting item amounts); a to-one relation cell copies the raw
source FK (#6533 parity); a string cell interpolates / copies / literals.

Adds a button on the source view; the clone saves through the target's repository so numbering,
status init and calculated fields fire. `sourceStatus:` flips the SOURCE to the given EntityStatus
seed id once the target exists (proforma -> INVOICED) - a system write: no `-updated` re-fire, but
the source's `-transitioned` topic is published.

`event: { onTransition: <Source>, when: "Status == <status>" }` (or `onCreate`, or a process step)
mints the target with nobody clicking; the `map:` entry copying the source's key is then the
**at-most-once guard**, and a target retired into a `cancelled`/`void` `stage:` stops blocking, so the
source may be generated from again. `sourceStatusOnRetire:` is the INVERSE of the completion hook and
what makes that reissue automatic: retiring the target returns the source to the named status - one
targeted write carrying the source's `-transitioned` - so the ordinary trigger re-fires and mints the
replacement. It fires only while the source still stands at `sourceStatus` and no target of it still
counts, so a redelivered retirement is a no-op. Without it, a source flipped by `sourceStatus:` can never re-qualify and only a shared
`button: true` can reissue. It needs an `event:` to re-fire, `sourceStatus:` (a different
status), a local target whose nomenclature classifies a retiring stage, `mode: once`, and - when the
source declares a `lifecycle:` - the edge back.

`prompt:` (#6685) declares a small input form shown before the target is created - the values the
source cannot derive (which payment, how much). Entries name fields / to-one relations of the
TARGET, so the dialog's controls are typed from the target's own definitions and its `dependsOn:`
cascades apply unchanged; required inputs are enforced in the dialog and again by the generated
controller (400). This is what makes a post-issue child (a manual payment allocation) reachable on
an **immutable** document - the panel is read-only by design, the action button is not:

```yaml
generates:
  - name: allocate-payment
    from: SalesInvoice
    to: SalesInvoiceCustomerPayment   # must be a composition child of forEntity (local, scope entity)
    map: { SalesInvoice: id, Customer: Customer }
    prompt:
      - { field: CustomerPayment, required: true }
      - { field: amount, required: true }
```

A prompted property may not also be mapped/defaulted (one writer); prompted values are set on the
target after `map`/`defaults`, and the create still goes through the target's repository, so the
ordinary `-created` event and rollups fire unchanged.

## postings - source-document to ledger

When a (usually cross-model) source document reaches a status, create ONE local document with
computed multi-line content. Idempotent via the back-reference; a missing rule/account skips (the
unposted worklist), never throws.

```yaml
postings:
  - name: salesInvoicePosting
    event: { onTransition: SalesInvoice, model: sales-invoices, when: "Status == 3" }
    creates: JournalEntry
    backReference: SalesInvoice
    map: { entryDate: date, reason: "Sales invoice {number}" }
    rule: { entity: PostingRule, match: { documentType: "Sales Invoice" } }
    items:
      - { Account: rule(receivableAccount), debit: "Net + Vat" }
      - { Account: rule(revenueAccount),    credit: "Net" }
      - { Account: rule(vatAccount),        credit: "Vat", when: "Vat != 0" }
```

## expansions - child rows from a date span

```yaml
expansions:
  - name: installments
    from: Loan
    into: LoanInstallment
    unit: month                                     # day (default) | week | month
    between: { start: startDate, end: endDate }
    map: { dueDate: period }
    spread: { total: principal, into: amount, round: 2 }   # last row absorbs the remainder
    count: periods
```

A span change is reconciled as a diff - the missing periods are inserted, the ones that fell out of
the span are deleted, every other row is left alone; never mix hand-entered rows into an expanded
child.

## rollups - denormalised parent totals

```yaml
rollups:
  - { name: memberLoanCount, entity: Loan, via: member, field: loanCount }        # count
  - { name: invoicePaid, entity: Allocation, via: SalesInvoice, field: paid,      # sum + balance + status
      op: sum, of: amount, capacity: total, balance: balance,
      status: Status, statusWhenFull: 7, statusWhenPartial: 6 }
```

Roll-ups compose transitively across a multi-level composition (leaf edit -> mid total -> top
total); recomputation stops when values stop changing.

Either end may be owned by another model. A cross-model PARENT is named by the `via` relation's own
`model:` alias (the child is local and owns the event). A cross-model CHILD is named by the roll-up's
`model:` plus a `parent:` naming the local entity the total lands on - the n:m allocation direction,
where the link rows live with one side of the pairing and the other side's total belongs here:

```yaml
rollups:
  - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, model: sales-invoices,
      parent: CustomerPayment, via: CustomerPayment, field: allocated, op: sum, of: amount }
```

`capacity`/`balance`/`status` are refused for a cross-model PARENT (they read its own fields and
seeds) but work for a cross-model CHILD, where they are writes on the local parent - except the
overdraw guard, which belongs to the child's own DAO and is reported as not installed. The vacated
side of a re-parented FOREIGN child is repaired only when the owner model marks that relation as a
grouping key.

## settlements - payment allocation

```yaml
settlements:
  - name: autoAllocate
    junction: SalesInvoiceCustomerPayment
    invoice: SalesInvoice
    payment: CustomerPayment
    amount: amount
    total: total
    paid: paid
    pot: amount
    order: date                       # oldest first
    match: [Customer, Currency]
    status: Status
    payableStatuses: [3, 4, 6]
```

Generates the on-payment spread handler and an on-invoice pull delegate; pair with a `rollups` sum
entry that maintains `paid`/`balance`/status. The spread handler is bound to the payment's create
AND its update event, and is a recompute of the payment's unallocated balance rather than an append:
a payment corrected after it was booked - or created incomplete and completed later - is re-allocated
for the amount it actually carries, and an amount corrected downwards releases the excess allocation
(newest first).

## reports - read-only aggregations

```yaml
reports:
  - name: OrdersByMonth
    source: Order
    dimensions: ["month(orderDate)"]          # month()/year() bucket dates; relation.field joins
    measures: ["count(*)", "sum(total)"]
    filter: "total > 0"
    chart: bar                                # render as a chart page
    widget: { value: "sum(total)", at: { "month(orderDate)": now }, label: Revenue (this month) }
  - name: TrialBalance
    kind: balance                             # opening / period / closing debit+credit per dimension
    source: JournalEntryItem
    date: journalEntry.entryDate              # runtime From/To pickers
    debit: debit
    credit: credit
    dimensions: [account.code, account.name]
    filter: "journalEntry.status == 2"
  - name: GeneralLedger
    kind: balance
    source: JournalEntryItem
    date: journalEntry.entryDate              # its first hop is the document the lines share
    debit: debit
    credit: credit
    dimensions: [account.code, account.name]
    correspondence: account.code              # turnovers per corresponding account, allocated
```

In `filter:`, reference relations via `relation.field` (translated to a JOIN); a bare relation
name passes into the SQL untranslated.

## widgets - custom dashboard tiles

```yaml
widgets:
  - { name: SystemHealth, kind: kpi,  url: /services/js/myapp/custom/health.js, icon: activity }
  - { name: SalesFunnel,  kind: page, url: /services/web/myapp/custom/funnel/index.html }
```

## seeds - initial data

```yaml
seeds:
  - name: statuses
    entity: OrderStatus
    rows:
      - { id: 1, name: DRAFT }
      - { id: 2, name: POSTED }
  - name: cities
    entity: City
    rows:
      - { id: 1, name: Sofia, Country: 34 }   # FK by the relation's authored name (case-sensitive)
  - name: countries
    entity: Country
    file: data/countries.csv                  # large sets: developer-owned CSV in a subfolder
  - name: uoms-bg
    entity: UoM
    language: bg                              # translations for a multilingual entity (_LANG)
    rows:
      - { id: 1, name: "Килограм" }
```

Row keys must match a field or relation name exactly (case-sensitive).

## notifications - email on change

```yaml
notifications:
  - name: welcomeMember
    event: { onCreate: Member }               # exactly one of onCreate/onUpdate/onDelete
    to: email                                 # a field, one-hop relation.field, or a literal
    subject: "Welcome"
    body: "Your membership is active."
```

## schedules - cron

Per matching row, exactly one of `notify` or `generate`:

```yaml
schedules:
  - name: monthlyTimesheets
    cron: "0 0 1 1 * ?"
    entity: Employee                          # the schedule's SOURCE must be local
    where:
      - { field: status, op: eq, value: ACTIVE }
    generate:
      to: EmployeeTimesheet                   # cross-model target via `uses:` alias
      map: { Employee: id }
      defaults: { Period: now }
```

A `where` value is a literal or a **moment**: `CURRENT_DATE` / `CURRENT_TIMESTAMP` (`NOW`), optionally
offset by a single signed ISO-8601 duration resolved against the clock of the run that fires - which is
what makes a staleness sweep expressible:

```yaml
    where:
      - { field: provisioningStatus, op: eq, value: Provisioning }
      - { field: changedAt,          op: lt, value: "CURRENT_TIMESTAMP-PT30M" }   # stuck for 30 minutes
      - { field: sentOn,             op: lt, value: "CURRENT_DATE-P7D" }          # unanswered for a week
```

Exactly one offset on one token - a moment vocabulary, not an expression language. The comparison
happens in the queried field's own shape, so a `date` field takes `CURRENT_DATE` and a date-only amount
(`P7D`/`P1M`/`P1Y`) while a `timestamp` field takes `CURRENT_TIMESTAMP` and any amount; a mismatched
token, a time offset on a date, a second offset, or a moment on a non-temporal field is an authoring
error rather than a query that silently never matches.

## integrations - outbound HTTP

```yaml
integrations:
  - { name: pushNewMember, event: { onCreate: Member }, method: POST, url: "https://api.example.com/members" }
```

## inbound - webhooks, queues and drop folders

```yaml
inbound:
  - { name: leadHook, path: /webhooks/lead, create: Lead }
```

An arrival may declare how its payload is READ, on any of the three shapes - what the payload looks
like has nothing to do with what it travelled on. Without these keys the payload deserializes straight
into the entity, which works only when the sender's JSON already **is** the entity, field for field:

```yaml
inbound:
  - name: userAssignments
    source: { queue: "global:codbex.user-assignment-requests" }
    accept: { type: user.assignment.requested, version: 1 }   # anything else: warn and ignore
    create: TenantUserAssignment
    map:
      messageId: messageId                                     # entity field <- envelope key
      tenant:    { lookup: Tenant, by: tenantId, from: tenantId }   # business key -> relation FK
```

`accept:` gates on the envelope keys it names; a message that does not match is **acknowledged and
ignored** with a warning (202 on a webhook, a skipped record in a drop file), never failed into
redelivery - a sender rolling out a new version must not fill the receiver's error queue. `map:`
projects the envelope onto the record, and a `lookup:` resolves a **business key to a relation**: `by:`
must be a **unique** field of the target (or its primary key), since a lookup that could match several
rows would silently pick one, and one that matches nothing **rejects** the arrival rather than storing a
null relation. Everything still saves through the entity's own repository. v1 lookups are same-model.

## outbound - emit on a queue or topic

The mirror of `inbound`: when an event fires, emit a message. `to:` names exactly one of
`queue`/`topic`; without a `payload:` the body is the record's own JSON.

```yaml
outbound:
  - name: publishOrder
    event: { onCreate: Order }
    to: { queue: "codbex.orders" }
  - name: announceActivation
    event: { onStepCompleted: { process: OrderApproval, step: activate }, when: "channel != internal" }
    to: { topic: "codbex.order-activations" }
    payload:                                  # the declared envelope, as on integrations
      type: "order.activated"
      messageId: "{uuid}"
      reference: number
```

The message is published after the write that raised the event is persisted and is **not**
transactional with it - a failed publish is logged and the write stands. No outbox, no exactly-once,
no ordering. A destination name is application-owned and therefore tenant-scoped; mark it `global:`
when the queue or topic is a contract with a system outside this deployment (the platform marker from
[#6766](https://github.com/eclipse-dirigible/dirigible/issues/6766) - the name is passed through
verbatim, so nothing in the intent layer resolves it).

## permissions - roles

```yaml
permissions:
  - { role: Librarian, can: [Member:read, Member:write, Loan:approve] }
```

## Print, tests and the shell (generated automatically)

Every document (header-items) master also gets a standard `<Entity>.print` template (the Print
button renders PDF via the document-template engine, per-language via CMS folders), a `<name>.test`
UI-test manifest, and its perspective in the generated Harmonia SPA + the shared application shell
(dashboard, Inbox, Documents, Reports, Settings incl. Region & Language).

## Planned - recognised but not yet implemented

- **Reserved `function:` roles** - `Board`, `Gantt`, `Timeline`; rejected with a clear "not yet
  available" message. (`function: Calendar` is now first-class - the role alias for
  `view: calendar`.)
- **Bridge fields on a generated `manyToMany` link** - the materialized link entity carries only its
  key and the two FKs; a link with its own data is authored as an explicit intermediate entity.
- **Declarative glue actions beyond the current set** (see CLAUDE.md "Planned: declarative glue"):
  `generateDocument` (PDF), `assign`. Today's implemented glue: triggers, decision/form resolvers,
  notifications, schedules, integrations, inbound arrivals (webhook / queue-topic / drop folder),
  outbound departures, rollups, settlements, expansions, generates, postings.
- **Cross-model schedule SOURCE** - a schedule's `entity` must be local (the generate target may
  be cross-model).
- ~~`generates` completion hook~~ - LANDED (#6237): `sourceStatus` flips the source's
  EntityStatus after the target is created.
- **Embedded calendar panel for a DEPENDENT composition child** inside its master page - calendar
  views require a PRIMARY entity today.
- **Pipeline hardening follow-ups** (tracked on the emission-coverage IT): seed-row key
  validation at generate time, surfaced + retried CSVIM import failures, `checks:` violations
  mapped to 4xx, generator-version stamping of generated output.
