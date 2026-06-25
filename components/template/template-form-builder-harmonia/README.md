# template-form-builder-harmonia

Runtime form generator for the **Alpine.js + Harmonia** stack — the counterpart to
`template-form-builder-angularjs`. Given a `.form` artifact it generates a standalone
Harmonia form page (`gen/<genFolder>/forms/<form>/index.html` + `form.js`), used for
app forms and **BPM task forms** (opened by the `processTasks` store of
`template-application-ui-harmonia-java`).

Registered on `platform-templates` (extension `form`), so it appears in the Generate
picker and can be selected as the intent recipe's `form` template
(`generation.form.templateId` in `<intent>.settings`).

## The neutral `formController(ctx)` contract (read this first)

A `.form`'s `code` block in the AngularJS runtime is written against `$scope` / `$http`,
which an Alpine/Harmonia runtime cannot execute. This module therefore defines a
**framework-neutral contract**: the `code` is the **body of `formController(ctx)`** and
uses `ctx` instead of Angular globals:

| ctx member | purpose |
|---|---|
| `ctx.model` | the reactive form model (bound to each widget's `x-model="model.<key>"`); for a **BPM task form** it is preloaded from `GET /services/inbox/tasks/{id}/variables` so fields show the task's current values |
| `ctx.params` | parsed query params (`taskId`, `processInstanceId`, an entity id, …) |
| `ctx.http` | the platform fetch client (`get/post/put/delete`; pass `{ baseUrl: '' }` for an absolute URL such as a generated controller or `/services/...`) |
| `ctx.task` | `{ id, processInstanceId, complete(variables?) }` — `complete()` POSTs to `/services/inbox/tasks/{id}` |
| `ctx.notify(msg)` | show a status message on the form |
| `ctx.close()` | ask a hosting SPA dialog to close (no-op standalone) |

The code attaches button handlers onto `ctx`; a widget `button`'s `callback` names the
handler, invoked via the page's `run()`:

```js
// neutral .form code (body of formController(ctx))
ctx.onApproveClicked = async () => {
  await ctx.http.put(`/services/.../requests/${ctx.params.taskId}/approve`, {}, { baseUrl: '' });
  await ctx.task.complete();
  ctx.notify('Request approved');
  ctx.close();
};
```

### AngularJS `.form` compatibility (no rewrite required)

`.form` artifacts authored against the AngularJS form-builder (e.g. the ones the intent's
`FormIntentGenerator` emits) use `$scope` / `$http` / `NotificationHub` / `DialogHub`.
Rather than force a rewrite, `form.js` defines **compatibility shims** inside
`formController(ctx)` that map those onto `ctx`:

| legacy global | shimmed to |
|---|---|
| `$scope` | `ctx` (so `$scope.model` and `$scope.onXClicked = fn` land on `ctx`) |
| `$http.{get,post,put,delete}` | `ctx.http.*`, returning an AngularJS-style `{ data }` promise (rejects with `{ data: { message }, status }`) |
| `NotificationHub().show({title,description})` | `ctx.notify(...)` |
| `DialogHub().closeWindow()` | `ctx.close()` |

So an intent-generated task form (`$scope.onApproveClicked = …; $http.post('/services/bpm/…')`)
runs unchanged. New forms can still be authored directly against the neutral `ctx`.

## Status (v1)

- **Widgets rendered:** header, paragraph, line, spacer, link, input-textfield,
  input-textarea, input-number, input-checkbox, input-date, input-datetime-local,
  input-time, input-color, button, and one level of `container-hbox` / `container-vbox`.
- **Fallback:** any other `controlId` (image, input-documents, input-combobox/select/radio
  with feeds, table, stepIndicator, progress) renders as a labelled text input with a TODO.
- **Assets (self-contained):** the page loads only `form.js` + the Harmonia/Alpine/Lucide
  webjars. `form.js` carries its own minimal fetch client (`harmoniaHttp`), so the form does
  **not** depend on the SPA shell's `window.App`. This matters because a BPM task form is
  opened standalone (an iframe/dialog with `?taskId=&processInstanceId=`), where the shell
  assets are not present at a predictable relative path (an earlier `../../js/...` reference
  404'd and left `App` undefined).

## Follow-ups
- Feed-driven widgets (combobox/select/radio with `feeds`), documents, table, stepIndicator.
- Optionally have `FormIntentGenerator` emit neutral `ctx` handlers directly (the compat
  shims make this optional rather than required).
- A close/refresh handshake with the host dialog (`harmonia.form.close` postMessage is sent;
  the SPA shell could listen and close + refresh proactively).
