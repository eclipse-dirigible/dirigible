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
| `ctx.model` | the reactive form model (bound to each widget's `x-model="model.<key>"`) |
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

> **Migration:** existing `.form` files with AngularJS `code` must have that code
> rewritten to this contract to run under Harmonia. **Follow-up:** update
> `FormIntentGenerator` (engine-intent) to emit neutral handlers instead of the current
> `$scope` stubs, so intent-generated task forms render in Harmonia out of the box.

## Status (v1)

- **Widgets rendered:** header, paragraph, line, spacer, link, input-textfield,
  input-textarea, input-number, input-checkbox, input-date, input-datetime-local,
  input-time, input-color, button, and one level of `container-hbox` / `container-vbox`.
- **Fallback:** any other `controlId` (image, input-documents, input-combobox/select/radio
  with feeds, table, stepIndicator, progress) renders as a labelled text input with a TODO.
- **Assets:** reuses the co-generated SPA shell's `app.js` / `config.js` / `api.js` /
  `apiError.js` (`../../js/...`) + the Harmonia/Alpine/Lucide webjars — so a form is
  generated alongside `template-application-ui-harmonia-java` output.

## Follow-ups
- Feed-driven widgets (combobox/select/radio with `feeds`), documents, table, stepIndicator.
- `FormIntentGenerator` neutral-code emission + task-form migration.
- A close/refresh handshake with the host dialog (`harmonia.form.close` postMessage is sent;
  the SPA shell could listen and close + refresh proactively).
