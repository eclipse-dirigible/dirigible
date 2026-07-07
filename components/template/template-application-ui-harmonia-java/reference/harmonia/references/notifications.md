# Notifications

The Notification component is used to present important system feedback, status updates, or contextual information to the user. Notifications communicate events that require awareness, confirmation, or action without interrupting the current workflow.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Directives

`x-h-notification-overlay` is the root. The directives compose one component and must be nested as shown in the Example below (the library throws at runtime when a required ancestor is missing):

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

::: info Named arguments
The `add` function uses object destructuring for its arguments. You will need to call it by passing a single object containing the arguments.
:::

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

::: info Named arguments
The `update` function uses object destructuring for its arguments.
:::

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

## Example

```html
<i data-lucide="bell" x-h-lucide></i>
```

More examples in the docs site: Notification Overlay, Notification Item, Notification Popover List, Add a notification, Notification with progress, Notification Position.

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
