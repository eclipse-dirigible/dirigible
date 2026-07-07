# Dialog

A modal container that appears in response to a user action, temporarily interrupting the current workflow to request information or confirmation. Dialogs require users to make a decision before they can continue interacting with the underlying interface.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use dialogs to capture critical decisions, confirmations, or inputs that demand immediate attention. Avoid overusing dialogs for non-essential interactions, as frequent interruptions can disrupt the user experience.

## Directives

`x-h-dialog-overlay` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-dialog-overlay`
- `x-h-dialog`
- `x-h-dialog-header`
- `x-h-dialog-title`
- `x-h-dialog-close`
- `x-h-dialog-description`
- `x-h-dialog-footer`

## Examples

```html
<div x-data="{ showDialog: false }">
  <button x-h-button @click="showDialog = !showDialog">Show</button>

  <div x-h-dialog-overlay :data-open="showDialog">
    <div x-h-dialog>
      <div x-h-dialog-header>
        <h2 x-h-dialog-title>Edit profile</h2>
        <p x-h-dialog-description>Make changes to your profile and click save.</p>
        <button x-h-dialog-close aria-label="Icon button" @click="showDialog = false">
          <i x-h-lucide role="img" data-lucide="x"></i>
        </button>
      </div>
      <div class="grid gap-4">
        <div class="grid gap-3">
          <label x-h-label for="name-1">Name</label>
          <input x-h-input id="name-1" name="name" value="Olivia Davis" />
        </div>
        <div class="grid gap-3">
          <label x-h-label for="username-1">Username</label>
          <input x-h-input id="username-1" name="username" value="@olivia-davis" />
        </div>
      </div>
      <div x-h-dialog-footer>
        <button x-h-button data-variant="outline" @click="showDialog = false">Cancel</button>
        <button x-h-button data-variant="primary" @click="showDialog = false">Save</button>
      </div>
    </div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/dialog.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
