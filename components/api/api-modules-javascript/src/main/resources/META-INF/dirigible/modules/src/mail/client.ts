/**
 * Provides a client for sending emails, supporting both simple text/HTML
 * messages and complex multipart messages with attachments or inline content.
 */
const Properties = Java.type("java.util.Properties");
const MailFacade = Java.type("org.eclipse.dirigible.components.api.mail.MailFacade");

/**
 * Defines the structure for specifying email recipients.
 * Recipients can be provided as a single email string or an array of strings.
 */
export interface MailRecipients {
    /** The primary recipients of the email. */
    to?: string | string[];
    /** Carbon Copy recipients. */
    cc?: string | string[];
    /** Blind Carbon Copy recipients. */
    bcc?: string | string[];
}

/**
 * Defines a single part of a multipart email message.
 * This is used for bodies, attachments, and inline images.
 */
export interface MailMultipart {
    /** The nature of the part: 'text' (body), 'inline' (e.g., image displayed in HTML), or 'attachment'. */
    type: "text" | "inline" | "attachment";
    /** The MIME content type of the part (e.g., 'text/plain', 'text/html', 'image/png'). */
    contentType: string;
    /** The text content for 'text' parts. */
    text?: string;
    /** Required for 'inline' parts; used in the HTML body to reference the content (e.g., `<img src="cid:...">`). */
    contentId?: string;
    /** The name of the file for 'attachment' and 'inline' parts. */
    fileName?: string;
    /** The base64-encoded data content for 'attachment' and 'inline' parts. */
    data?: string;
}

/**
 * Defines the content type for simple emails sent via {@link MailClient.send}.
 */
type MailContentType = "html" | "plain";

/**
 * The MailClient provides methods for sending emails, handling recipient processing
 * and interfacing with the underlying MailFacade.
 */
export class MailClient {
    private readonly native: any;

    /**
     * A static convenience method to send a multipart email without instantiating a client.
     * This is suitable for emails that require attachments, inline images, or mixed content.
     *
     * @param from The sender's email address.
     * @param recipients The recipient(s) structure (string for 'to', or {@link MailRecipients} object).
     * @param subject The subject line of the email.
     * @param parts An array of {@link MailMultipart} objects defining the email content.
     */
    public static sendMultipart(from: string, recipients: string | MailRecipients, subject: string, parts: MailMultipart[]): void {
        const mailClient = new MailClient();
        mailClient.sendMultipart(from, recipients, subject, parts);
    }

    /**
     * A static convenience method to send a simple email with only a single text or HTML body.
     *
     * @param from The sender's email address.
     * @param recipients The recipient(s) structure (string for 'to', or {@link MailRecipients} object).
     * @param subject The subject line of the email.
     * @param text The body content of the email.
     * @param contentType Specifies the body format: 'html' or 'plain'.
     */
    public static send(from: string, recipients: string | MailRecipients, subject: string, text: string, contentType: MailContentType): void {
        const mailClient = new MailClient();
        mailClient.send(from, recipients, subject, text, contentType);
    }

    /**
     * Creates a new instance of the MailClient, optionally configuring the underlying
     * native mail facade.
     *
     * @param options Optional key-value object containing configuration properties for the mail client (e.g., SMTP settings).
     */
    constructor(options?: object) {
        this.native = options ? MailFacade.getInstance(toJavaProperties(options)) : MailFacade.getInstance();
    }

    /**
     * Sends a simple email with a single body part (text or HTML).
     *
     * @param from The sender's email address.
     * @param _recipients The recipient(s) structure (string for 'to', or {@link MailRecipients} object).
     * @param subject The subject line of the email.
     * @param text The body content of the email.
     * @param contentType Specifies the body format: 'html' or 'plain'.
     * @throws {Error} Throws an error if the recipient format is invalid or the native call fails.
     */
    public send(from: string, _recipients: string | MailRecipients, subject: string, text: string, contentType: MailContentType): void {
        const recipients = processRecipients(_recipients);

        const part = {
            contentType: contentType === "html" ? "text/html" : "text/plain",
            text: text,
            type: 'text'
        };

        try {
            this.native.send(from, recipients.to, recipients.cc, recipients.bcc, subject, [part]);
        } catch (error) {
            console.error(error.message);
            throw new Error(error as string);
        }
    }

