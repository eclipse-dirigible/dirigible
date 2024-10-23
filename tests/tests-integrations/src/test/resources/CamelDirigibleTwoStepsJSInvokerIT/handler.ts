import { logging } from "sdk/log";

const logger = logging.getLogger("TwoStepsHandler");

export function onMessage(message: any) {
    const body: string = message.getBody();

    logger.info("Received body [{}]", body);

    const newBody = body.toUpperCase();
    message.setBody(newBody);

    return message;
}