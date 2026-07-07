# Notifications

The Notification component is used to present important system feedback, status updates, or contextual information to the user. Notifications communicate events that require awareness, confirmation, or action without interrupting the current workflow.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use Notifications to inform users about the result of an action, system state changes, or important contextual information.

- Keep messages concise and actionable.
- Prioritize clarity over technical detail.
- Match the notification style to the severity and urgency of the message.
- Use consistent wording and visual patterns across the product.
- Include actions only when users can meaningfully respond.
- Avoid showing multiple notifications simultaneously unless necessary.

## Directives

`x-h-notification-overlay` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-notification-overlay`
- `x-h-notification-list`
- `x-h-notification`
- `x-h-notification-media`
- `x-h-notification-title`
- `x-h-notification-description`
- `x-h-notification-actions`
- `x-h-notification-close`

## API

The notification component contains a [magic method](https://alpinejs.dev/globals/alpine-data#using-magic-properties) called `$notifications`.
It is used to add, update and remove notifications from the global state.

### Magic properties

| Property       | Type     | Description                      |
| -------------- | -------- | -------------------------------- |
| add            | function | Adds a notification.             |
| update         | function | Updates a notification.          |
| remove         | function | Removes a notification.          |
| addListener    | function | Adds a notification listener.    |
| removeListener | function | Removes a notification listener. |

#### Arguments

- `add` function

> **Note:** Named arguments
> The `add` function uses object destructuring for its arguments. You will need to call it by passing a single object containing the arguments.

| Argument | Type                                                                                                      | Required | Description                                                                                                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id       | string                                                                                                    | false    | Unique notification ID.                                                                                                                                                                                   |
| template | string                                                                                                    | true     | Template ID.                                                                                                                                                                                              |
| position | `top-left`<br />`top-center`<br />`top-right`<br />`bottom-left`<br />`bottom-center`<br />`bottom-right` | false    | Notification position. On medium-size screens, center notifications will be reassigned to the right top/bottom position. On mobile screens, they will be reassigned to the top and bottom center position |
| timeout  | number                                                                                                    | false    | Miliseconds before removing the notification. If the notification should not be removed, use a value of `0`.                                                                                              |
| data     | object                                                                                                    | false    | Contains all the properties that will be used to render the notification template.                                                                                                                        |

```js
this.$notifications.add({
  id: 'fileDeleted',
  template: 'basic',
  position: 'bottom-center',
  data: {
    title: 'Delete operation',
    message: 'File X was deleted',
  },
});
```

- `update` function

> **Note:** Named arguments
> The `update` function uses object destructuring for its arguments.

| Argument | Type   | Required | Description                                                                                                                  |
| -------- | ------ | -------- | ---------------------------------------------------------------------------------------------------------------------------- |
| id       | string | true     | Notification ID.                                                                                                             |
| data     | object | true     | Contains the updated properties. They are merged with the existing data, so only the changed properties need to be provided. |

```js
this.$notifications.update({
  id: 'fileDeleted',
  data: {
    message: 'File X was restored',
  },
});
```

- `remove` function

| Argument | Type   | Required | Description      |
| -------- | ------ | -------- | ---------------- |
| id       | string | true     | Notification ID. |

```js
this.$notifications.remove('fileDeleted');
```

- `addListener` function

| Argument   | Returns                   | Type   | Required | Description                                                                                |
| ---------- | ------------------------- | ------ | -------- | ------------------------------------------------------------------------------------------ |
| `listener` | Listener reference object | object | true     | Contains callbacks for one or more of the three events - `added`, `updated` and `removed`. |

- `listener` object functions

| function | Arguments    | Required | Description                                                                                                                   |
| -------- | ------------ | -------- | ----------------------------------------------------------------------------------------------------------------------------- |
| added    | `item`       | false    | Called when a notification has been added. First argument is the notification object.                                         |
| updated  | `id`, `data` | false    | Called when a notification has been updated. First argument is the notification ID and the second is the updated data object. |
| removed  | `id`         | false    | Called when a notification has been removed. First argument is the notification ID.                                           |

```js
const listenerRef = this.$notifications.addListener({
  added(item) {
    console.log(item);
  },
  updated(id, data) {
    console.log(id, data);
  },
  removed(id) {
    console.log(id);
  },
});
```

- `removeListener` function

| Argument           | Returns | Type   | Required | Description                                                            |
| ------------------ | ------- | ------ | -------- | ---------------------------------------------------------------------- |
| Listener reference | void    | object | true     | The listener reference that is returned by the `addListener` function. |

```js
this.$notifications.removeListener(listenerRef);
```

### Attributes

#### x-h-notification

| Attribute    | Values  | Required | Description                      |
| ------------ | ------- | -------- | -------------------------------- |
| data-unread  | boolean | false    | Marks the notification as unread |
| data-variant | `toast` | false    | Toast style notification         |

#### x-h-notification-actions

| Attribute        | Values                      | Required | Description                          |
| ---------------- | --------------------------- | -------- | ------------------------------------ |
| data-orientation | `vertical`<br/>`horizontal` | false    | Changes the aligment of the actions. |

### Modifiers

#### x-h-notification

| Modifier | Type    | Required | Description                                          |
| -------- | ------- | -------- | ---------------------------------------------------- |
| floating | boolean | false    | Adds rounded corners and shadow to the notification. |

## Examples

### Notification Overlay

In order to show a notification, you must place a `x-h-notification-overlay` component inside your application.

It is recommended to place it inside the body tag or the topmost element initialized with Alpine.

You can define multiple notification templates. Templates are not monitored for changes, so once the overlay initializes, any changes or new template additions will not be applied.

```html
<section x-h-notification-overlay>
  <template id="basic">
    <li x-h-notification.floating>
      <div x-h-notification-title x-text="title"></div>
    </li>
  </template>
  <template id="progress"> ... </template>
  ...
