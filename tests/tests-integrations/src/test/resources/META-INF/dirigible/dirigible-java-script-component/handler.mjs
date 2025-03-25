export function onMessage(message) {
    const body = message.getBody();

    const newBody = body.toUpperCase();
    message.setBody(newBody);

    return message;
}
