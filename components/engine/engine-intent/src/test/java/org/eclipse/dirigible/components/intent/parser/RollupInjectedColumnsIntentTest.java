/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A roll-up reads the columns the generator injects into its child (dirigible #7947): the file
 * metadata of a {@code function: Attachment} child and the audit columns of an audited or file
 * child - the "mirror the newest upload's path onto the parent" shape.
 */
class RollupInjectedColumnsIntentTest {

    private static final String YAML = """
            name: logos
            entities:
              - name: Company
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: logo,   type: string, length: 1000, readOnly: true }
                  - { name: images, type: integer, readOnly: true }
              - name: CompanyLogo
                function: Attachment
                relations:
                  - { name: Company, kind: manyToOne, to: Company, composition: true, required: true }
            rollups:
              - { name: latestLogo, entity: CompanyLogo, via: Company, field: logo, op: latest, of: StoragePath, by: CreatedAt }
              - name: imageCount
                entity: CompanyLogo
                via: Company
                field: images
                where:
                  - { field: contentType, op: eq, value: image/png }
            """;

    private static String refusalOf(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getMessage();
    }

    @Test
    void aRollupReadsTheInjectedFileAndAuditColumnsOfAnAttachmentChild() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void anInjectedColumnKeepsItsType() {
        String message = refusalOf(YAML.replace("{ name: logo,   type: string, length: 1000, readOnly: true }",
                "{ name: logo,   type: integer, readOnly: true }"));
        assertTrue(message.contains("type [integer] must match the copied `of` field type [string]"), message);
        message = refusalOf(YAML.replace("by: CreatedAt", "by: CreatedBy"));
        assertTrue(message.contains("by [CreatedBy] must be a date/timestamp field"), message);
    }

    @Test
    void onlyAFileChildHasTheFileColumnsAndOnlyAnAuditedEntityTheAuditOnes() {
        String plain = "fields:\n      - { name: id, type: integer, primaryKey: true, generated: true }";
        String message = refusalOf(YAML.replace("function: Attachment", plain));
        assertTrue(message.contains("rollup [latestLogo] of [StoragePath] is not a field of [CompanyLogo]"), message);
        assertTrue(message.contains("rollup [latestLogo] by [CreatedAt] is not a field of [CompanyLogo]"), message);
        assertTrue(message.contains("where-condition reads [contentType], which is not a field or to-one relation of [CompanyLogo]"),
                message);
        message = refusalOf(YAML.replace("function: Attachment", "audit: true\n    " + plain));
        assertTrue(message.contains("rollup [latestLogo] of [StoragePath] is not a field of [CompanyLogo]"), message);
        assertTrue(!message.contains("by [CreatedAt]"), message);
    }
}
