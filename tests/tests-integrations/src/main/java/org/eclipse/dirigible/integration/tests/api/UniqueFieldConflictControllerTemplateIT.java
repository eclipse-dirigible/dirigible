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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * A single-column {@code unique: true} must answer a duplicate with the same kind of 4xx a
 * {@code required:} miss already answers with - not the HTTP 500 carrying the raw JDBC text that
 * the generated controller used to hand back (#7098).
 *
 * <p>
 * A composite key is easy to map because the model NAMES the constraint. A single-column unique is
 * left on the column for the database to name, so the only handle is the column name the dialect
 * builds that generated name out of. That premise is what makes the mapping work, so it is asserted
 * against the driver text actually observed on PostgreSQL and H2 rather than assumed: the entries
 * the template emits are read back out of the rendered source and applied to those real messages by
 * the same rule the rendered {@code duplicateOrRethrow} applies.
 *
 * <p>
 * Rendering needs nothing from a running instance, so this boots no application context and uses
 * its own engine instance, exactly like {@code RoleScopedFieldControllerTemplateIT}, whose fixture
 * shape this mirrors.
 */
class UniqueFieldConflictControllerTemplateIT {

    private static final String TEMPLATE = "/META-INF/dirigible/template-application-rest-java/api/EntityController.java.template";

    /** The message observed on BusinessIntents STA (PostgreSQL) in the report - #7098. */
    private static final String POSTGRES_DUPLICATE = "could not execute statement [ERROR: duplicate key value violates unique constraint "
            + "\"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key\" Detail: Key (\"PUBLIC_HOLIDAY_DAY\")=(2026-09-07) "
            + "already exists.] [insert into VACATIONS_PUBLIC_HOLIDAY ...]";

    /** The same collision on the default H2 datasource, which names the index, not the column. */
    private static final String H2_DUPLICATE = "Unique index or primary key violation: "
            + "\"CONSTRAINT_INDEX_7 ON PUBLIC.VACATIONS_PUBLIC_HOLIDAY(PUBLIC_HOLIDAY_DAY NULLS FIRST) VALUES ( /* 1 */ DATE '2026-09-07' )\"";

    /** A different conflict on the same column: referencing rows, not a duplicate. */
    private static final String POSTGRES_FOREIGN_KEY = "insert or update on table \"vacations_vacation\" violates foreign key constraint "
            + "\"VACATION_PUBLIC_HOLIDAY_DAY_FK\" Detail: Key (PUBLIC_HOLIDAY_DAY)=(2026-09-07) is not present in table ...";

    /** {@code messages.put("KEY".toUpperCase(Locale.ROOT), "message");} in the rendered source. */
    private static final Pattern EMITTED_ENTRY =
            Pattern.compile("messages\\.put\\(\"([^\"]+)\"\\.toUpperCase\\(Locale\\.ROOT\\), \"([^\"]+)\"\\);");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void aUniqueFieldsCollisionIsAnsweredWithAConflictThatNamesTheField() throws Exception {
        String rendered = render(context());

        assertEquals("A PublicHoliday with this 'Day' already exists", answerFor(rendered, POSTGRES_DUPLICATE),
                "the PostgreSQL duplicate must map to the message naming the field");
        assertEquals("A PublicHoliday with this 'Day' already exists", answerFor(rendered, H2_DUPLICATE),
                "and so must the H2 one, which names the index instead of the constraint");
    }

    /** The 500 in the report came from the write not being wrapped at all on either verb. */
    @Test
    void bothWritePathsRouteThroughTheMapping() throws Exception {
        String rendered = render(context());

        assertEquals(2, occurrences(rendered, "throw duplicateOrRethrow(e);"),
                "create and update must both hand the violation to the mapping: " + rendered);
        assertTrue(rendered.contains("saved = repository.save(entity);"), "the create must save inside the try");
        assertTrue(rendered.contains("return repository.update(entity);"), "the update must run inside the try");
        assertTrue(rendered.contains("if (isConstraintViolation(e) && isDuplicateViolation(e)) {"),
                "the mapping must answer only for a UNIQUENESS violation: " + rendered);
        assertNoUnresolvedReferences(rendered);
    }

    /**
     * The same column appears in a foreign-key violation's text, and that is a different conflict - the
     * generated delete has its own answer for it. Answering it as a duplicate would name a field the
     * caller did not collide on.
     */
    @Test
    void aForeignKeyViolationOnTheSameColumnIsNotADuplicate() throws Exception {
        String rendered = render(context());

        assertTrue(rendered.contains("upper.contains(\"DUPLICATE\") || upper.contains(\"UNIQUE\")"),
                "the discriminator must be the violation's own class, not the column: " + rendered);
        assertTrue(rendered.contains("\"23505\".equals(sqlException.getSQLState())"), "SQLSTATE 23505 must be honoured too");
        assertFalse(POSTGRES_FOREIGN_KEY.toUpperCase(Locale.ROOT)
                                        .contains("DUPLICATE"),
                "the fixture must not be a duplicate by text either - otherwise it proves nothing");
    }

    /**
     * A column name can be a prefix of a longer one on the same table, and both keys then match the
     * same message. The more specific one is the one the caller collided on.
     */
    @Test
    void theMoreSpecificColumnWins() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), uniqueDay(), unique("DayOfNotice", "PUBLIC_HOLIDAY_DAY_OF_NOTICE")));

        String rendered = render(context);

        assertEquals("A PublicHoliday with this 'DayOfNotice' already exists",
                answerFor(rendered,
                        "duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_OF_NOTICE_key\""),
                "the longer column must not be answered with the shorter one's message");
        assertTrue(rendered.contains("Comparator.comparingInt(String::length)"), "the ordering must be part of the emitted map");
    }

    /** The composite-key mapping the single-column case joins must keep behaving as it did. */
    @Test
    void aCompositeKeyStillAnswersWithItsAuthoredMessage() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), column("Company", "PUBLIC_HOLIDAY_COMPANY"), column("Day", "PUBLIC_HOLIDAY_DAY")));
        context.put("uniqueConstraints", List.of(compositeKey()));

        String rendered = render(context);

        assertEquals("This day is already a holiday of the company",
                answerFor(rendered, "duplicate key value violates unique constraint \"PublicHoliday_Company_Day\""),
                "the authored message must still be the answer for the constraint the model named");
        assertNoUnresolvedReferences(rendered);
    }

    /**
     * The mapping is hand-written Java inside a Velocity template, and nothing in the build compiles a
     * rendered controller - a stray brace would only surface as a broken generated application. The
     * compiler's PARSE phase is the gate that fits here: it reports the syntax of the rendered source
     * without needing the SDK types it imports on the classpath.
     */
    @Test
    void everyRenderedShapeIsSyntacticallyValidJava() throws Exception {
        Map<String, Object> composite = context();
        composite.put("uniqueConstraints", List.of(compositeKey()));
        Map<String, Object> noKey = context();
        noKey.put("properties", List.of(primaryKey(), column("Name", "PUBLIC_HOLIDAY_NAME")));

        for (Map<String, Object> context : List.of(context(), composite, noKey)) {
            assertEquals(List.of(), syntaxErrors(render(context)), "the rendered controller must parse as Java");
        }
    }

    /** An entity with no business key at all must come out exactly as it always did. */
    @Test
    void anEntityWithoutAnyBusinessKeyGetsNoneOfIt() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), column("Name", "PUBLIC_HOLIDAY_NAME")));

        String rendered = render(context);

        assertFalse(rendered.contains("duplicateOrRethrow"), "there is nothing to map: " + rendered);
        assertFalse(rendered.contains("DUPLICATE_MESSAGES"), "and no map to carry");
        assertTrue(rendered.contains("${name}Entity saved = repository.save(entity);".replace("${name}", "PublicHoliday")),
                "the unwrapped save must be the one emitted");
    }

    /**
     * Applies the rendered mapping to a driver message exactly as the rendered
     * {@code duplicateOrRethrow} does - the entries come out of the generated source, so the fixture
     * cannot drift from what the template emits.
     */
    private static String answerFor(String rendered, String driverMessage) {
        String upper = driverMessage.toUpperCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : emittedMessages(rendered).entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** The emitted keys, in the longest-first order the rendered map iterates them in. */
    private static Map<String, String> emittedMessages(String rendered) {
        Map<String, String> messages = new TreeMap<>(Comparator.comparingInt(String::length)
                                                               .reversed()
                                                               .thenComparing(Comparator.naturalOrder()));
        Matcher matcher = EMITTED_ENTRY.matcher(rendered);
        while (matcher.find()) {
            messages.put(matcher.group(1)
                                .toUpperCase(Locale.ROOT),
                    matcher.group(2));
        }
        assertFalse(messages.isEmpty(), "the template emitted no duplicate messages at all: " + rendered);
        return messages;
    }

    /** The syntax diagnostics of the rendered source - the parse phase reports nothing else. */
    private static List<String> syntaxErrors(String rendered) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "the tests must run on a JDK - there is no system Java compiler");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///PublicHolidayController.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return rendered;
            }
        };
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, List.of("-proc:none"), null, List.of(source));
        ((JavacTask) task).parse();
        return diagnostics.getDiagnostics()
                          .stream()
                          .filter(diagnostic -> diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                          .map(diagnostic -> diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT))
                          .toList();
    }

    private static int occurrences(String rendered, String needle) {
        int count = 0;
        for (int at = rendered.indexOf(needle); at >= 0; at = rendered.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    private String render(Map<String, Object> parameters) throws Exception {
        String template;
        try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
            assertNotNull(in, "template resource not found on classpath: " + TEMPLATE);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, TEMPLATE, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("messages.put") || line.contains("duplicateOrRethrow") || line.contains("already exists")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived: " + line);
            }
        }
    }

    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "PublicHoliday");
        parameters.put("projectName", "vacations");
        parameters.put("perspectiveName", "Vacations");
        parameters.put("javaGenFolderName", "vacations");
        parameters.put("javaPerspectiveName", "vacations");
        parameters.put("tablePrefix", "VACATIONS_");
        parameters.put("dataName", "PUBLIC_HOLIDAY");
        parameters.put("properties", List.of(primaryKey(), uniqueDay(), column("Name", "PUBLIC_HOLIDAY_NAME")));
        parameters.put("sensitiveProperties", new ArrayList<>());
        return parameters;
    }

    /** The shape {@code EdmIntentGenerator} puts on a composite {@code unique:} declaration. */
    private static Map<String, Object> compositeKey() {
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("name", "PublicHoliday_Company_Day");
        constraint.put("columns", List.of(Map.of("name", "PUBLIC_HOLIDAY_COMPANY"), Map.of("name", "PUBLIC_HOLIDAY_DAY")));
        constraint.put("message", "This day is already a holiday of the company");
        return constraint;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = column("Id", "PUBLIC_HOLIDAY_ID");
        property.put("dataType", "INTEGER");
        property.put("dataTypeJavaClass", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    /** The reported field: intent {@code day: { type: date, unique: true }}. */
    private static Map<String, Object> uniqueDay() {
        return unique("Day", "PUBLIC_HOLIDAY_DAY");
    }

    private static Map<String, Object> unique(String name, String dataName) {
        Map<String, Object> property = column(name, dataName);
        property.put("dataUnique", "true");
        return property;
    }

    private static Map<String, Object> column(String name, String dataName) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", dataName);
        property.put("dataType", "VARCHAR");
        property.put("dataTypeJavaClass", "String");
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
