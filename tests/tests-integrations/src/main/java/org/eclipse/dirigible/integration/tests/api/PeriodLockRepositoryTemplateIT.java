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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * The period lock ({@code immutableInPeriod:}) enforced in the generated REPOSITORY (dirigible
 * #7590), rendered through the platform's {@link VelocityGenerationEngine}: a write dated inside a
 * closed period is refused wherever the row is written - a user's REST write already met the
 * controller's 409, but a generated posting, a create-from and a schedule write through the
 * repository and never meet a controller.
 */
class PeriodLockRepositoryTemplateIT {

    private static final String DAO_BASE = "/META-INF/dirigible/template-application-dao-java/data/";
    private static final String CALL = "requirePeriodOpen(entity.BookedOn);";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void everyWritePathRefusesADateInsideAClosedPeriod() throws Exception {
        String rendered = render(context(true));

        // save, update and updateWithoutEvent - the three writes that carry the date.
        assertEquals(3, rendered.split(java.util.regex.Pattern.quote(CALL), -1).length - 1,
                "the period check must run on every write path that carries the date: " + rendered);
        assertTrue(rendered.contains("private void requirePeriodOpen(java.time.LocalDate date) {"), rendered);
        assertTrue(rendered.contains("new gen.ledger.data.accountingperiod.AccountingPeriodRepository().findAll("),
                "the register is queried through its own repository: " + rendered);
        assertTrue(rendered.contains(".le(\"StartDate\", date)") && rendered.contains(".ge(\"EndDate\", date)"),
                "the covering period: " + rendered);
        assertTrue(rendered.contains("for (String closed : \"2,3\".split(\",\")) {"), "the closed statuses: " + rendered);
        assertTrue(
                rendered.contains("throw new ValidationException(") && rendered.contains(
                        "\"The AccountingPeriod covering this date is closed - book the correction in an open period\");"),
                "refused with the controller's own sentence, as a ValidationException: " + rendered);
        assertTrue(rendered.contains("import org.eclipse.dirigible.sdk.db.ValidationException;"), rendered);
    }

    /** An entity declaring no period lock renders exactly as it did. */
    @Test
    void anEntityWithoutAPeriodLockRendersNoCheck() throws Exception {
        String rendered = render(context(false));

        assertFalse(rendered.contains("requirePeriodOpen"), rendered);
    }

    private String render(Map<String, Object> parameters) throws Exception {
        String location = DAO_BASE + "Repository.java.template";
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** The lock as ModelParameterProcessor.resolvePeriodLock hands it to the template. */
    private static Map<String, Object> context(boolean locked) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "LedgerBooking");
        parameters.put("projectName", "ledger");
        parameters.put("perspectiveName", "LedgerBooking");
        parameters.put("javaGenFolderName", "ledger");
        parameters.put("javaPerspectiveName", "ledgerbooking");
        parameters.put("tablePrefix", "LEDGER_");
        parameters.put("dataName", "LEDGER_BOOKING");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties", List.of(primaryKey(), property("BookedOn", "DATE", "java.time.LocalDate")));
        parameters.put("sensitiveProperties", List.of());
        if (locked) {
            Map<String, Object> periodLock = new LinkedHashMap<>();
            periodLock.put("dateProperty", "BookedOn");
            periodLock.put("dateJavaClass", "java.time.LocalDate");
            periodLock.put("entity", "AccountingPeriod");
            periodLock.put("entityClass", "gen.ledger.data.accountingperiod.AccountingPeriodEntity");
            periodLock.put("repositoryClass", "gen.ledger.data.accountingperiod.AccountingPeriodRepository");
            periodLock.put("startProperty", "StartDate");
            periodLock.put("endProperty", "EndDate");
            periodLock.put("statusProperty", "Status");
            periodLock.put("closedValues", "2,3");
            parameters.put("periodLock", periodLock);
        }
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = property("Id", "INTEGER", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> property(String name, String dataType, String javaClass) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", "LEDGER_BOOKING_" + name.toUpperCase());
        property.put("dataType", dataType);
        property.put("dataTypeJavaClass", javaClass);
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataNotNull", Boolean.FALSE);
        return property;
    }
}