</section>
```

- Lucide icons

The recommended way to render Lucide icons inside notifications is the Lucide plugin: add `x-h-lucide` to each icon placeholder and it renders automatically as each notification is shown, with no extra configuration.

```html
<i data-lucide="bell" x-h-lucide></i>
```

If you do not use the plugin, then you must enable template icon replacement in the `createIcons` configuration object, since the icons live inside a `<template>`.

```js
lucide.createIcons({ inTemplates: true });
```

### Notification Item

The notification item has four inner components but they have no strict structure. This gives you the flexibility to design your own notification.

- #### Toast-like notification

```html
<li x-h-notification.floating data-variant="toast">
  <div x-h-notification-title>Toast</div>
</li>
```

- #### Basic notification with icon

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-media>
    <svg x-h-icon data-icon="circle-info" role="img" aria-label="info"></svg>
  </div>
  <div x-h-notification-title>Notification</div>
</li>
```

- #### Closable notification with icon, title and description

```html
<li x-h-notification.floating class="hbox">
  <div x-h-notification-media>
    <svg x-h-icon data-icon="circle-info" class="size-6" role="img" aria-label="info"></svg>
  </div>
  <div class="vbox flex-1">
    <h1 x-h-notification-title>Notification</h1>
    <p x-h-notification-description>Lorem ipsum dolor sit amet, consectetur adipiscing elit...</p>
  </div>
  <div x-h-notification-actions data-orientation="vertical">
    <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
      <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
    </button>
  </div>
</li>
```

- #### Closable notification with avatar, title and description

```html
<li x-h-notification.floating class="hbox">
  <div x-h-notification-media>
    <div x-h-avatar>
      <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
      <div x-h-avatar-fallback>HM</div>
    </div>
  </div>
  <div class="vbox flex-1">
    <h1 x-h-notification-title>Notification</h1>
    <p x-h-notification-description>Lorem ipsum dolor sit amet, consectetur adipiscing elit...</p>
  </div>
  <div x-h-notification-actions data-orientation="vertical">
    <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
      <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
    </button>
  </div>
</li>
```

- #### File deleted notification with undo button

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-title>File deleted</div>
  <div x-h-notification-actions data-orientation="horizontal" data-align="center">
    <button x-h-button data-size="sm" data-variant="outline">Undo</button>
  </div>
</li>
```

- #### Progress notification with cancel button

```html
<li x-h-notification.floating class="hbox">
  <div x-h-notification-actions data-orientation="vertical">
    <button x-h-button class="rounded-full" data-size="icon" data-variant="outline" aria-label="cancel">
      <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
    </button>
  </div>
  <div class="vbox flex-1 gap-2">
    <h1 x-h-notification-title>Generating project...</h1>
    <div x-h-progress="40"></div>
  </div>
</li>
```

- #### Upload notification with actions

```html
<li x-h-notification.floating class="vbox gap-2">
  <div x-h-notification-title class="hbox items-center gap-2">
    <svg x-h-icon data-icon="import" class="size-6" role="img" aria-label="import"></svg>
    <span class="text-lg">Uploading in progress</span>
  </div>
  <p x-h-notification-description>Please wait while your file is being uploaded.<br />This may take a moment.</p>
  <div x-h-progress="40"></div>
  <div x-h-notification-actions data-orientation="horizontal">
    <button x-h-button class="flex-1" data-variant="outline">Cancel upload</button>
    <button x-h-button class="flex-1" data-variant="primary">Show all uploads</button>
  </div>