    /**
     * Sends a complex email composed of multiple parts (text bodies, HTML, attachments, inline content).
     *
     * @param from The sender's email address.
     * @param _recipients The recipient(s) structure (string for 'to', or {@link MailRecipients} object).
     * @param subject The subject line of the email.
     * @param parts An array of {@link MailMultipart} objects defining the email content.
     * @throws {Error} Throws an error if the recipient format is invalid or the native call fails.
     */
    public sendMultipart(from: string, _recipients: string | MailRecipients, subject: string, parts: MailMultipart[]): void {
        let recipients = processRecipients(_recipients);
        try {
            // Note: The native facade expects base64 data to be JSON-stringified if present.
            return this.native.send(from, recipients.to, recipients.cc, recipients.bcc, subject, stringifyPartData(parts));
        } catch (error) {
            console.error(error.message);
            throw new Error(error as string);
        }
    }
}

/**
 * Prepares the multipart array for the native facade call by JSON-stringifying
 * the 'data' field of any part that has it (required for base64 data transport).
 *
 * @param parts The array of MailMultipart objects.
 * @returns The modified parts array ready for the native call.
 */
function stringifyPartData(parts: MailMultipart[]): MailMultipart[] {
    parts.forEach(function (part: any) {
        if (part.data) {
            part.data = JSON.stringify(part.data);
        }
        return part;
    })
    return parts;
}

/**
 * Processes the recipients input (either a single string or a {@link MailRecipients} object)
 * into separate 'to', 'cc', and 'bcc' arrays of strings.
 *
 * @param recipients The raw recipient data.
 * @returns An object containing `to`, `cc`, and `bcc` arrays.
 * @throws {Error} Throws an error if the input format is invalid.
 */
function processRecipients(recipients: string | MailRecipients) {
    let to = [];
    let cc = [];
    let bcc = [];
    if (typeof recipients === "string") {
        to.push(recipients);
    } else if (typeof recipients === "object") {
        to = parseRecipients(recipients, "to");
        cc = parseRecipients(recipients, "cc");
        bcc = parseRecipients(recipients, "bcc");
    } else {
        const errorMessage = "Invalid 'recipients' format: " + JSON.stringify(recipients);
        console.error(errorMessage);
        throw new Error(errorMessage);
    }

    return { to: to, cc: cc, bcc: bcc };
}

/**
 * Converts a JavaScript object of properties into a Java Properties object.
 *
 * @param properties The JavaScript object containing key-value configuration pairs.
 * @returns A Java Properties instance.
 */
function toJavaProperties(properties: any) {
    const javaProperties = new Properties();
    Object.keys(properties).forEach(function (e) {
        javaProperties.put(e, properties[e]);
    });
    return javaProperties;
}

/**
 * Helper function to safely extract and validate recipients from a specific field ('to', 'cc', 'bcc').
 *
 * @param recipients The MailRecipients object.
 * @param type The field name to parse ('to', 'cc', or 'bcc').
 * @returns An array of recipient email strings.
 * @throws {Error} Throws an error if the field value is not a string, array, or undefined.
 */
function parseRecipients(recipients: MailRecipients, type: keyof MailRecipients): string[] {
    const objectType = typeof recipients[type];
    if (objectType === "string") {
        return [recipients[type] as string];
    } else if (Array.isArray(recipients[type])) {
        return recipients[type] as string[];
    } else if (objectType === "undefined") {
        return [];
    }
    const errorMessage = `Invalid 'recipients.${type}' format: [${recipients[type]}|${objectType}]`;
    console.error(errorMessage);
    throw new Error(errorMessage);
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = MailClient;
}