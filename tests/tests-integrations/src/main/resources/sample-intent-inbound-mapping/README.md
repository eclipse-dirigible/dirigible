# sample-intent-inbound-mapping

A minimal intent project exercising **mapping on arrival**
([eclipse-dirigible/dirigible#6769](https://github.com/eclipse-dirigible/dirigible/issues/6769)):
`accept:` (a type/version gate) and `map:` (an envelope-to-entity projection) on an `inbound` entry,
including the `lookup:` that turns a **business key into a relation** - and **arrival collections**
([eclipse-dirigible/dirigible#7593](https://github.com/eclipse-dirigible/dirigible/issues/7593)), which
turn an **array in the envelope into composition child rows**.

This folder is both the **manual-testing project** (import it into a workspace and follow "Run it"
below) and the fixture of **`IntentInboundMappingSampleIT`**, which drives the very same journey
through the browser IDE - opens `app.intent` in the Intent Editor, clicks Generate, publishes via the
Workbench - and asserts the outcomes automatically, so the sample can never silently rot.

## What it demonstrates

Before this feature an arrival was `Json.parse(payload, <Entity>Entity.class)`, so it could only be
ingested when the sender's JSON already **was** the entity, field for field. The envelope this project
receives is the one real integrations actually use:

```json
{ "messageId": "9f9d1c9e-...", "type": "user.assignment.requested", "version": 1,
  "tenantId": "acme", "email": "new.user@example.com", "role": "User", "seatCount": 3 }
```

Three things the old shape could not do, and this project declares instead:

- **`map:`** projects the envelope's keys onto the entity's own (`seatCount` -> `Seats`). A key the map
  does not name is not the record's business.
- **`lookup:`** resolves `tenantId: "acme"` to the `Tenant` foreign key and `role: "User"` to the
  `AssignmentRole` one. This is the single most common requirement of any arrival, and on its own the
  reason a modelled arrival still needed a hand-written consumer. `by:` must be a **unique** field of
  the target (both registers declare one), because a lookup that could match several rows would
  silently pick one. A lookup that matches nothing **rejects** the message - never a null relation.
- **`accept:`** ignores what this application does not understand. A `version: 2` message is
  acknowledged and dropped with a warning rather than failed, so a sender rolling out a new version
  cannot fill this receiver's error queue.

The same `accept:`/`map:` block appears on both arrivals - a queue and a webhook - because it
describes the payload, not the transport. `AssignmentRole` is a `function: Setting` nomenclature, so
its generated repository lives under the shared `Settings` perspective: the lookup's import resolves
through it, which is exactly the resolution a settings-unaware one would get wrong.

### Arrays become child rows

An order arrives with its lines and its tags:

```json
{ "orderNumber": "PO-1",
  "lines": [ { "sku": "A", "qty": 2 }, { "sku": "B", "qty": 1 } ],
  "tags":  [ "rush", "gift" ] }
```

`lines` and `tags` are one-to-many relations of `PurchaseOrder` whose targets are composition
children, so each takes a **collection**: `from:` names the array, and the collection's own `map:` fills
one child row per element. A line's `sku` is a lookup like any other, and `tags` maps the bare element
itself (`label: "."`). The arrival stores **one** `PurchaseOrder`, two `PurchaseOrderLine`s and two
`PurchaseOrderTag`s.

- **All or nothing.** Every element is resolved before anything is written. An unknown sku, a `null`
  tag or a sixth line (the declared `max: 5`) rejects the whole order - the queue logs an ERROR naming
  the element, the webhook answers **400** - and nothing of it is stored.
- **One transaction, then one process.** The order and its lines are saved together, and the create
  event is dispatched after the commit. The `CountLines` process it starts writes the number of lines it
  found into `LinesSeen` (`custom/CountLinesTask.java`): **2** for the order above, which is the proof
  that the lines were there before the process started.

## Run it

1. Import this folder as a project into your workspace (zip-import or copy it in; the project name
   below is assumed to be `sample-intent-inbound-mapping` - adjust the URLs if you name it
   differently).
2. Open `app.intent` (double-click -> the Intent Editor). The glue card for each arrival is badged
   with its gate and its lookups. Click **Generate**.
3. **Publish** the project. The seeds import two tenants (`acme`, `globex`) and two roles
   (`User`, `Administrator`).
4. Post a valid envelope to the webhook:

   ```bash
   curl -u admin:admin -H 'Content-Type: application/json' \
     -d '{"messageId":"m-1","type":"user.assignment.requested","version":1,
          "tenantId":"acme","email":"new.user@example.com","role":"User","seatCount":3}' \
     http://localhost:8080/services/java/sample-intent-inbound-mapping/gen/events/assignments/AssignmentHookWebhook/assignments
   ```

   The answer is the saved record, with `Tenant` and `Role` carrying the seeded **ids** - resolved
   from the names the envelope sent.

5. Post one this application does not understand (`"version":2`) and it answers **202** with
   `{"ignored": ...}`; nothing is stored, and the log carries the warning.
6. Post one naming a tenant that does not exist (`"tenantId":"nope"`) and it answers **400**; again
   nothing is stored, rather than a record with a hole in it.
7. The queue arrival behaves identically, and `custom/AssignmentRequestSender.java` is here so it can
   be tried by hand. The platform's broker listens on `vm://localhost` only - there is no TCP
   transport - so a message cannot be published from outside the running instance; that controller
   publishes from within it:

   ```bash
   curl -u admin:admin -H 'Content-Type: application/json' \
     -d '{"messageId":"q-1","type":"user.assignment.requested","version":1,
          "tenantId":"globex","email":"queue.user@example.com","role":"Administrator","seatCount":7}' \
     http://localhost:8080/services/java/sample-intent-inbound-mapping/custom/AssignmentRequestSender/send
   ```

   Consuming is asynchronous, so read the records back a moment later: the new row carries
   `Tenant: 2` and `Role: 2` - resolved from `globex` and `Administrator`. Sending the same envelope
   with `"version":2` stores nothing and logs the ignore, and one naming a role that does not exist
   stores nothing and logs the rejection; both under the logger
   `gen.events.assignments.AssignmentRequestsConsumer`, visible in the Logs view or the console.

   Mark the destination `global:` when the queue is a contract with another deployment (the platform's
   external-contract marker; the name is passed to the broker verbatim).

8. The orders arrive the same two ways - posted to the webhook, or published by
   `custom/OrderSender.java`:

   ```bash
   curl -u admin:admin -H 'Content-Type: application/json' \
     -d '{"orderNumber":"PO-1","lines":[{"sku":"A","qty":2},{"sku":"B","qty":1}],"tags":["rush","gift"]}' \
     http://localhost:8080/services/java/sample-intent-inbound-mapping/gen/events/assignments/OrderHookWebhook/orders

   curl -u admin:admin -H 'Content-Type: application/json' \
     -d '{"orderNumber":"PO-2","lines":[{"sku":"A","qty":1}],"tags":["gift"]}' \
     http://localhost:8080/services/java/sample-intent-inbound-mapping/custom/OrderSender/send
   ```

   The seeds import two products, `A` and `B`. Change a sku to one that does not exist and the webhook
   answers 400 and the queue logs the rejection under `gen.events.assignments.OrdersConsumer`; either
   way no order, line or tag is stored. A moment after an accepted order, its `LinesSeen` shows how many
   lines the `CountLines` process counted.

Read the records back at:

```
http://localhost:8080/services/java/sample-intent-inbound-mapping/gen/assignments/api/tenantuserassignment/TenantUserAssignmentController
http://localhost:8080/services/java/sample-intent-inbound-mapping/gen/assignments/api/purchaseorder/PurchaseOrderController
http://localhost:8080/services/java/sample-intent-inbound-mapping/gen/assignments/api/purchaseorder/PurchaseOrderLineController?PurchaseOrder=<id>
```

or through the generated UI at
`/services/web/sample-intent-inbound-mapping/gen/assignments/index.html`.
