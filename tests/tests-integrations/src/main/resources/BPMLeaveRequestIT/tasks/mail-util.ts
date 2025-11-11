import { configurations } from "@aerokit/sdk/core";
import { logging } from "@aerokit/sdk/log";
import { client as mailClient } from "@aerokit/sdk/mail";

const logger = logging.getLogger("mail-util.ts");

function isMailConfigured() {
    return configurations.get("MAIL_USERNAME", "user") &&
        configurations.get("MAIL_PASSWORD", "password") &&
        configurations.get("MAIL_TRANSPORT_PROTOCOL", "smtp") &&
        (
//             (configurations.get("MAIL_SMTPS_HOST", "localhost") && configurations.get("MAIL_SMTPS_PORT", PortUtil.getFreeRandomPort()) && configurations.get("MAIL_SMTPS_AUTH", "true"))
//             ||
            (configurations.get("MAIL_SMTP_HOST", "localhost") && configurations.get("MAIL_SMTP_PORT", "56565") && configurations.get("DIRIGIBLE_MAIL_SMTP_AUTH", "true"))
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
