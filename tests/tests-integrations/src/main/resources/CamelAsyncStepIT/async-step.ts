import { getName } from "./async-functions";

export async function onMessage(message: any) {
    console.log("Calling asyncFunction...");

    const name = await getName();

    message.setBody("My name is: " + name);

    return message;
}