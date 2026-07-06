/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.print;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A document master is detected from the explicit {@code function} roles even when the items child
 * is NOT named {@code *Item} - the whole point of dropping the naming convention.
 */
class PrintDocumentByFunctionTest {

    @Test
    void detectsDocumentDeclaredByFunctionWithoutItemNaming() {
        IntentModel model = IntentParser.parse("""
                name: timesheets
                entities:
                  - name: ProjectTimesheet
                    function: Document
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, function: DocumentTitle }
                  - name: EmployeeTimesheet
                    function: DocumentItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet, composition: true, required: true }
                """);
        Map<EntityIntent, EntityIntent> masters = PrintIntentGenerator.documentMasters(model);
        assertEquals(1, masters.size(), "the function-declared document must be detected");
        EntityIntent master = masters.keySet()
                                     .iterator()
                                     .next();
        assertEquals("ProjectTimesheet", master.getName());
        assertEquals("EmployeeTimesheet", masters.get(master)
                                                 .getName(),
                "the DocumentItem child is the line-items, despite not being named *Item");
    }
}
