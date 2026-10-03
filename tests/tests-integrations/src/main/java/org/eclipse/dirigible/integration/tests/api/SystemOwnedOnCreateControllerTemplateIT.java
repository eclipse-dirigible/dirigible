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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * Renders the three generated REST controllers and the generated repository through the platform's
 * {@link VelocityGenerationEngine} to pin WHERE a system-owned column is dropped on create.
 *
 * <p>
 * A {@code readOnly} field, an aggregate footer field, {@code ProcessId(s)}, a label's {@code Name}
 * - the set {@code update()} preserves from the stored row - are not the caller's to set on create
 * either (#7549). #7568 enforced that at the top of the repository's {@code save()}, which is the
 * create path of every system writer: the attachment upload and the snapshot mint assign exactly
 * those columns before {@code save()}, and every uploaded file got a row with null metadata
 * (#7620). The drop belongs to the REST surface, after the controller's own checks and immediately
 * before the save, on all three surfaces - and the repository must keep what a system writer
 * assigns.
 *
 * <p>
 * Both halves are asserted here over the rendered Java, because an unrendered Velocity branch fails
 * only later, at compile time, in a user's project: the controllers must call the drop, drop the
 * right columns and nothing else, and the repository's {@code save()} must carry no such strip
 * while its {@code update()} still preserves. The runtime claim - an upload's row carries its file
 * - is {@code IntentAttachmentUploadIT}'s.
 */
class SystemOwnedOnCreateControllerTemplateIT {

    private static final String REST = "/META-INF/dirigible/template-application-rest-java/api/";
    private static final String DAO = "/META-INF/dirigible/template-application-dao-java/data/Repository.java.template";

    private static final Map<String, String> CONTROLLERS = Map.of("EntityController.java.template", "TicketController",
            "EntityMyController.java.template", "TicketMyController", "EntityPartnerController.java.template", "TicketPartnerController");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void everyControllerDropsTheSystemOwnedColumnsBeforeTheSaveAndNothingElse() throws Exception {
        for (Map.Entry<String, String> controller : CONTROLLERS.entrySet()) {
            String template = controller.getKey();
            String rendered = render(REST + template, context(systemOwnedProperties()));

            int create = rendered.indexOf("public TicketEntity create(");
            int drop = rendered.indexOf("dropSystemOwnedOnCreate(entity);");
            int save = rendered.indexOf("repository.save(entity)");
            assertTrue(create > 0 && drop > create && save > drop,
                    template + " must drop the system-owned columns inside create(), after its checks and before the save: " + rendered);

            // The author's readOnly and the aggregate footer field are dropped, the date-typed one too...
            assertTrue(rendered.contains("entity.Solution = null;"), template + " must drop the readOnly column: " + rendered);
            assertTrue(rendered.contains("entity.Total = null;"), template + " must drop the aggregate column: " + rendered);
            assertTrue(rendered.contains("entity.CreatedAt = null;"), template + " must drop the date-typed readOnly column");
            // ...and the discard is reported, except for the date, whose round-trip is lossy.
            assertTrue(rendered.contains("discarded.add(\"Solution=\" + entity.Solution);"), template + " must report the discard");
            assertTrue(rendered.contains("discarded.add(\"Total=\" + entity.Total);"), template + " must report the discard");
            assertFalse(rendered.contains("\"CreatedAt=\""), template + " must not report a date: " + rendered);
            assertTrue(rendered.contains("LOG.warn(\"A create of Ticket carried system-owned {} - discarded."),
                    template + " must warn once per create naming what was dropped");
            assertTrue(rendered.contains("Logging.getLogger(\"gen.services.api.tickets." + controller.getValue() + "\")"),
                    template + " must log through the SDK logger of the generated class: " + rendered);

            // An ordinary column, the primary key, the number and uuid stamps (save()'s own #7548
            // discard) and an aggregate the repository recomputes on update are all left alone.
            for (String kept : List.of("Title", "Id", "Number", "Uuid", "Balance")) {
                assertFalse(rendered.contains("entity." + kept + " = null;"), template + " must not drop " + kept + ": " + rendered);
            }
            assertNoUnresolvedReferences(rendered);
        }
    }

    @Test
    void anEntityWithNoSystemOwnedColumnEmitsNoDropAndNoLogger() throws Exception {
        for (String template : CONTROLLERS.keySet()) {
            String rendered = render(REST + template, context(List.of(primaryKey(), plain("Title"))));

            assertFalse(rendered.contains("dropSystemOwnedOnCreate"), template + " must emit no drop without a system-owned column");
            assertFalse(rendered.contains("sdk.log"), template + " must not import a logger it does not use: " + rendered);
        }
    }

    /** A see-only personal surface refuses every write with 403 before any payload is looked at. */
    @Test
    void aSeeOnlyPersonalSurfaceEmitsNoDrop() throws Exception {
        Map<String, Object> context = context(systemOwnedProperties());
        context.put("personalReadOnly", Boolean.TRUE);
        String rendered = render(REST + "EntityMyController.java.template", context);

        assertFalse(rendered.contains("dropSystemOwnedOnCreate"), "a see-only surface has nothing to drop: " + rendered);
        assertFalse(rendered.contains("sdk.log"), "a see-only surface has nothing to report: " + rendered);
    }

    /**
     * The repository is the trusted path of every system writer: its {@code save()} stores what the
     * writer assigned, and only its {@code update()} preserves the system-owned columns.
     */
    @Test
    void theRepositoryKeepsWhatASystemWriterAssignsOnCreateAndStillPreservesOnUpdate() throws Exception {
        String rendered = render(DAO, daoContext());

        int saveStart = rendered.indexOf("public TicketEntity save(TicketEntity entity) {");
        int updateStart = rendered.indexOf("public TicketEntity update(TicketEntity entity) {");
        assertTrue(saveStart > 0 && updateStart > saveStart, "the repository must render save() and update(): " + rendered);
        String save = rendered.substring(saveStart, updateStart);
        String update = rendered.substring(updateStart);

        assertFalse(save.contains("entity.Solution = null;"), "save() must keep a readOnly value a system writer assigned: " + save);
        assertFalse(save.contains("entity.Total = null;"), "save() must keep an aggregate value a system writer assigned: " + save);
        assertFalse(save.contains("entity.CreatedAt = null;"), "save() must keep a date a system writer assigned: " + save);
        assertFalse(rendered.contains("discardedOnCreate"), "the create-time strip of #7568 must be gone: " + rendered);

        assertTrue(update.contains("entity.Solution = existingRow.Solution;"),
                "update() must still preserve the readOnly column: " + update);
        assertTrue(update.contains("entity.Total = existingRow.Total;"), "update() must still preserve the aggregate column: " + update);
    }

    private String render(String location, Map<String, Object> parameters) throws Exception {
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** Asserts the emitted drop resolved every reference - an unresolved one renders literally. */
    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("dropSystemOwnedOnCreate") || line.contains("discarded") || line.contains("LOG")
                    || line.contains("system-owned")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the drop: " + line);
            }
        }
    }

    private static Map<String, Object> context(List<Map<String, Object>> properties) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("name", "Ticket");
        parameters.put("projectName", "services");
        parameters.put("perspectiveName", "Tickets");
        parameters.put("javaGenFolderName", "services");
        parameters.put("javaPerspectiveName", "tickets");
        parameters.put("properties", properties);
        parameters.put("sensitiveProperties", new ArrayList<>());
        parameters.put("personalProperty", "Person");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.services.data.people.PersonRepository");
        parameters.put("partnerProperty", "Customer");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.services.data.people.CustomerRepository");
        return parameters;
    }

    /** The minimal DAO template context: the entity above with no compositions, checks or history. */
    private static Map<String, Object> daoContext() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("name", "Ticket");
        parameters.put("entityLabel", "Ticket");
        parameters.put("projectName", "services");
        parameters.put("perspectiveName", "Tickets");
        parameters.put("javaGenFolderName", "services");
        parameters.put("javaPerspectiveName", "tickets");
        parameters.put("pkPropertyName", "Id");
        parameters.put("dataSource", "DefaultDB");
        parameters.put("properties", List.of(primaryKey(), plain("Title"), readOnly("Solution"), aggregate("Total"), createdAt()));
        return parameters;
    }

    /**
     * One of each kind the rule distinguishes, as {@code ModelParameterProcessor} hands them to the
     * templates: an ordinary column, an author's readOnly, an aggregate footer field, an aggregate
     * recomputed on update, a number stamped on create, a uuid, and a date-typed audit column.
     */
    private static List<Map<String, Object>> systemOwnedProperties() {
        Map<String, Object> balance = aggregate("Balance");
        balance.put("calculatedPropertyExpressionUpdate", "Total - Paid");
        Map<String, Object> number = readOnly("Number");
        number.put("numberStampOnCreate", "true");
        Map<String, Object> uuid = readOnly("Uuid");
        uuid.put("generatedUuid", "true");
        return List.of(primaryKey(), plain("Title"), readOnly("Solution"), aggregate("Total"), balance, number, uuid, createdAt());
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "TICKET_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> plain(String name) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", "TICKET_" + name.toUpperCase());
        property.put("dataType", "VARCHAR");
        property.put("dataTypeJavaClass", "String");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }

    private static Map<String, Object> readOnly(String name) {
        Map<String, Object> property = plain(name);
        property.put("isReadOnlyProperty", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> aggregate(String name) {
        Map<String, Object> property = plain(name);
        property.put("dataType", "DECIMAL");
        property.put("dataTypeJavaClass", "java.math.BigDecimal");
        property.put("aggregate", "true");
        return property;
    }

    private static Map<String, Object> createdAt() {
        Map<String, Object> property = readOnly("CreatedAt");
        property.put("dataType", "TIMESTAMP");
        property.put("dataTypeJavaClass", "java.util.Date");
        property.put("isDateType", Boolean.TRUE);
        return property;
    }
}
