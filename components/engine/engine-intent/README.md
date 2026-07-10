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
contributor internals live in [`CLAUDE.md`](CLAUDE.md). Every enforcement-bearing construct below
is covered end to end (generated tokens + runtime behavior) by `IntentEmissionCoverageIT`.

## Index

| Construct | What it gives you |
|---|---|
| [`entities`](#entities) | tables + CRUD UI + generated Java repository/REST |
| [field/relation attributes](#field--relation-attributes) | uniqueness, layout, read-only, dropdown filtering, cascades |
| [`function`](#function--presentation-role) | explicit presentation role (Document, Setting, ...) |
| [`checks`](#checks--declarative-validations) | cross-field / cross-line validations |
| [`immutableIn`](#immutablein--status-keyed-immutability) | 409 on user writes in a given status |
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
| [`inbound`](#inbound--webhooks) | webhook that creates records |
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

## immutableIn - status-keyed immutability

```yaml
- name: JournalEntry
  immutableIn: [2]     # while Status holds seed id 2 (POSTED), REST update/delete return 409
```

Requires a `function: EntityStatus` relation. Workflow/system writes through the repository stay
possible - corrections are flow-generated reversals, never edits.

## hierarchy / leafOnly - tree entities

```yaml
- name: Account
  hierarchy: Parent                                        # the tree edge (self-relation)
  relations:
    - { name: Parent, kind: manyToOne, to: Account }
# elsewhere - only leaf accounts are referenceable (server-enforced):
- { name: Account, kind: manyToOne, to: Account, model: kf-accounts, leafOnly: true }
```

The list renders as an expandable tree; the server rejects cycles and leaf-only references to a
node with children.

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

## uses - cross-model references

Entities owned by another intent model are referenced read-only (a PROJECTION + FK + dropdown - no
local table/DAO). Generate leaf-first so the owner's model exists.

```yaml
uses:
  - { model: kf-mod-countries }
entities:
  - name: Supplier
    relations:
      - { name: Country, kind: manyToOne, to: Country, model: kf-mod-countries }
```

Many-to-many is an explicit intermediate entity (composition to one side + `manyToOne` to the
other, plus bridge fields); `manyToMany` is parsed but never materialized.

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
```

Adds a button on the source view; the clone saves through the target's repository so numbering,
status init and calculated fields fire.

## postings - source-document to ledger

When a (usually cross-model) source document reaches a status, create ONE local document with
computed multi-line content. Idempotent via the back-reference; a missing rule/account skips (the
unposted worklist), never throws.

```yaml
postings:
  - name: salesInvoicePosting
    event: { onTransition: SalesInvoice, model: kf-mod-sales-invoices, when: "Status == 3" }
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

A span change replaces the generated child set; never mix hand-entered rows into an expanded child.

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
entry that maintains `paid`/`balance`/status.

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

## integrations - outbound HTTP

```yaml
integrations:
  - { name: pushNewMember, event: { onCreate: Member }, method: POST, url: "https://api.example.com/members" }
```

## inbound - webhooks

```yaml
inbound:
  - { name: leadHook, path: /webhooks/lead, create: Lead }
```

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

- **`function: Calendar`** - reserved as an entity presentation role; rejected with a clear "not
  yet available" message (the `view: calendar|range|slots` pages above are the current calendar
  surface). Likewise other reserved `function` values for upcoming templates.
- **`manyToMany`** - parsed but never materialized; the supported shape is the explicit
  intermediate entity.
- **Declarative glue actions beyond the current set** (see CLAUDE.md "Planned: declarative glue"):
  `publish`/consume message, `generateDocument` (PDF), `assign`, process-step events, inbound
  message/file events. Today's implemented glue: triggers, decision/form resolvers,
  notifications, schedules, integrations, inbound webhooks, rollups, settlements, expansions,
  generates, postings.
- **Cross-model schedule SOURCE** - a schedule's `entity` must be local (the generate target may
  be cross-model).
- **`generates` completion hook** - flipping the SOURCE record's status after creating the target
  (`onDone`-style) is not yet expressible.
- **Embedded calendar panel for a DEPENDENT composition child** inside its master page - calendar
  views require a PRIMARY entity today.
- **Pipeline hardening follow-ups** (tracked on the emission-coverage IT): seed-row key
  validation at generate time, surfaced + retried CSVIM import failures, `checks:` violations
  mapped to 4xx, generator-version stamping of generated output.