</li>
```

- #### Chat notification with avatar and reply button

```html
<li x-h-notification.floating class="hbox">
  <div x-h-notification-media class="justify-start">
    <div x-h-avatar class="size-10">
      <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
      <div x-h-avatar-fallback>HM</div>
    </div>
  </div>
  <div class="vbox flex-1 gap-1">
    <h1 x-h-notification-title>Harmonia Doe</h1>
    <p x-h-notification-description>I just received the document. It looks good.</p>
    <div x-h-notification-actions class="pt-2" data-orientation="horizontal">
      <button x-h-button data-variant="primary" data-size="sm">
        <svg x-h-icon data-icon="reply" role="img" aria-label="reply"></svg>
        Reply
      </button>
    </div>
  </div>
  <div x-h-notification-actions data-orientation="vertical" class="justify-start">
    <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
      <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
    </button>
  </div>
</li>
```

- #### Information variant notification

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-media>
    <div x-h-avatar class="rounded-lg" data-variant="information">
      <svg x-h-icon data-icon="circle-info" role="img" aria-label="info"></svg>
    </div>
  </div>
  <div x-h-notification-title>Information</div>
</li>
```

- #### Warning variant notification

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-media>
    <div x-h-avatar class="rounded-lg" data-variant="warning">
      <svg x-h-icon data-icon="circle-warning" role="img" aria-label="warning"></svg>
    </div>
  </div>
  <div x-h-notification-title>Warning</div>
</li>
```

- #### Success variant notification

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-media>
    <div x-h-avatar class="rounded-lg" data-variant="positive">
      <svg x-h-icon data-icon="circle-success" role="img" aria-label="success"></svg>
    </div>
  </div>
  <div x-h-notification-title>Success</div>
</li>
```

- #### Error variant notification

```html
<li x-h-notification.floating class="hbox items-center">
  <div x-h-notification-media>
    <div x-h-avatar class="rounded-lg" data-variant="negative">
      <svg x-h-icon data-icon="circle-error" role="img" aria-label="error"></svg>
    </div>
  </div>
  <div x-h-notification-title>Error</div>
</li>
```

### Notification Popover List

You can use the notification list anywhere but the most common place is usually inside a popover in a toolbar.

```html
<header x-h-toolbar>
  <img x-h-toolbar-image src="/harmonia/logo/harmonia.svg" alt="@harmonia" />
  <h1 x-h-toolbar-title>Harmonia</h1>
  <div x-h-toolbar-spacer></div>
  <button x-h-button x-h-popover-trigger data-variant="transparent" data-size="icon" aria-label="Notifications">
    <i x-h-lucide role="img" data-lucide="bell"></i>
  </button>
  <div x-h-popover data-innerclicks="true" class="w-full" data-max-w="lg">
    <div x-h-toolbar data-size="md">
      <span x-h-toolbar-title>Notifications (3)</span>
      <div x-h-toolbar-spacer></div>
      <button x-h-button data-size="md" data-variant="transparent"><svg x-h-icon data-icon="trash" role="img" aria-label="trash"></svg>Clear</button>
    </div>
    <ol x-h-notification-list>
      <li x-h-notification class="hbox" data-unread="true">
        <div x-h-notification-media class="justify-start">
          <div x-h-avatar class="size-10">
            <img x-h-avatar-image src="/harmonia/logo/harmonia-square.svg" alt="@harmonia" />
            <div x-h-avatar-fallback>HM</div>
          </div>
        </div>
        <div class="vbox flex-1 gap-1">
          <h1 x-h-notification-title>Harmonia Doe</h1>
          <p x-h-notification-description>I just received the document. It looks good.</p>
          <div x-h-notification-actions class="pt-2" data-orientation="horizontal">
            <button x-h-button data-variant="primary" data-size="sm">
              <svg x-h-icon data-icon="reply" role="img" aria-label="reply"></svg>
              Reply
            </button>
          </div>
        </div>
        <div x-h-notification-actions data-orientation="vertical" class="justify-start">
          <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
            <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
          </button>
        </div>
      </li>
      <li x-h-notification class="hbox items-center">
        <div x-h-notification-media class="w-10">
          <div x-h-avatar class="rounded-lg" data-variant="negative">
            <svg x-h-icon data-icon="circle-error" role="img" aria-label="error"></svg>
          </div>
        </div>
        <div class="vbox flex-1">
          <h1 x-h-notification-title>Generation failed</h1>
          <p x-h-notification-description>Failed to generate a monthly report. Check console log.</p>
        </div>
        <div x-h-notification-actions data-orientation="vertical">
          <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
            <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
          </button>
        </div>
      </li>
      <li x-h-notification class="hbox items-center">
        <div x-h-notification-media class="w-10">
          <svg x-h-icon data-icon="circle-info" class="size-6" role="img" aria-label="info"></svg>
        </div>
        <div x-h-notification-title class="flex-1">Monthly report generation started</div>
        <div x-h-notification-actions data-orientation="vertical">
          <button x-h-button data-variant="transparent" data-size="icon-sm" aria-label="close notification">
            <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
          </button>
        </div>
      </li>
    </ol>
  </div>
</header>
```

