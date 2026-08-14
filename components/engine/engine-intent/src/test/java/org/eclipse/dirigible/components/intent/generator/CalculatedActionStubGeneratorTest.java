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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Which calculated-field action this project is allowed to scaffold a stub for - the decision that
 * matters, because scaffolding one somebody else already owns is not a harmless extra file: two
 * compilation units with the same binary name fail the whole registry-wide client-Java batch.
 */
class CalculatedActionStubGeneratorTest {

    private static final String INTENT = """
            name: sales
            entities:
              - name: Invoice
                imports: |
                  import shared.numbers.SharedNumberAction;
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, calculatedActionOnCreate: InvoiceNumberAction }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);

    private EntityIntent invoice() {
        return model.getEntities()
                    .get(0);
    }

    @Test
    void anUnqualifiedActionIsScaffoldedIntoCustom() {
        assertEquals("custom/InvoiceNumberAction.java", CalculatedActionStubGenerator.targetFile(invoice(), "InvoiceNumberAction"));
    }

    @Test
    void aCustomPackagedActionKeepsItsSubfolders() {
        assertEquals("custom/sales/NumberAction.java", CalculatedActionStubGenerator.targetFile(invoice(), "custom.sales.NumberAction"));
    }

    @Test
    void anActionOutsideCustomIsNotOurs() {
        assertNull(CalculatedActionStubGenerator.targetFile(invoice(), "shared.numbers.SharedNumberAction"));
    }

    @Test
    void anActionTheEntityImportsFromElsewhereIsNotOurs() {
        // The entity's authored imports say where the class comes from - it is another compilation unit,
        // and writing our own would collide on the binary name.
        assertNull(CalculatedActionStubGenerator.targetFile(invoice(), "SharedNumberAction"));
    }

    @Test
    void theStubImplementsTheSdkContractAndReturnsTheFieldsType() {
        String stub = CalculatedActionStubGenerator.stub("custom", "InvoiceNumberAction", "Invoice", "number", "String");

        assertTrue(stub.contains("package custom;"));
        assertTrue(stub.contains("import org.eclipse.dirigible.sdk.db.CalculatedField;"));
        assertTrue(stub.contains("@Component"));
        assertTrue(stub.contains("public class InvoiceNumberAction implements CalculatedField<Object, String>"));
        assertTrue(stub.contains("public String calculate(Object entity)"));
        // It says what it is for, and that it is the developer's from here on.
        assertTrue(stub.contains("Invoice.number"));
        assertTrue(stub.contains("never"));
    }

    @Test
    void theValueTypeFollowsTheFieldsLogicalType() {
        assertEquals("String", CalculatedActionStubGenerator.valueType("string"));
        assertEquals("Integer", CalculatedActionStubGenerator.valueType("integer"));
        assertEquals("java.math.BigDecimal", CalculatedActionStubGenerator.valueType("decimal"));
        assertEquals("java.time.LocalDate", CalculatedActionStubGenerator.valueType("date"));
    }
}
