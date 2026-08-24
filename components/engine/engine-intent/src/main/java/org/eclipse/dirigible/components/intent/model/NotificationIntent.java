/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A declarative notification: send a message when an entity event fires. The first concrete pattern
 * of the declarative-glue catalog (see the engine guide's "Planned: declarative glue").
 *
 * <p>
 * It is a self-contained reaction (event -> send). {@link #event} is a free-form map carrying
 * exactly one of {@code onCreate} / {@code onUpdate} / {@code onDelete} naming a declared entity,
 * plus an optional {@code when} guard expression. (The key is {@code event}, not {@code on} - YAML
 * 1.1 resolves a bare {@code on} to the boolean {@code true}.) The generator emits an annotated
 * client-Java {@code @Listener} that binds to the entity's event topic and sends via the SDK;
 * {@link #to} is a direct field of the event entity or a literal address.
 *
 * <p>
 * The same shape is the reusable <b>notify block</b>: it is embedded in a
 * {@code schedules[].notify}, a {@code transitions[].notify} and a {@code serviceTask}'s
 * {@code args.notify} - the three places an intent can act - where {@link #name} / {@link #event}
 * are unused (the call site IS the event) and {@link #attach} may name the document to send along.
 * {@code attach: print} renders the record's {@code .print} template to PDF server-side and
 * attaches it, which is how a business document (an invoice to its customer, a payslip to its
 * employee) is mailed declaratively. Its report-side sibling {@code attach: { report: <name>, bind:
 * { <parameter>: <field> } }} renders a declared REPORT instead, with the recipient row binding the
 * report's parameters - the customer statement, where the mailed artifact is a period's rows rather
 * than one record's own document.
 */
public class NotificationIntent {

    private String name;
    private Map<String, Object> event = new LinkedHashMap<>();
    private String channel = "email";
    private String to;
    private String subject;
    private String body;
    /**
     * Optional document to attach, in one of two authored shapes - hence {@link Object} rather than a
     * String, and hence {@link #getAttach()} reporting only the KIND:
     *
     * <ul>
     * <li>a scalar: {@code print} renders the record's own {@code .print} template to PDF (through the
     * entity's generated print feeder), {@code recordPrint} a fan-out anchor's;</li>
     * <li>a map: {@code { report: <name>, bind: { <parameter>: <field> } }} renders a declared report,
     * each bound parameter resolved against the record the message is about.</li>
     * </ul>
     *
     * Blank = a plain-text message.
     */
    private Object attach;
    /**
     * The fixed print template language for {@link #attach} (a {@code languages:} code). Mutually
     * exclusive with {@link #languageFrom}; absent both, the render falls back to the first entry of
     * the tenant-resolved application language set at run time.
     */
    private String language;
    /**
     * A one-hop {@code relation.field} path on the entity the message is about that determines the
     * {@link #attach} render language per record (e.g. {@code languageFrom: customer.language}).
     * Mutually exclusive with {@link #language}; a blank resolved value falls back like an absent knob.
     */
    private String languageFrom;
    /**
     * Optional <b>fan-out</b>: name a related entity and the block sends ONE message PER ROW of it
     * instead of one about the record - the payroll run that mails every payslip to its own employee,
     * the request for quotation that goes out to each invited supplier. The named entity must have a
     * to-one relation back to the record; every path ({@link #to}, and the placeholders in
     * {@link #subject} / {@link #body}) then resolves against the ROW, and {@link #attach} attaches the
     * ROW's own document.
     */
    private String forEach;
    /**
     * The name the {@link #attach}ed PDF arrives under - a pattern of literals and {@code {token}}
     * interpolations over the rendered record ({@code {Number}_{Date:yyyyMMdd}_{Customer.Name}}), with
     * {@code .pdf} appended. Absent, the name is the document's own number, or the entity name plus the
     * record id when it has none. A real archive wants a self-describing name; the fixed one is only
     * the default.
     */
    private String fileName;

    /**
     * The keys {@link #fromMap} reads - the whole vocabulary of an embedded notify block. Published so
     * the parser can reject anything else instead of dropping it silently;
     * {@code NotificationIntentTest} pins the two together, since a key added to {@code fromMap} and
     * not to this set would go straight back to being unauthorable.
     */
    public static final Set<String> BLOCK_KEYS =
            Set.of("to", "subject", "body", "attach", "language", "languageFrom", "fileName", "forEach", "channel");

    /**
     * The kind {@link #getAttach()} reports for the map shape {@code attach: { report, bind }}. It is
     * not an authorable scalar - {@code attach: report} names no report and binds nothing - so it can
     * never collide with one.
     */
    public static final String ATTACH_REPORT = "report";

    /** The keys the report shape reads, published so the parser can reject anything else. */
    public static final Set<String> ATTACH_REPORT_KEYS = Set.of("report", "bind");

    /**
     * Read an <b>embedded</b> notify block off a free-form map - a process step's {@code args.notify},
     * whose args are untyped by design. {@code name} / {@code event} stay unset: the call site is the
     * event.
     *
     * @param raw the map, or any other value (including {@code null}) for "no notify block"
     * @return the notify block, or {@code null} when {@code raw} is not a map
     */
    public static NotificationIntent fromMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        NotificationIntent notify = new NotificationIntent();
        notify.setTo(string(map.get("to")));
        notify.setSubject(string(map.get("subject")));
        notify.setBody(string(map.get("body")));
        notify.setAttach(map.get("attach"));
        notify.setLanguage(string(map.get("language")));
        notify.setLanguageFrom(string(map.get("languageFrom")));
        notify.setFileName(string(map.get("fileName")));
        notify.setForEach(string(map.get("forEach")));
        String channel = string(map.get("channel"));
        if (channel != null) {
            notify.setChannel(channel);
        }
        return notify;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event == null ? new LinkedHashMap<>() : event;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * The attachment KIND, which is what every reader of an attachment branches on: the authored scalar
     * ({@code print} / {@code recordPrint}) as written, {@link #ATTACH_REPORT} for the map shape (even
     * a malformed one - {@link #getReportAttachment()} and the parser report what is wrong with it),
     * and {@code null} for a plain-text message.
     *
     * @return the attachment kind, or {@code null} when nothing is attached
     */
    public String getAttach() {
        if (attach instanceof Map<?, ?>) {
            return ATTACH_REPORT;
        }
        return attach == null ? null : attach.toString();
    }

    public void setAttach(Object attach) {
        this.attach = attach;
    }

    /**
     * The report attachment the map shape declares.
     *
     * @return the report name and its bindings, or {@code null} when {@code attach} is not the map
     *         shape
     */
    public ReportAttachment getReportAttachment() {
        if (!(attach instanceof Map<?, ?> map)) {
            return null;
        }
        String report = string(map.get("report"));
        Map<String, String> bind = new LinkedHashMap<>();
        if (map.get("bind") instanceof Map<?, ?> bound) {
            for (Map.Entry<?, ?> entry : bound.entrySet()) {
                bind.put(String.valueOf(entry.getKey()), string(entry.getValue()));
            }
        }
        return new ReportAttachment(report == null ? null : report.trim(), bind);
    }

    /**
     * A report attachment: the declared report to render, and how the record the message is about binds
     * its parameters.
     *
     * @param report the declared report's name, {@code null} when the map shape named none
     * @param bind parameter name -> a field or one-hop {@code relation.field} path of the record, in
     *        authored order
     */
    public record ReportAttachment(String report, Map<String, String> bind) {
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageFrom() {
        return languageFrom;
    }

    public void setLanguageFrom(String languageFrom) {
        this.languageFrom = languageFrom;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getForEach() {
        return forEach;
    }

    public void setForEach(String forEach) {
        this.forEach = forEach;
    }
}
