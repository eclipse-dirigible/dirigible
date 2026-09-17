# sample-intent-resilience

A minimal intent project exercising **declarative step resilience**
([eclipse-dirigible/dirigible#6762](https://github.com/eclipse-dirigible/dirigible/issues/6762) and
[#7056](https://github.com/eclipse-dirigible/dirigible/issues/7056)): `retry: { count, every }` and
`onError:` on a **`delegate:`** and on a **`notify:`** service task, the `{error}` placeholder, and
declared step data (`vars:` + `produces:`/`uses:`/`clearAfter`).

This folder is both the **manual-testing project** (import it into a workspace and follow "Run it"
below) and the fixture of **`IntentResilienceSampleIT`**, which drives the very same journey through
the browser IDE - opens `app.intent` in the Intent Editor, clicks Generate, publishes via the
Workbench - and asserts the same outcomes automatically, so the sample can never silently rot.

One process, two hand-written delegates and one send, every outcome reachable from the UI:

- **`SchemaProvisioner`** fails its first two attempts per tenant and succeeds on the third — the
  declared `retry: { count: 3, every: PT10S }` recovers it with no incident, producing the
  `dbPassword` step data.
- **`AppProvisioner`** consumes `dbPassword` (`uses:`) and stamps `generatedKey` on the record —
  unless the tenant's **title contains "fail"**, in which case every attempt throws: its
  `retry: { count: 2 }` exhausts after three attempts and the `onError` route records the FINAL
  attempt's message into `failureMessage` via `{error}`, then sets the status to Failed.
- **`notifyOwner`** is a **send** step (`notify:`), reached when the tenant's title is exactly
  **"notify"**. This sample configures no SMTP, so the delivery always fails - and a send step's
  whole work is the message, so that fails the task. Its `retry: { count: 1, every: PT5S }`
  re-attempts once and the `onError` route then records the FINAL attempt's message the same way a
  delegate's does. That is #7056: before it, `retry:`/`onError:` were refused on a send, so this
  step could only dead-letter - the flow stopped at it, the tenant stayed in **Requested**, and
  `failureMessage` stayed empty. Note the send is deliberately **not** the last step: making it last
  was the available workaround, and it constrained process design for a reason unrelated to the
  domain.
- `clearAfter: provisionApp` removes `dbPassword` from the instance data once the app step
  completes, so the credential does not survive in the process history.

## Prerequisite

A Dirigible build that includes both changes — #6762
([PR #6783](https://github.com/eclipse-dirigible/dirigible/pull/6783)) for the delegate half and
#7056 for the send half. Both add runtime code (the conversion of an exhausted failure into the
caught BPMN error, on the `flowable:class` and the `flowable:delegateExpression` path respectively),
so an older build refuses the intent at Generate or dead-letters the send.

## Run it

1. Import this folder as a project into your workspace (zip-import or copy it in; the project
   name below is assumed to be `sample-intent-resilience` — adjust the URLs if you name it
   differently).
2. Open `app.intent` (double-click → the Intent Editor). The diagram's process pane shows the
   dashed **`on error (after N retries)`** edges. Click **Generate**.
3. **Publish** the project.
4. Create a tenant — via the generated UI (`/services/web/sample-intent-resilience/gen/provisioning/index.html`)
   or REST:

   ```sh
   # The happy path: watch the Logs view - two deliberate failures, two ~10s retry waits, then success.
   curl -s -u admin:admin -H 'Content-Type: application/json' \
        -d '{"Title":"acme"}' \
        http://localhost:8080/services/java/sample-intent-resilience/gen/provisioning/api/tenantapplication/TenantApplicationController

   # The failure path: every attempt refuses, the third (1 + count: 2) routes to recordFailure.
   curl -s -u admin:admin -H 'Content-Type: application/json' \
        -d '{"Title":"please fail"}' \
        http://localhost:8080/services/java/sample-intent-resilience/gen/provisioning/api/tenantapplication/TenantApplicationController

   # The send path: provisioning succeeds, then the mail cannot be delivered - one retry, then the
   # same onError route. Without #7056 this step would dead-letter and the flow would stop here.
   curl -s -u admin:admin -H 'Content-Type: application/json' \
        -d '{"Title":"notify"}' \
        http://localhost:8080/services/java/sample-intent-resilience/gen/provisioning/api/tenantapplication/TenantApplicationController
   ```

5. Watch it settle (each retry waits ~10s, plus the async executor's acquire cycle):

   ```sh
   curl -s -u admin:admin \
        http://localhost:8080/services/java/sample-intent-resilience/gen/provisioning/api/tenantapplication/TenantApplicationController
   ```

   - **"acme"** ends with `GeneratedKey` set (proof the produced credential flowed through
     `uses:`) and status **Provisioned**.
   - **"please fail"** ends with `FailureMessage: "no capacity for 'please fail' (attempt 3)"` —
     the FINAL attempt's message, not the first — and status **Failed**. No dead-letter job, no
     incident.
   - **"notify"** ends with `FailureMessage` starting
     `"Failed to send the notifyOwner mail of the TenantProvisioning process: "` and status
     **Failed** — again with no dead-letter job. Configure a working SMTP
     (`DIRIGIBLE_MAIL_SMTP_HOST` and friends) and the same tenant ends **Provisioned** instead, the
     send having succeeded on its first attempt.

6. The cleared credential: while an instance is still running (or via the successful record's
   `ProcessId` right after the app step), list its variables —

   ```sh
   curl -s -u admin:admin \
        http://localhost:8080/services/bpm/bpm-processes/instance/<ProcessId>/variables
   ```

   `dbPassword` is absent the moment `provisionApp` completes (on the failure path it lingers
   until the instance ends — `clearAfter` fires on the step's NORMAL completion only). The
   retries themselves are visible in the Processes view while an instance is between attempts.

## Files

- `app.intent` — the whole model; Generate derives `.edm`/`.model`, `TenantProvisioning.bpmn`
  (retry cycles, error boundary events, the clearing end-listener), seeds and the app code.
- `custom/SchemaProvisioner.java`, `custom/AppProvisioner.java` — the hand-written delegates the
  intent binds via `delegate:`; `custom/` is developer-owned and survives regeneration. The send
  needs no hand-written code at all: `notify:` generates its own handler.
