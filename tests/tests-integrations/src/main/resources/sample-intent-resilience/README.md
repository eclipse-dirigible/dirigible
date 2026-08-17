# sample-intent-resilience

A minimal intent project exercising **declarative step resilience**
([eclipse-dirigible/dirigible#6762](https://github.com/eclipse-dirigible/dirigible/issues/6762)):
`retry: { count, every }` and `onError:` on delegate service tasks, the `{error}` placeholder, and
declared step data (`vars:` + `produces:`/`uses:`/`clearAfter`).

This folder is both the **manual-testing project** (import it into a workspace and follow "Run it"
below) and the fixture of **`IntentResilienceSampleIT`**, which deploys it and asserts the same two
outcomes automatically - so the sample can never silently rot.

One process, two hand-written delegates, both outcomes reachable from the UI:

- **`SchemaProvisioner`** fails its first two attempts per tenant and succeeds on the third — the
  declared `retry: { count: 3, every: PT10S }` recovers it with no incident, producing the
  `dbPassword` step data.
- **`AppProvisioner`** consumes `dbPassword` (`uses:`) and stamps `generatedKey` on the record —
  unless the tenant's **title contains "fail"**, in which case every attempt throws: its
  `retry: { count: 2 }` exhausts after three attempts and the `onError` route records the FINAL
  attempt's message into `failureMessage` via `{error}`, then sets the status to Failed.
- `clearAfter: provisionApp` removes `dbPassword` from the instance data once the app step
  completes, so the credential does not survive in the process history.

## Prerequisite

A Dirigible build that includes the #6762 change
([PR #6783](https://github.com/eclipse-dirigible/dirigible/pull/6783) or any later build) — the
runtime half (converting the exhausted failure into the caught BPMN error) is new.

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
  intent binds via `delegate:`; `custom/` is developer-owned and survives regeneration.
