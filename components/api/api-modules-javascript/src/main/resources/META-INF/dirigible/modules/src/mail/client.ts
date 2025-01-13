/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const Properties = Java.type("java.util.Properties");
const MailFacade = Java.type("org.eclipse.dirigible.components.api.mail.MailFacade");

export interface MailRecipients {
    to?: string | string[];
    cc?: string | string[];
    bcc?: string | string[];
}

export interface MailMultipart {
    type: "text" | "inline" | "attachment";
    contentType: "text/plain" | "text/html";
    text?: string;
    contentId?: string;
    fileName?: string;
    data?: string;
}

type MailContentType = "html" | "plain";

export class MailClient {
    private readonly native: any;

    public static sendMultipart(from: string, recipients: string | MailRecipients, subject: string, parts: MailMultipart[]): void {
        const mailClient = new MailClient();
        mailClient.sendMultipart(from, recipients, subject, parts);
    }

    public static send(from: string, recipients: string | MailRecipients, subject: string, text: string, contentType: MailContentType): void {
        const mailClient = new MailClient();
        mailClient.send(from, recipients, subject, text, contentType);
    }

    constructor(options?: object) {
        this.native = options ? MailFacade.getInstance(toJavaProperties(options)) : MailFacade.getInstance();
    }

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
            throw new Error(error);
        }
    }

    public sendMultipart(from: string, _recipients: string | MailRecipients, subject: string, parts: {}): void {
        let recipients = processRecipients(_recipients);
        try {
            return this.native.send(from, recipients.to, recipients.cc, recipients.bcc, subject, stringifyPartData(parts));
        } catch (error) {
            console.error(error.message);
            throw new Error(error);
        }
    }
}

function stringifyPartData(parts: any): JSON {
    parts.forEach(function (part: any) {
        if (part.data) {
            part.data = JSON.stringify(part.data);
        }
        return part;
    })
    return parts;
}

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

function toJavaProperties(properties: any) {
    const javaProperties = new Properties();
    Object.keys(properties).forEach(function (e) {
        javaProperties.put(e, properties[e]);
    });
    return javaProperties;
}

function parseRecipients(recipients: string | MailRecipients, type: any): any {
    const objectType = typeof recipients[type];
    if (objectType === "string") {
        return [recipients[type]];
    } else if (Array.isArray(recipients[type])) {
        return recipients[type];
    } else if (objectType === "undefined") {
        return [];
    }
    const errorMessage = "Invalid 'recipients." + type + "' format: [" + recipients[type] + "|" + objectType + "]";
    console.error(errorMessage);
    throw new Error(errorMessage);
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = MailClient;
}