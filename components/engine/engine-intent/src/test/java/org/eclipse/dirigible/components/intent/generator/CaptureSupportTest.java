/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The {@code captures:} construct (#8544): what the parser refuses, and the capture class the glue
 * generator plans from what it accepts.
 */
class CaptureSupportTest {

    private static final String ENTITIES = """
            name: purchasing
            entities:
              - name: Supplier
                fields:
                  - { name: id,                 type: integer, primaryKey: true, generated: true }
                  - { name: name,               type: string,  length: 100 }
                  - { name: registrationNumber, type: string,  length: 20 }
                  - { name: vatNumber,          type: string,  length: 20 }
                  - { name: rank,               type: integer }
              - name: InvoiceStatus
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: PurchaseInvoice
                checks:
                  - { kind: duplicate, fields: [Supplier, number], message: "An invoice with this number already exists: {match}" }
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: number,     type: string,  length: 50 }
                  - { name: issuedOn,   type: date }
                  - { name: receivedAt, type: timestamp }
                  - { name: paid,       type: boolean }
                  - { name: net,        type: decimal, precision: 18, scale: 2 }
                  - { name: total,      type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "round(Net * 1.2, 2)", calculatedOnUpdate: "round(Net * 1.2, 2)" }
                relations:
                  - { name: Supplier, kind: manyToOne, to: Supplier }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: PurchaseInvoiceItem
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 200 }
                  - { name: quantity,    type: decimal, precision: 18, scale: 3 }
                  - { name: price,       type: decimal, precision: 18, scale: 2 }
                  - { name: amount,      type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "round(Quantity * Price, 2)", calculatedOnUpdate: "round(Quantity * Price, 2)" }
                relations:
                  - { name: PurchaseInvoice, kind: manyToOne, to: PurchaseInvoice, composition: true, required: true }
                  - { name: Vendor, kind: manyToOne, to: Supplier }
              - name: PurchaseInvoiceNote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
                relations:
                  - { name: PurchaseInvoice, kind: manyToOne, to: PurchaseInvoice, composition: true, required: true }
              - name: InvoiceScan
                function: Attachment
                relations:
                  - { name: PurchaseInvoice, kind: manyToOne, to: PurchaseInvoice, composition: true, required: true }
              - name: SupplierScan
                function: Attachment
                relations:
                  - { name: Supplier, kind: manyToOne, to: Supplier, composition: true, required: true }
              - name: Stray
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Keyless
                fields:
                  - { name: name, type: string }
            """;

    private static final String VALID = ENTITIES + """
            captures:
              - name: supplierInvoice
                into: PurchaseInvoice
                attachment: InvoiceScan
                instructions: "The supplier is the party that issued the document."
                qr: { separator: "*", parts: [device, receipt, date, time, total] }
                fields:
                  - { field: number, describe: "the supplier's own number of the document" }
                  - { field: issuedOn, qr: date }
                  - { field: net }
                  - { field: total, compare: true }
                  - { field: Supplier, match: [registrationNumber, vatNumber], describe: "the supplier" }
                items:
                  into: PurchaseInvoiceItem
                  fields:
                    - { field: description }
                    - { field: quantity }
                    - { field: price }
                    - { field: Vendor, match: [vatNumber] }
              - name: plainScan
                into: Stray
                fields:
                  - { field: name }
            """;

    @Test
    void a_valid_capture_with_header_fields_a_relation_a_qr_a_compare_items_and_an_attachment_parses() {
        IntentModel model = IntentParser.parse(VALID);

        assertEquals(2, model.getCaptures()
                             .size());
        assertEquals(Set.of("PurchaseInvoice", "Stray"), CaptureSupport.targets(model));
    }

    @Test
    void a_capture_is_planned_with_its_entity_table_key_fields_and_schema() {
        List<Map<String, Object>> captures = build(IntentParser.parse(VALID), new IntentSettings());

        assertEquals(2, captures.size());
        Map<String, Object> invoice = captures.get(0);
        assertEquals("SupplierInvoice", invoice.get("className"));
        assertEquals("PurchaseInvoice", invoice.get("entity"));
        assertEquals("PURCHASING_PURCHASE_INVOICE", invoice.get("table"));
        assertEquals("Id", invoice.get("keyProperty"));
        assertEquals(List.of("number:STRING", "issuedOn:DATE", "net:NUMBER", "total:NUMBER", "Supplier_registrationNumber:STRING",
                "Supplier_vatNumber:STRING"), schema(invoice.get("schemaFields")));
        assertEquals(List.of("description:STRING", "quantity:NUMBER", "price:NUMBER", "Vendor_vatNumber:STRING"),
                schema(invoice.get("schemaItems")));
        assertEquals(List.of("number", "issuedOn", "net"), names(invoice.get("fields")), "written onto the record");
        assertEquals(List.of("total"), names(invoice.get("compares")), "compared with the saved record, never written");
        assertEquals(List.of("Supplier"), names(invoice.get("relations")));
        assertEquals(Boolean.TRUE, invoice.get("hasRelations"));
        assertEquals(Boolean.TRUE, invoice.get("hasCompares"));
        assertEquals(Boolean.TRUE, invoice.get("hasWarnings"), "the entity's soft duplicate check is asked of the capture (#8631)");
        assertNotNull(invoice.get("roles"));
        assertFalse(((String) invoice.get("roles")).isBlank(), "the upload is gated by the entity's write role");
        assertFalse(((String) invoice.get("readRoles")).isBlank());
    }

    @Test
    @SuppressWarnings("unchecked")
    void a_relation_is_planned_with_its_keys_in_the_declared_order() {
        Map<String, Object> invoice = build(IntentParser.parse(VALID), new IntentSettings()).get(0);

        Map<String, Object> supplier = ((List<Map<String, Object>>) invoice.get("relations")).get(0);
        assertEquals("Supplier", supplier.get("targetEntity"));
        assertEquals(Boolean.FALSE, supplier.get("crossModel"));
        assertEquals("Id", supplier.get("targetKeyProperty"));
        List<Map<String, Object>> keys = (List<Map<String, Object>>) supplier.get("keys");
        assertEquals(List.of("RegistrationNumber", "VatNumber"), keys.stream()
                                                                     .map(key -> key.get("byProperty"))
                                                                     .toList());
        assertEquals("\"Supplier_registrationNumber\"", keys.get(0)
                                                            .get("schemaNameLiteral"));
        Map<String, Object> vendor = ((List<Map<String, Object>>) invoice.get("itemRelations")).get(0);
        assertEquals("Vendor", vendor.get("name"));
    }

    @Test
    void items_attachment_and_qr_are_planned() {
        Map<String, Object> invoice = build(IntentParser.parse(VALID), new IntentSettings()).get(0);

        assertEquals(Boolean.TRUE, invoice.get("hasItems"));
        assertEquals("PurchaseInvoiceItem", invoice.get("itemEntity"));
        assertEquals("PurchaseInvoice", invoice.get("itemBackRef"));
        assertEquals(Boolean.TRUE, invoice.get("hasAttachment"));
        assertEquals("InvoiceScan", invoice.get("attachmentEntity"));
        assertEquals("PurchaseInvoice", invoice.get("attachmentBackRef"));
        assertEquals(Boolean.TRUE, invoice.get("hasQr"));
        assertEquals("\"device\", \"receipt\", \"date\", \"time\", \"total\"", invoice.get("qrPartsLiteral"));
        assertEquals("\"*\"", invoice.get("qrSeparatorLiteral"));
    }

    @Test
    void a_capture_without_items_attachment_or_qr_plans_none_of_them() {
        Map<String, Object> plain = build(IntentParser.parse(VALID), new IntentSettings()).get(1);

        assertEquals("PlainScan", plain.get("className"));
        assertEquals(Boolean.FALSE, plain.get("hasItems"));
        assertEquals(Boolean.FALSE, plain.get("hasAttachment"));
        assertEquals(Boolean.FALSE, plain.get("hasQr"));
        assertEquals(Boolean.FALSE, plain.get("hasRelations"));
        assertEquals(Boolean.FALSE, plain.get("hasCompares"));
        assertEquals(Boolean.FALSE, plain.get("hasWarnings"));
        assertEquals("null", plain.get("instructionsLiteral"));
    }

    @Test
    void a_settings_opt_out_drops_the_capture() {
        IntentSettings settings = IntentSettings.parse("{\"overrides\":{\"captures\":{\"plainScan\":{\"generate\":false}}}}");

        List<Map<String, Object>> captures = build(IntentParser.parse(VALID), settings);

        assertEquals(List.of("SupplierInvoice"), captures.stream()
                                                         .map(capture -> capture.get("className"))
                                                         .toList());
    }

    // ----------------------------------------------------------------------------------- refusals

    @Test
    void a_capture_without_a_name_is_refused() {
        assertRefused("""
                captures:
                  - { into: Stray, fields: [{ field: name }] }
                """, "capture has no name");
    }

    @Test
    void a_duplicate_name_is_refused() {
        assertRefused("""
                captures:
                  - { name: scan, into: Stray, fields: [{ field: name }] }
                  - { name: scan, into: Stray, fields: [{ field: name }] }
                """, "duplicate capture [scan]");
    }

    @Test
    void two_names_generating_the_same_class_are_refused() {
        assertRefused("""
                captures:
                  - { name: scanIn, into: Stray, fields: [{ field: name }] }
                  - { name: ScanIn, into: Stray, fields: [{ field: name }] }
                """, "generates the class ScanInCapture, which another capture's name generates too");
    }

    @Test
    void an_unknown_entity_is_refused() {
        assertRefused("""
                captures:
                  - { name: scan, into: Ghost, fields: [{ field: name }] }
                """, "captures into unknown entity [Ghost]");
    }

    @Test
    void a_composition_child_target_is_refused() {
        assertRefused("""
                captures:
                  - { name: scan, into: PurchaseInvoiceItem, fields: [{ field: description }] }
                """, "a composition child");
    }

    @Test
    void a_target_without_a_primary_key_is_refused() {
        assertRefused("""
                captures:
                  - { name: scan, into: Keyless, fields: [{ field: name }] }
                """, "which declares no primary key");
    }

    @Test
    void a_capture_reading_no_fields_is_refused() {
        assertRefused("""
                captures:
                  - { name: scan, into: Stray }
                """, "reads no fields");
    }

    @Test
    void an_unknown_field_is_refused() {
        assertRefused(header("{ field: nope }"), "field [nope] is not a field or a to-one relation of [PurchaseInvoice]");
    }

    @Test
    void a_field_read_twice_is_refused() {
        assertRefused(header("{ field: number }, { field: number }"), "field [number] is read twice");
    }

    @Test
    void the_primary_key_is_refused() {
        assertRefused(header("{ field: id }"), "field [id] is the primary key of [PurchaseInvoice]");
    }

    @Test
    void a_timestamp_field_is_refused() {
        assertRefused(header("{ field: receivedAt }"), "has type [timestamp], which a document does not carry");
    }

    @Test
    void a_calculated_field_without_compare_is_refused() {
        assertRefused(header("{ field: total }"), "field [total] is calculated by the entity, so a capture cannot write it");
    }

    @Test
    void compare_on_a_plain_field_is_refused() {
        assertRefused(header("{ field: net, compare: true }"), "field [net] declares compare:, but nothing calculates it");
    }

    @Test
    void compare_on_an_item_field_is_refused() {
        assertRefused(items("{ field: amount, compare: true }"), "field [amount] declares compare:, which checks the saved document");
    }

    @Test
    void qr_naming_an_undeclared_part_is_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    qr: { parts: [date] }
                    fields:
                      - { field: issuedOn, qr: when }
                """, "checks against the qr part [when], which the capture's qr: does not name");
    }

    @Test
    void qr_on_an_item_field_is_refused() {
        assertRefused(items("{ field: price, qr: total }"), "field [price] declares qr:, but the code describes the document");
    }

    @Test
    void qr_together_with_compare_is_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    qr: { parts: [total] }
                    fields:
                      - { field: total, compare: true, qr: total }
                """, "field [total] declares both qr: and compare:");
    }

    @Test
    void a_relation_without_match_is_refused() {
        assertRefused(header("{ field: Supplier }"), "field [Supplier] is a relation to [Supplier] - name under match:");
    }

    @Test
    void match_on_a_plain_field_is_refused() {
        assertRefused(header("{ field: number, match: [name] }"), "field [number] declares match:, which resolves a relation");
    }

    @Test
    void match_by_an_unknown_target_field_is_refused() {
        assertRefused(header("{ field: Supplier, match: [nope] }"), "matches by [nope], which is not a field of [Supplier]");
    }

    @Test
    void match_by_a_non_string_field_is_refused() {
        assertRefused(header("{ field: Supplier, match: [rank] }"), "matches by [rank] of type [integer] - match by a string field");
    }

    @Test
    void the_status_relation_is_refused() {
        assertRefused(header("{ field: Status, match: [name] }"), "field [Status] is the entity's status");
    }

    @Test
    void the_item_back_reference_is_refused() {
        assertRefused(items("{ field: PurchaseInvoice, match: [number] }"), "is the line's back-reference to the document");
    }

    @Test
    void items_into_an_unknown_entity_are_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    fields: [{ field: number }]
                    items: { into: Ghost, fields: [{ field: description }] }
                """, "items read into unknown entity [Ghost]");
    }

    @Test
    void items_into_a_non_composition_child_are_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    fields: [{ field: number }]
                    items: { into: Stray, fields: [{ field: name }] }
                """, "items read into [Stray], which is not a composition child of [PurchaseInvoice]");
    }

    @Test
    void an_unknown_attachment_is_refused() {
        assertRefused(attachment("Ghost"), "keeps the file in unknown entity [Ghost]");
    }

    @Test
    void an_attachment_that_is_not_a_file_entity_is_refused() {
        assertRefused(attachment("PurchaseInvoiceNote"), "keeps the file in [PurchaseInvoiceNote], which is not a function: Attachment");
    }

    @Test
    void an_attachment_that_is_not_a_child_of_the_target_is_refused() {
        assertRefused(attachment("SupplierScan"),
                "keeps the file in [SupplierScan], which is not a composition child of [PurchaseInvoice]");
    }

    @Test
    void a_qr_without_parts_is_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    qr: { separator: "*", parts: [] }
                    fields: [{ field: number }]
                """, "qr names no parts");
    }

    @Test
    void a_qr_part_named_twice_is_refused() {
        assertRefused("""
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    qr: { parts: [date, date] }
                    fields: [{ field: number }]
                """, "qr names the part [date] twice");
    }

    // ------------------------------------------------------------------------------------ helpers

    private static String header(String fields) {
        return """
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    fields: [%s]
                """.formatted(fields);
    }

    private static String items(String fields) {
        return """
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    fields: [{ field: number }]
                    items: { into: PurchaseInvoiceItem, fields: [%s] }
                """.formatted(fields);
    }

    private static String attachment(String entity) {
        return """
                captures:
                  - name: scan
                    into: PurchaseInvoice
                    attachment: %s
                    fields: [{ field: number }]
                """.formatted(entity);
    }

    private static void assertRefused(String captures, String expected) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + captures));
        List<String> issues = refused.getIssues();
        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "missing issue [" + expected + "] in:\n" + String.join("\n", issues));
    }

    @SuppressWarnings("unchecked")
    private static List<String> schema(Object schemaFields) {
        return ((List<Map<String, Object>>) schemaFields).stream()
                                                         .map(field -> strip((String) field.get("nameLiteral")) + ":" + field.get("type"))
                                                         .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Object fields) {
        return ((List<Map<String, Object>>) fields).stream()
                                                   .map(field -> (String) field.get("name"))
                                                   .toList();
    }

    private static String strip(String literal) {
        return literal.substring(1, literal.length() - 1);
    }

    private static List<Map<String, Object>> build(IntentModel model, IntentSettings settings) {
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "purchasing", null);
        return CaptureSupport.build(model, IntentEntities.byName(model), IntentEntities.compositionParents(model), settings, context);
    }
}