### Add a notification

The `$notifications` magic can be used both from inline HTML and from inside a component object.

**Inline**

```html
<button x-h-button @click="$notifications.add({ template: 'basic', data: { title: 'You have been notified.' } })">Notify</button>
```

**Component Object**

```html
<button x-h-button @click="notify()">Notify</button>

<script>
  Alpine.data('notifyController', () => ({
    notify() {
      this.$notifications.add({
        template: 'basic',
        data: {
          title: 'You have been notified.',
        },
      });
    },
  }));
</script>
```

### Notification with progress

Notifications can contain actions and dynamic information. The following example simulates a generation progress notification.

```html
<div x-data="notifyProgress">
  <section x-h-notification-overlay>
    <template id="progress">
      <li x-h-notification.floating class="hbox min-w-xs">
        <template x-if="progress >= 100">
          <div x-h-notification-media>
            <div x-h-avatar class="size-9" data-variant="information">
              <svg x-h-icon data-icon="circle-info" role="img" aria-label="info"></svg>
            </div>
          </div>
        </template>
        <template x-if="progress < 100">
          <div x-h-notification-actions data-orientation="vertical">
            <button x-h-button x-h-notification-close class="rounded-full" data-size="icon" data-variant="outline" aria-label="cancel">
              <svg x-h-icon data-icon="close" role="img" aria-label="close"></svg>
            </button>
          </div>
        </template>
        <div class="vbox flex-1 gap-2">
          <h1 x-h-notification-title x-text="title"></h1>
          <div x-h-progress="progress"></div>
        </div>
      </li>
    </template>
  </section>

  <button x-h-button @click="showNotification">Start</button>
</div>
<script>
  Alpine.data('notifyProgress', () => ({
    progress: 0,
    intervalID: undefined,
    showNotification() {
      // If there is an intervalID, that means the notification is already shown
      if (!this.intervalID) {
        // Reset the progress
        this.progress = 10;

        // Add the notification and set the timeout to 0, so it wouldn't automatically remove itself
        this.$notifications.add({
          id: 'progress-demo',
          template: 'progress',
          data: {
            title: 'Generating...',
            progress: this.progress,
          },
          timeout: 0,
        });

        // Notification listeners do not have the access to the same 'this' object as the controller.
        // We can work around this by creating a function and calling it instead.
        const clearNotification = () => {
          clearInterval(this.intervalID);
          this.intervalID = undefined;
          // Remove the notification listener
          this.$notifications.removeListener(listenerRef);
        };

        // Add a notification listener that will watch for removal changes.
        // If the user has canceled the notification, cancel the interval and progress update.
        const listenerRef = this.$notifications.addListener({
          removed(id) {
            if (id === 'progress-demo') {
              clearNotification();
            }
          },
        });

        // Simulate work with setInterval
        this.intervalID = setInterval(() => {
          this.progress += 10;
          if (this.progress >= 100) {
            this.$notifications.update({
              id: 'progress-demo',
              data: {
                title: 'Generated!',
                progress: 100,
              },
            });
            clearNotification();
            // Remove the notification
            setTimeout(() => {
              this.$notifications.remove('progress-demo');
            }, 3000);
          } else {
            this.$notifications.update({
              id: 'progress-demo',
              data: {
                progress: this.progress,
              },
            });
          }
        }, 1000);
      }
    },
  }));
</script>
```

### Notification Position

```html
<div class="size-full" x-data="notifyPosition">
  <section x-h-notification-overlay>
    <template id="basic">
      <li x-h-notification.floating>
        <div x-h-notification-title x-text="title"></div>
      </li>
    </template>
  </section>

  <div class="grid size-full grid-cols-3 place-content-center gap-4">
    <button x-h-button @click="notify('top-left')">Top Left</button>
    <button x-h-button @click="notify('top-center')">Top Center</button>
    <button x-h-button @click="notify('top-right')">Top Right</button>
    <button x-h-button @click="notify('bottom-left')">Bottom Left</button>
    <button x-h-button @click="notify('bottom-center')">Bottom Center</button>
    <button x-h-button @click="notify('bottom-right')">Bottom Right</button>
  </div>
</div>
<script>
  Alpine.data('notifyPosition', () => ({
    notify(pos) {
      this.$notifications.add({ template: 'basic', position: pos, data: { title: pos }, timeout: 3000 });
    },
  }));
</script>
```

Full docs: https://www.codbex.com/harmonia/components/notifications.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
