/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A save refused by a business rule that names no field of the form (an intent {@code checks:}
 * rule, answered as a plain 400) shows the server's own sentence, not the generic "Please review
 * your input" line (issue #8342). The text was authored for the person who pressed Save; the
 * catalog line told them nothing to fix.
 *
 * <p>
 * {@code baseFormPage.applyApiError} is exercised by evaluating the shipped {@code apiError.js},
 * {@code basePage.js} and {@code baseFormPage.js} in a polyglot context; the generated write paths
 * that format their own errors (line dialog, line and record deletes) are checked to route through
 * the same {@code refusalMessageFor}.
 */
class HarmoniaRefusalMessageIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    private static final String UI = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    private static final String REFUSAL = "A delivery note is made from a posted goods issue - pick the goods issue";

    @Test
    void aRefusalNamingNoFieldShowsTheServersSentence() {
        try (Context context = load()) {
            apply(context, 400, "BadRequest", REFUSAL);
            assertEquals(REFUSAL, saveFailureMessage(context));
            assertEquals("save-failure", context.eval("js", "page.state")
                                                .asString());
        }
    }

    @Test
    void aConflictRefusalShowsTheServersSentence() {
        try (Context context = load()) {
            apply(context, 409, "Conflict", "SalesInvoice is allowed only from status [1] - current status is [3]");
            assertEquals("SalesInvoice is allowed only from status [1] - current status is [3]", saveFailureMessage(context));
        }
    }

    @Test
    void aFaultKeepsTheCatalogLine() {
        try (Context context = load()) {
            apply(context, 500, "InternalServerError", "could not execute statement; SQL [insert into DELIVERY_NOTE ...]");
            assertEquals("Something went wrong on our end. Please try again.", saveFailureMessage(context),
                    "a 500's raw exception text must never reach the screen");
        }
    }

    @Test
    void aRefusalNamingAFieldStillMarksThatField() {
        try (Context context = load()) {
            apply(context, 400, "BadRequest", "The 'Date' property is required");
            assertEquals("", saveFailureMessage(context), "a per-field refusal goes onto the field, not the banner");
            assertEquals("The 'Date' property is required", context.eval("js", "page.errors.Date")
                                                                   .asString());
        }
    }

    /**
     * #8596: a refusal the server resolved for the reader's language quotes the field's translated
     * label, not its name - the body's {@code field} is what marks it, and the sentence is shown as the
     * server wrote it.
     */
    @Test
    void aTranslatedRefusalMarksTheFieldTheBodyNames() {
        try (Context context = load()) {
            applyWithField(context, 409, "Conflict", "Вече съществува доставка с този 'Номер за проследяване'", "TrackingNumber");
            assertEquals("", saveFailureMessage(context), "a per-field refusal goes onto the field, not the banner");
            assertEquals("Вече съществува доставка с този 'Номер за проследяване'", context.eval("js", "page.errors.TrackingNumber")
                                                                                           .asString());
        }
    }

    /**
     * #8597: a {@code checks:} rule about one field marks it, though its authored sentence quotes none.
     */
    @Test
    void aCheckRefusalAboutOneFieldMarksThatField() {
        try (Context context = load()) {
            applyWithField(context, 400, "BadRequest", "A delivery date cannot be in the past", "Date");
            assertEquals("", saveFailureMessage(context));
            assertEquals("A delivery date cannot be in the past", context.eval("js", "page.errors.Date")
                                                                         .asString());
        }
    }

    /** A field the form does not show (a hidden or calculated one) keeps the banner. */
    @Test
    void aRefusalAboutAFieldOutsideTheFormKeepsTheBanner() {
        try (Context context = load()) {
            applyWithField(context, 400, "BadRequest", "The total cannot be negative", "Total");
            assertEquals("The total cannot be negative", saveFailureMessage(context));
        }
    }

    /**
     * #8637: a new attempt shows only its own outcome - the previous refusal's sentence does not stay
     * in the banner beside the next attempt's "Fields that need attention".
     */
    @Test
    void aNewAttemptForgetsThePreviousRefusal() {
        try (Context context = load()) {
            apply(context, 500, "InternalServerError", "could not execute statement");
            context.eval("js", "page.resetSaveOutcome();");
            assertEquals("", saveFailureMessage(context));
            assertEquals(0, context.eval("js", "page.unmatchedCauses.length")
                                   .asInt());
        }
    }

    /**
     * #8637: the server's message under a field is about the value it refused, so it goes once that
     * value is changed - with no client rule on the field (a duplicate key) nothing else clears it.
     */
    @Test
    void aServerFieldErrorGoesWhenItsValueChanges() {
        try (Context context = load()) {
            context.eval("js", "page.form.TrackingNumber = 'TRK-1';");
            applyWithField(context, 409, "Conflict", "A delivery with this tracking number already exists", "TrackingNumber");
            context.eval("js", "page.dropStaleServerErrors();");
            assertEquals("A delivery with this tracking number already exists", fieldError(context, "TrackingNumber"),
                    "the refused value is still there - so is the refusal");
            context.eval("js", "page.form.TrackingNumber = 'TRK-2'; page.dropStaleServerErrors();");
            assertEquals("", fieldError(context, "TrackingNumber"));
            assertEquals("", fieldError(context, "__summary"), "the summary repeating it goes with it");
        }
    }

    /** #8637: the 422 causes the server mapped onto fields go the same way, one field at a time. */
    @Test
    void aMappedCauseGoesWhenItsValueChanges() {
        try (Context context = load()) {
            context.eval("js",
                    "page.form.Date = '2026-01-01'; page.form.TrackingNumber = 'TRK-1';"
                            + "page.applyApiError({ isApiError: true, httpStatus: 422, errorType: 'ValidationError', errorCauses: ["
                            + "{ field: 'Date', message: 'Date is in the past' }, { field: 'TrackingNumber', message: 'Too short' }] });"
                            + "page.form.Date = '2026-12-01'; page.dropStaleServerErrors();");
            assertEquals("", fieldError(context, "Date"));
            assertFalse(fieldError(context, "TrackingNumber").isEmpty(), "an unchanged field keeps its cause");
        }
    }

    /** #8637: an error the form's own rules set is not the server's - it keeps its on-blur timing. */
    @Test
    void aClientErrorIsLeftToItsRule() {
        try (Context context = load()) {
            context.eval("js", "page.errors = { Date: 'Date is required' }; page.form.Date = '2026-12-01'; page.dropStaleServerErrors();");
            assertEquals("Date is required", fieldError(context, "Date"));
        }
    }

    /**
     * #8637: the generated forms use both - each save forgets the last outcome before it validates, and
     * the form drops a server field error as that field is edited.
     */
    @Test
    void everyGeneratedFormForgetsAStaleRefusal() throws Exception {
        for (String page : List.of("perspective/manage/form-page.js.template", "perspective/document/document-page.js.template")) {
            String content = read(UI + page);
            int save = content.indexOf("async save() {");
            assertTrue(save >= 0, page + ": no save()");
            int reset = content.indexOf("this.resetSaveOutcome();", save);
            assertTrue(reset >= 0 && reset < content.indexOf("if (!this.validate())", save),
                    page + ": save() must forget the previous refusal before it validates");
        }
        for (String view : List.of("perspective/manage/form-view.html.template", "perspective/document/document-view.html.template")) {
            assertTrue(read(UI + view).contains("x-effect=\"dropStaleServerErrors()\""),
                    view + ": a server field error must go as its field is edited");
        }
    }

    @Test
    void everyGeneratedWritePathQuotesTheRefusal() throws Exception {
        String documentPage = read(UI + "perspective/document/document-page.js.template");
        assertTrue(documentPage.contains("this.draftError = App.services.apiErrors.refusalMessageFor(e, fallback);"),
                "the line dialog's save must show the refusal");
        assertTrue(documentPage.contains("this.itemsError = App.services.apiErrors.refusalMessageFor(e, 'Could not delete the line.');"),
                "a refused line delete must show the refusal");
        for (String page : List.of("my/my-document-page.js.template", "partner/partner-document-page.js.template")) {
            assertTrue(read(UI + page).contains("this.itemError = App.services.apiErrors.refusalMessageFor(e, fallback);"),
                    page + ": the line dialog's save must show the refusal");
        }
        for (String page : List.of("perspective/manage/form-page.js.template", "perspective/manage/list-page.js.template")) {
            String content = read(UI + page);
            assertTrue(content.contains("this.deleteError = App.services.apiErrors.refusalMessageFor(e,"),
                    page + ": a refused delete must show the refusal");
            assertFalse(content.contains("this.deleteError = App.services.apiErrors.messageFor("),
                    page + ": a delete still drops the refusal");
        }
        assertTrue(read(SHELL + "components/detailPanel.js").contains("App.services.apiErrors.refusalMessageFor(e, \"Could not delete.\")"),
                "a refused detail-panel delete must show the refusal");
    }

    private static void apply(Context context, int status, String errorType, String message) {
        context.eval("js", "page.applyApiError({ isApiError: true, httpStatus: " + status + ", errorType: '" + errorType
                + "', errorMessage: " + "'" + message.replace("'", "\\'") + "' }, { fallbackMessage: 'Could not save.' });");
    }

    private static void applyWithField(Context context, int status, String errorType, String message, String field) {
        context.eval("js",
                "page.applyApiError({ isApiError: true, httpStatus: " + status + ", errorType: '" + errorType + "', errorMessage: '"
                        + message.replace("'", "\\'") + "', field: '" + field
                        + "', messageKey: 'shipping:shipping-model.checks.Delivery_0' }, { fallbackMessage: 'Could not save.' });");
    }

    private static String fieldError(Context context, String field) {
        return context.eval("js", "page.errors." + field + " || ''")
                      .asString();
    }

    private static String saveFailureMessage(Context context) {
        return context.eval("js", "page.saveFailureMessage")
                      .asString();
    }

    /**
     * Evaluate the shipped apiError.js, basePage.js and baseFormPage.js; the page holds a form with a
     * Date field, and the scroll-to-summary tick is a no-op outside a browser.
     */
    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this; var T = (key, fallback) => fallback; var console = { error: () => {} };");
        for (String script : List.of("services/apiError.js", "components/pages/basePage.js", "components/pages/baseFormPage.js")) {
            try {
                context.eval("js", read(SHELL + script));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to evaluate " + script, ex);
            }
        }
        context.eval("js", "var page = baseFormPage(); page.form = { Date: null, TrackingNumber: null }; page.$nextTick = () => {};");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaRefusalMessageIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
