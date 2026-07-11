# Personalized surfaces — design decisions & implementation plan

Status: **AGREED 2026-07-11**; also agreed: the entity `label:` display-name feature (shipped alongside phase A - see the engine-intent README). This is the design doc + status tracker, in the spirit of
[`HARMONIA_RUNTIME_PLAN.md`](./HARMONIA_RUNTIME_PLAN.md).

## The problem

Generated applications have ONE UI per entity. For entities that are *a person's own records*
(vacation requests, timesheet day allocations), one surface is wrong for everybody:

- the **back-office user** needs everything — all rows, all fields (correct a timesheet while its
  owner is away);
- the **employee** needs a surface bound to *themselves* — their rows only, the owner preselected
  and immutable, and confidential fields (billing rates, amounts) absent.

Bolting per-role field rules onto the one UI rots fast and leaks on the first missed condition.

## The decision: one model, two generated surfaces

- **Power surface** — exactly today's generated UI. Unchanged, role-gated as now.
- **Personal surface** — generated *additionally* for personalized entities: rows filtered
  server-side to the logged-in user's mapped record; the owner FK **forced server-side** on
  writes (client value ignored) and rendered read-only; `sensitive` fields absent from the pages
  **and from the wire** (the personal controller serializes an allow-list — UI-only hiding is
  cosmetic security).

Deliberately **NOT** doing: a general `hiddenFor: [role]` field matrix (two fixed audiences are
auditable; revisit only if a third audience materializes), and per-manager task routing
(approvals stay `flowable:candidateGroups` — any member of the role approves; decided
2026-07-11).

## The DSL (three words)

```yaml
# 1. Once, on the mapping entity (the module that owns it):
- name: Employee
  identity: email            # field matched against the logged-in username

# 2. On the to-one relation that means "whose record this is":
- { name: Employee, kind: manyToOne, to: Employee, model: kf-mod-employees, personal: true }

# 3. On fields the person must never see:
- { name: rate, type: decimal, sensitive: true }
```

Semantics:

- `identity: <field>` — the platform resolves *current user → row of this entity whose field
  equals the username → id*, per request, tenant-scoped. No mapping row → personal surfaces are
  empty (never an error). Consumers referencing the entity cross-model inherit the mapping
  through the resolved `.model` (like every other cross-model target attribute).
- `personal: true` — valid on a `manyToOne`/`oneToOne` whose target declares `identity`. Drives
  the personal surface: `WHERE <fk> = :me` on reads, forced FK on writes, locked control.
  **Composition children inherit the scope through the parent chain** (a day allocation is
  personal because its EmployeeTimesheet is) — the child's guard is ancestor-ownership, no
  annotation on the child.
- `sensitive: true` — stripped from personal pages and nulled/omitted in personal REST
  responses; write attempts ignored. Power surface unaffected. Not valid on the PK, the personal
  FK, or the identity field.

## Phases (one PR each; sequence matters)

| Phase | Scope | Status |
|---|---|---|
| **A** | DSL (`identity`/`personal`/`sensitive`) + backend contract: parser validations, EDM/model emission, cross-model identity resolution, generated **personal REST controller** (scoped reads, forced FK, ancestor-ownership for children, sensitive stripping) | **done** |
| **B** | Personal UI: "My …" pages — a list per personal root + a form per personal entity (owner and sensitive fields never rendered; children as embedded panels — x-h-calendar for detailCalendar children with date-click booking, a table otherwise), SPA routes + a "My" sidebar section, and a shell perspective per root on **`application-personal-perspectives`** | **done** |
| **C** | **My Shell** — `/services/web/my/` (`resources-my`): the personal counterpart of the shared application shell, aggregating every module's personal perspectives under the single 'my' group; task-first (the Inbox IS the home page — a personal task is how work arrives); no projections/documents/reports (power surfaces). Gotcha encoded: a perspective whose groupId matches no group is SILENTLY DROPPED by the perspectives service, and a group definer must carry `items: []` to classify as a group | **done** |
| **D** | Per-user task assignment: `assignee: personal` on a user task → `flowable:assignee="${__personalUser}"`, resolved by the trigger listener through the personal relation + identity mapping and carried IN the start payload (assignee expressions evaluate at task creation, inside Process.start) | **done** |
| **E** | Collection-driven generation: `schedules`/`generates` emit **child rows from a source collection** (e.g. one timesheet line per assigned employee + one allocation per working day) | planned |
| **F** | Document item dialog honors `readOnly`: read-only columns render as values, not controls (saves already ignored them - the input was fake editability) | **done** |

## Flagship application (the consuming suite's timesheets flow)

Start of month, per active project: generate the timesheet header + one line per **assigned**
employee + one allocation per working day (default **8 hours** — the unchanged month confirms in
one click). A small per-line process drops **"Fill & confirm"** into that employee's Inbox (phase
D) — the task IS the deep link, no notification-URL plumbing. The employee adjusts hours (0 is
fine), sets tasks, removes days, confirms; the existing manager approval then runs as today.

Other decisions recorded: v1 pre-generation does NOT skip approved vacation days (cross-module
read deferred); local dev with basic-auth maps `admin` by seeding a dev identity row
(`email: admin`) in demo/fixture data; naming stays `identity` / `personal` / `sensitive`.

## Testing contract

Per the engine-intent guide: every phase extends the emission-coverage fixture AND asserts the
outermost layer — phase A: generated tokens (`identityProperty`, `relationshipPersonal`,
`sensitiveProperty`, the `My` controller) and REST behavior over a published app (scoped list,
403/404 on foreign records, forced FK, sensitive absence), with the fixture seeding an identity
row for the IT user so the personal surface is exercisable.
