import { configurations } from "sdk/core";
import { logging } from "sdk/log";
import { client as mailClient } from "sdk/mail";

const logger = logging.getLogger("mail-util.ts");

function isMailConfigured() {
    return configurations.get("DIRIGIBLE_MAIL_USERNAME") &&
        configurations.get("DIRIGIBLE_MAIL_PASSWORD") &&
        configurations.get("DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL") &&
        (
            (configurations.get("DIRIGIBLE_MAIL_SMTPS_HOST") && configurations.get("DIRIGIBLE_MAIL_SMTPS_PORT") && configurations.get("DIRIGIBLE_MAIL_SMTPS_AUTH"))
            ||
            (configurations.get("DIRIGIBLE_MAIL_SMTP_HOST") && configurations.get("DIRIGIBLE_MAIL_SMTP_PORT") && configurations.get("DIRIGIBLE_MAIL_SMTP_AUTH"))
        );
}

export function sendMail(to: string, subject: string, content: string) {
    const from = configurations.get("LEAVE_REQUEST_APP_FROM_EMAIL", "leave-request-app@example.com");

    if (isMailConfigured()) {
        logger.info("Sending mail to [{}] with subject [{}] and content: [{}]...", to, subject, content);
        mailClient.send(from, to, subject, content, 'html');
    } else {
        logger.info("Mail to [{}] with subject [{}] and content [{}] will NOT be send because the mail client is not configured.", to, subject, content);
    }

}