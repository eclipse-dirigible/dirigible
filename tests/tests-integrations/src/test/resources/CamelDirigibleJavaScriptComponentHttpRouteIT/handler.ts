export function onMessage(message: any) {
    message.setBody("Body set by the handler");

    return message;
}
