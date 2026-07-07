# Breakpoint Listener

Creates a listener that triggers actions when the viewport reaches specified breakpoint. This function helps implement responsive behavior dynamically in the application.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## API

### Arguments

| Argument   | Type            | Required | Description                                                                                                                  |
| ---------- | --------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------- |
| handler    | function        | true     | Handler function that will be called when the breakpoint threshold has been met\*. Called immediately after initialization.  |
| breakpoint | integer\|string | false    | The desired breakpoint. If a number is given, it will be treated as pixels. By default, it's 768px.                          |
| frame      | boolean         | false    | By default, the listener will be attached to the topmost frame (window). This sets the current frame (iframe) as the target. |

::: info Threshold event
The handler function is invoked only when the window width crosses the specified breakpoint, either becoming narrower or wider, rather than on every resize event. A boolean value is passed as the first argument, indicating the direction of the change.
:::

### Returns

#### Object

| Property | Type     | Description                                                                |
| -------- | -------- | -------------------------------------------------------------------------- |
| remove   | function | Use this function to remove the event listener when it's no longer needed. |

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
