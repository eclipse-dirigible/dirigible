const MessagesHolder = Java.type("org.eclipse.dirigible.integration.tests.api.java.messaging.MessagesHolder");

export function onMessage(message) {
    console.log(`${new Date()}### BACKGROUND HANDLER ### - Received a message: [${message}]`);
    console.log(`${new Date()}### BACKGROUND HANDLER ### - Setting message: [${message}]`);
    MessagesHolder.setLatestReceivedMessage(message);
    console.error(`${new Date()}### Message set`);
}

export function onError(error) {
    console.error(`${new Date()}### BACKGROUND HANDLER ### - Received an error: [${error}]`);
    console.error(`${new Date()}### BACKGROUND HANDLER ### - Setting error: [${error}]`);
    MessagesHolder.setLatestReceivedError(error);
    console.error(`${new Date()}### Error set`);
}