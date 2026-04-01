/**
 * @module mail/index
 * @package @aerokit/sdk/mail
 * @overview
 * 
 * This module provides functionalities for email handling within the Aerokit SDK. It includes classes and methods for sending emails, managing email templates, and configuring email settings.
 * 
 * The main components of this module are:
 * - MailClient: Represents a client for sending emails and provides methods to configure email settings and send email messages.
 */

export * from "./client";
export { MailClient as client } from "./client";
