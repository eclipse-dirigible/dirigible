export function onMessage(message: any) {
    message.setBody("This is a body set by the handler");

    return message;
}
