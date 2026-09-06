/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Reading the subject a generated trigger seeded into a process's variables (issue #7077) - the
 * locators an Inbox row resolves the record's number, counterparty and total through.
 */
class TaskSubjectTest {

    private static Map<String, Object> variables() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("__entityUrl", "/services/java/billing/gen/billing/api/sales_invoice/SalesInvoiceController");
        variables.put("__entityId", 6);
        variables.put("__subjectFields", "Number:text,Customer:relation,Total:number");
        variables.put("__CustomerEntityUrl", "/services/java/billing/gen/billing/api/customer/CustomerController");
        variables.put("__CustomerEntityLabel", "Name");
        return variables;
    }

    @Test
    void aRelationCarriesTheLocatorsItsLabelIsReadThrough() {
        TaskSubject subject = TaskSubject.from(variables());

        assertEquals("6", subject.id());
        assertEquals(3, subject.fields()
                               .size());
        assertEquals(new TaskSubject.Field("Number", "text", null, null), subject.fields()
                                                                                 .get(0));
        assertEquals(
                new TaskSubject.Field("Customer", "relation", "/services/java/billing/gen/billing/api/customer/CustomerController", "Name"),
                subject.fields()
                       .get(1));
        assertEquals("number", subject.fields()
                                      .get(2)
                                      .kind());
    }

    /**
     * A relation whose target locator the process does not carry is DROPPED, not shown: rendering the
     * raw foreign key would put a second opaque id where the id was the complaint.
     */
    @Test
    void aRelationWithoutItsLocatorIsDropped() {
        Map<String, Object> variables = variables();
        variables.remove("__CustomerEntityUrl");

        assertEquals(2, TaskSubject.from(variables)
                                   .fields()
                                   .size());
    }

    /** A hand-authored BPMN, or a deployment generated before #7077, declares no subject at all. */
    @Test
    void aProcessThatDeclaresNoSubjectHasNone() {
        Map<String, Object> variables = variables();
        variables.remove("__subjectFields");

        assertNull(TaskSubject.from(variables));
        assertNull(TaskSubject.from(Map.of()));
    }
}
