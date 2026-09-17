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
 * The create path of the SELF-SERVICE surfaces, rendered through the platform's
 * {@link VelocityGenerationEngine} (dirigible #7099).
 *
 * <p>
 * The personal (my) and partner controllers are the ones a person actually types into, and they
 * used to be the only ones that skipped the power surface's value validation: a missing required
 * field reached the insert and came back as an HTTP 500 quoting a NOT NULL violation on a physical
 * column, where the power surface answers 400 naming the property. For a numbered document it was
 * worse than a bad status - the repository had already allocated the document number, so the
 * refused create left a permanent hole in a series a jurisdiction requires to be gap-free.
 *
 * <p>
 * Both halves are asserted here: the validator on the two self-service surfaces, and the ORDER of
 * the number allocation in the generated repository - last, after every guard that can refuse the
 * create. Rendering needs nothing from a running instance, so this boots no application context,
 * exactly like {@code RoleScopedFieldControllerTemplateIT}, whose fixture shape it mirrors.
 */
class PersonalSurfaceCreateValidationTemplateIT {

    private static final String REST_BASE = "/META-INF/dirigible/template-application-rest-java/api/";
    private static final String DAO_BASE = "/META-INF/dirigible/template-application-dao-java/data/";

    private static final List<String> SELF_SERVICE_SURFACES =
            List.of("EntityMyController.java.template", "EntityPartnerController.java.template");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    /** The message is the power surface's, verbatim: one refusal, whichever door the caller used. */
    @Test
    void aSelfServiceSurfaceNamesTheMissingRequiredFieldExactlyAsThePowerSurfaceDoes() throws Exception {
        String expected = "throw new ResponseStatusException(HttpStatus.BAD_REQUEST, \"The 'FromDate' property is required\");";
        assertTrue(render("EntityController.java.template", context()).contains(expected), "the power surface's own refusal changed");

        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template, context());
            assertTrue(rendered.contains(expected), template + " must refuse a missing required field by name: " + rendered);
        }
    }

    /**
     * Length and pattern constraints and row-level checks come with it - the whole validator, not a
     * subset.
     */
    @Test
    void aSelfServiceSurfaceAppliesTheLengthPatternAndRowChecksToo() throws Exception {
        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template, context());

            assertTrue(rendered.contains("The 'Note' exceeds the maximum length of 40"), template + " must enforce the declared length");
            assertTrue(rendered.contains("does not match the required pattern"), template + " must enforce the format pattern");
            assertTrue(rendered.contains("exactly one of FromDate / Note"), template + " must enforce the row-level check");
        }
    }

    /**
     * Validated after the owner is forced and the sensitive / role-scoped fields cleared - the payload
     * the server has finished deciding - and before the save, so a refusal never reaches the insert.
     */
    @Test
    void aSelfServiceSurfaceValidatesTheFinalPayloadAndDoesItBeforeTheWrite() throws Exception {
        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(template, context());

            int owner = rendered.indexOf("// The owner is the server's decision");
            int validate = rendered.indexOf("        validate(entity);");
            int save = rendered.indexOf("repository.save(entity)");
            assertTrue(owner >= 0 && validate > owner, template + " must validate what it decided, not the raw payload: " + rendered);
            assertTrue(save > validate, template + " must validate before the insert: " + rendered);

            int update = rendered.indexOf("repository.update(entity)");
            assertTrue(rendered.lastIndexOf("        validate(entity);") < update, template + " must validate before the update too");
        }
    }

    /**
     * The counter is a sequence incremented in its own transaction, so what it hands out is spent
     * whatever the insert does: it has to be the LAST thing before the statement.
     */
    @Test
    void theDocumentNumberIsAllocatedAfterEveryGuardThatCanRefuseTheCreate() throws Exception {
        String rendered = render(DAO_BASE, "Repository.java.template", context());

        int required = rendered.indexOf("throw new ValidationException(\"VacationRequest.FromDate is required\")");
        int allocation = rendered.indexOf("DocumentNumbers.next(");
        int insert = rendered.indexOf("super.save(entity,");
        assertTrue(required >= 0, "the repository must still refuse a missing required value: " + rendered);
        assertTrue(allocation > required, "a refused create must not burn a number: " + rendered);
        assertTrue(insert > allocation, "the number must be stamped on the row that is inserted: " + rendered);
    }

    /**
     * ...and the number column, being NOT NULL and filled a few lines below, must not be refused by the
     * required check it now precedes - that would reject every create of a numbered document.
     */
    @Test
    void theNumberColumnIsNotItselfRefusedByTheRequiredCheck() throws Exception {
        String rendered = render(DAO_BASE, "Repository.java.template", context());

        assertTrue(!rendered.contains("VacationRequest.Number is required"),
                "the platform fills the number itself - it can never be the caller's omission: " + rendered);
    }

    /**
     * A required value the platform itself supplies is not the caller's to send: the number (allocated
     * on insert) and a uuid would otherwise be refused on every surface, naming a field no form has.
     */
    @Test
    void noSurfaceDemandsAValueThePlatformSuppliesItself() throws Exception {
        for (String template : List.of("EntityController.java.template", "EntityMyController.java.template",
                "EntityPartnerController.java.template")) {
            String rendered = render(template, context());

            assertTrue(!rendered.contains("The 'Number' property is required"),
                    template + " must not demand the number it does not receive: " + rendered);
            assertTrue(rendered.contains("The 'FromDate' property is required"), template + " must still demand what the caller does send");
        }
    }

    private String render(String templateName, Map<String, Object> parameters) throws Exception {
        return render(REST_BASE, templateName, parameters);
    }

    private String render(String base, String templateName, Map<String, Object> parameters) throws Exception {
        String location = base + templateName;
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** A numbered, personally owned document: the shape the defect was observed on. */
    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "VacationRequest");
        parameters.put("projectName", "vacations");
        parameters.put("perspectiveName", "VacationRequest");
        parameters.put("javaGenFolderName", "vacations");
        parameters.put("javaPerspectiveName", "vacationrequest");
        parameters.put("tablePrefix", "VACATIONS_");
        parameters.put("dataName", "VACATION_REQUEST");
        parameters.put("pkPropertyName", "Id");
        parameters.put("properties", List.of(primaryKey(), number(), fromDate(), note()));
        parameters.put("sensitiveProperties", List.of());
        parameters.put("rowChecks", List.of(rowCheck()));
        parameters.put("personalProperty", "Employee");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.vacations.data.vacationrequest.EmployeeRepository");
        parameters.put("partnerProperty", "Employee");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.vacations.data.vacationrequest.EmployeeRepository");
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Id");
        property.put("dataName", "VACATION_REQUEST_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        property.put("dataNotNull", Boolean.TRUE);
        return property;
    }

    /** intent `number: { series: Vacation Request, stampOn: create }` on a required column. */
    private static Map<String, Object> number() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Number");
        property.put("dataName", "VACATION_REQUEST_NUMBER");
        property.put("dataType", "VARCHAR");
        property.put("dataTypeJavaClass", "String");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataNotNull", Boolean.TRUE);
        property.put("isRequiredProperty", "true");
        property.put("numberSeries", "Vacation Request");
        property.put("numberPer", "");
        property.put("numberStampOnCreate", "true");
        return property;
    }

    /** The field the live defect was observed on: `required: true`, supplied by the caller. */
    private static Map<String, Object> fromDate() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "FromDate");
        property.put("dataName", "VACATION_REQUEST_FROM_DATE");
        property.put("dataType", "DATE");
        property.put("dataTypeJavaClass", "java.time.LocalDate");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataNotNull", Boolean.TRUE);
        property.put("isRequiredProperty", "true");
        return property;
    }

    private static Map<String, Object> note() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Note");
        property.put("dataName", "VACATION_REQUEST_NOTE");
        property.put("dataType", "VARCHAR");
        property.put("dataTypeJavaClass", "String");
        property.put("dataPrimaryKey", Boolean.FALSE);
        property.put("dataLength", 40);
        property.put("widgetPattern", "^[A-Za-z ]*$");
        property.put("widgetPatternJava", "^[A-Za-z ]*$");
        return property;
    }

    /**
     * intent `checks: exactlyOne` - a row-level refusal, the same one the power surface applies. The
     * escaped twin is the key the surfaces read at the Java site (#7241); the raw message stays for the
     * surfaces that render it as text.
     */
    private static Map<String, Object> rowCheck() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("fields", List.of("FromDate", "Note"));
        check.put("message", "exactly one of FromDate / Note must be set");
        check.put("messageJavaLiteral", "exactly one of FromDate / Note must be set");
        return check;
    }
}
