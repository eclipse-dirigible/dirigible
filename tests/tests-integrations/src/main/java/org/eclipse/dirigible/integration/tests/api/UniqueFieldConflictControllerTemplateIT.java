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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * A single-column {@code unique: true} must answer a duplicate with the same kind of 4xx a
 * {@code required:} miss already answers with - not the HTTP 500 carrying the raw JDBC text that
 * the generated controller used to hand back (#7098) - and it must name the field the caller
 * actually collided on (#7138).
 *
 * <p>
 * A composite key is easy to map because the model NAMES the constraint. A single-column unique is
 * left on the column for the database to name, so the only handle is the column name the dialect
 * builds that generated name out of. That premise is what makes the mapping work, so it is asserted
 * against the driver text actually observed on PostgreSQL and H2 rather than assumed - including
 * the statement both of them append to their message, which lists every column of the table and so
 * matches every business key the entity has.
 *
 * <p>
 * The mapping lives ONCE, in the generated repository (#8661): the key map is rendered there and
 * the platform's {@code JavaRepository} matches a violation against it on every write, so a
 * generate action, a schedule and the three controller surfaces all refuse a collision with the
 * same message. It used to be three hand-written copies in the controllers - which drifted (#7176)
 * and which no other writer had, so a repeated generate action answered the driver's text as a 500.
 * The rendered map is therefore extracted from the repository template, compiled into a
 * {@code JavaRepository} and RUN here against real exception chains; the controllers are checked
 * for handing their writes to it and carrying no copy of their own. Rendering and compiling need
 * nothing from a running instance, so this boots no application context and uses its own engine
 * instance, exactly like {@code RoleScopedFieldControllerTemplateIT}, whose fixture shape this
 * mirrors.
 */
class UniqueFieldConflictControllerTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";
    private static final String TEMPLATE = BASE + "EntityController.java.template";
    private static final String REPOSITORY_TEMPLATE = "/META-INF/dirigible/template-application-dao-java/data/Repository.java.template";

    /** What a controller write path hands a failure to: the repository's own mapping (#8661). */
    private static final String HAND_OFF = "throw repository.duplicateKeyOr(e);";

    /** The two doors a person actually types into - #7137. */
    private static final List<String> SELF_SERVICE_SURFACES =
            List.of("EntityMyController.java.template", "EntityPartnerController.java.template");

    /** Every surface a write reaches the entity through. */
    private static final List<String> SURFACES =
            List.of("EntityController.java.template", "EntityMyController.java.template", "EntityPartnerController.java.template");

    /** SQLSTATE 23505 - a uniqueness violation, which a primary-key collision also reports. */
    private static final String UNIQUE_VIOLATION = "23505";

    /** SQLSTATE 23503 - a foreign-key violation, a different conflict on the same column. */
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    /** Hibernate re-reports every failure with the executed statement attached. */
    private static final String HIBERNATE_INSERT =
            " [insert into VACATIONS_PUBLIC_HOLIDAY (PUBLIC_HOLIDAY_DAY,PUBLIC_HOLIDAY_DAY_OF_NOTICE,PUBLIC_HOLIDAY_NAME,PUBLIC_HOLIDAY_ID) values (?,?,?,?)]";

    /** The message observed on BusinessIntents STA (PostgreSQL) in the report - #7098. */
    private static final String POSTGRES_DUPLICATE =
            "ERROR: duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key\"\n"
                    + "  Detail: Key (\"PUBLIC_HOLIDAY_DAY\")=(2026-09-07) already exists.";

    /** The same collision on the default H2 datasource, which names the index, not the column. */
    private static final String H2_DUPLICATE = "Unique index or primary key violation: "
            + "\"CONSTRAINT_INDEX_7 ON PUBLIC.VACATIONS_PUBLIC_HOLIDAY(PUBLIC_HOLIDAY_DAY NULLS FIRST) VALUES ( /* 1 */ DATE '2026-09-07' )\"; SQL statement:\n"
            + "insert into VACATIONS_PUBLIC_HOLIDAY (PUBLIC_HOLIDAY_DAY,PUBLIC_HOLIDAY_NAME,PUBLIC_HOLIDAY_ID) values (?,?,?) [23505-232]";

    /** A different conflict on the same column: a missing reference, not a duplicate. */
    private static final String POSTGRES_FOREIGN_KEY =
            "ERROR: insert or update on table \"vacations_vacation\" violates foreign key constraint "
                    + "\"VACATION_PUBLIC_HOLIDAY_DAY_FK\"\n  Detail: Key (PUBLIC_HOLIDAY_DAY)=(2026-09-07) is not present in table \"vacations_public_holiday\".";

    /** A primary-key collision, which PostgreSQL reports with the very same SQLSTATE. */
    private static final String POSTGRES_PRIMARY_KEY =
            "ERROR: duplicate key value violates unique constraint \"vacations_public_holiday_pkey\"\n"
                    + "  Detail: Key (PUBLIC_HOLIDAY_ID)=(5) already exists.";

    /**
     * The authored message of the composite key, quoting the field it is about - the shape the DSL's
     * own examples suggest, and the one that ends the Java literal it lands in unless the twin the
     * processor derives is what the template writes (#7241).
     */
    private static final String COMPOSITE_KEY_MESSAGE = "This \"day\" is already a holiday of the company";

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void aUniqueFieldsCollisionIsAnsweredWithAConflictThatNamesTheField() throws Exception {
        Mapping mapping = mapping(context());

        assertEquals("A PublicHoliday with this 'Day' already exists", mapping.answerFor(POSTGRES_DUPLICATE, UNIQUE_VIOLATION),
                "the PostgreSQL duplicate must map to the message naming the field");
        assertEquals("A PublicHoliday with this 'Day' already exists", mapping.answerFor(H2_DUPLICATE, UNIQUE_VIOLATION),
                "and so must the H2 one, which names the index instead of the constraint");
    }

    /**
     * The statement the drivers append lists EVERY column of the table, so a message read whole matches
     * every business key the entity has - and with two of them the answer named whichever key the map
     * happened to iterate first, which on equal-length column names is alphabetical order (#7138).
     */
    @Test
    void theCollidedFieldIsTheOneNamedEvenWhenTheStatementListsTheOthers() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), uniqueDay(), unique("DayOfNotice", "PUBLIC_HOLIDAY_DAY_OF_NOTICE")));
        Mapping mapping = mapping(context);

        assertEquals("A PublicHoliday with this 'Day' already exists",
                mapping.answerFor(POSTGRES_DUPLICATE + HIBERNATE_INSERT, POSTGRES_DUPLICATE, UNIQUE_VIOLATION),
                "the statement's column list must not decide which field the collision is reported on");
        assertEquals("A PublicHoliday with this 'DayOfNotice' already exists", mapping.answerFor(
                "ERROR: duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_OF_NOTICE_key\""
                        + HIBERNATE_INSERT,
                "ERROR: duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_OF_NOTICE_key\"",
                UNIQUE_VIOLATION), "and the other field must be answered on its own collision");
    }

    /**
     * A primary-key collision reports SQLSTATE 23505 too, and its statement tail carries every
     * business-key column - so it used to come back as a duplicate of a field the caller never wrote.
     * It names no business key, so it is not this mapping's conflict to answer.
     */
    @Test
    void aPrimaryKeyCollisionIsNotAnsweredAsABusinessKeyDuplicate() throws Exception {
        Mapping mapping = mapping(context());

        assertNull(mapping.answerFor(POSTGRES_PRIMARY_KEY + HIBERNATE_INSERT, POSTGRES_PRIMARY_KEY, UNIQUE_VIOLATION),
                "a primary-key violation must be rethrown, not renamed after a business key");
    }

    /**
     * Not every dialect says "duplicate" or "unique" in words - SQLSTATE 23505 is the portable half of
     * the discriminator, and a violation that says neither is not one of these conflicts at all.
     */
    @Test
    void theSqlStateIsHonouredOnItsOwnAndIsTheOnlyOtherHandle() throws Exception {
        Mapping mapping = mapping(context());
        String wordless = "constraint violation on VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key";

        assertEquals("A PublicHoliday with this 'Day' already exists", mapping.answerFor(wordless, UNIQUE_VIOLATION),
                "SQLSTATE 23505 alone must be enough to recognise the duplicate");
        assertNull(mapping.answerFor(wordless, "23000"),
                "and a violation that neither reports 23505 nor says so in words is not a duplicate");
    }

    /**
     * The same column appears in a foreign-key violation's text, and that is a different conflict - the
     * generated delete has its own answer for it. Answering it as a duplicate would name a field the
     * caller did not collide on.
     */
    @Test
    void aForeignKeyViolationOnTheSameColumnIsNotADuplicate() throws Exception {
        Mapping mapping = mapping(context());

        assertNull(mapping.answerFor(POSTGRES_FOREIGN_KEY + HIBERNATE_INSERT, POSTGRES_FOREIGN_KEY, FOREIGN_KEY_VIOLATION),
                "a missing reference must not be answered as a duplicate");
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
        Mapping mapping = mapping(context);

        assertEquals("A PublicHoliday with this 'DayOfNotice' already exists",
                mapping.answerFor(
                        "duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_OF_NOTICE_key\"",
                        UNIQUE_VIOLATION),
                "the longer column must not be answered with the shorter one's message");
    }

    /** The composite-key mapping the single-column case joins must keep behaving as it did. */
    @Test
    void aCompositeKeyStillAnswersWithItsAuthoredMessage() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), column("Company", "PUBLIC_HOLIDAY_COMPANY"), column("Day", "PUBLIC_HOLIDAY_DAY")));
        context.put("uniqueConstraints", List.of(compositeKey()));

        assertEquals(COMPOSITE_KEY_MESSAGE,
                mapping(context).answerFor("duplicate key value violates unique constraint \"PublicHoliday_Company_Day\"",
                        UNIQUE_VIOLATION),
                "the authored message must still be the answer for the constraint the model named");
        assertNoUnresolvedReferences(render(REPOSITORY_TEMPLATE, context));
    }

    /**
     * Every controller surface hands what its writes raised to the repository's mapping, and carries no
     * copy of its own: three hand-written copies only stayed one behaviour while they stayed one text
     * (#7176), and no copy at all reached the other writers - a generate action creating its second
     * document for the same key answered 500 (#8661).
     */
    @Test
    void everySurfaceHandsItsWritesToTheRepositorysMapping() throws Exception {
        Map<String, Object> context = selfServiceContext();
        context.put("uniqueConstraints", List.of(compositeKey()));

        for (String template : SURFACES) {
            String rendered = render(BASE + template, context);

            assertEquals(2, occurrences(rendered, HAND_OFF), template + " must hand both the create and the update over: " + rendered);
            assertFalse(rendered.contains("isDuplicateViolation"), template + " must carry no matcher of its own: " + rendered);
            assertFalse(rendered.contains("DUPLICATE_REFUSALS"), template + " must carry no key map of its own: " + rendered);
        }
        assertTrue(render(TEMPLATE, context).contains("saved = repository.save(entity);"), "the create must save inside the try");
        for (String template : SELF_SERVICE_SURFACES) {
            String rendered = render(BASE + template, context);
            assertTrue(rendered.contains("return scrub(repository.save(entity));"), template + " must save inside the try");
            assertTrue(rendered.contains("return scrub(repository.update(entity));"), template + " must update inside the try");
        }
    }

    /**
     * The controllers are hand-written Java inside Velocity templates, and nothing in the build
     * compiles a rendered controller - a stray brace would only surface as a broken generated
     * application. The compiler's PARSE phase is the gate that fits here: it reports the syntax of the
     * rendered source without needing the SDK types it imports on the classpath.
     */
    @Test
    void everyRenderedShapeIsSyntacticallyValidJava() throws Exception {
        Map<String, Object> composite = selfServiceContext();
        composite.put("uniqueConstraints", List.of(compositeKey()));
        Map<String, Object> noKey = selfServiceContext();
        noKey.put("properties", List.of(primaryKey(), column("Name", "PUBLIC_HOLIDAY_NAME")));
        Map<String, Object> readOnly = selfServiceContext();
        readOnly.put("personalReadOnly", "true");

        for (Map<String, Object> context : List.of(selfServiceContext(), composite, noKey, readOnly)) {
            for (String template : SURFACES) {
                assertEquals(List.of(), syntaxErrors(render(BASE + template, context)), template + " must parse as Java");
            }
        }
    }

    /** An entity with no business key at all must come out exactly as it always did. */
    @Test
    void anEntityWithoutAnyBusinessKeyGetsNoneOfIt() throws Exception {
        Map<String, Object> context = selfServiceContext();
        context.put("properties", List.of(primaryKey(), column("Name", "PUBLIC_HOLIDAY_NAME")));

        assertFalse(render(REPOSITORY_TEMPLATE, context).contains("duplicateKeys()"), "the repository has no key to map");
        for (String template : SURFACES) {
            assertFalse(render(BASE + template, context).contains("duplicateKeyOr"), template + " has nothing to hand over");
        }
        assertTrue(render(TEMPLATE, context).contains("PublicHolidayEntity saved = repository.save(entity);"),
                "the unwrapped save must be the one emitted");
        for (String template : SELF_SERVICE_SURFACES) {
            assertTrue(render(BASE + template, context).contains("return scrub(repository.save(entity));"),
                    template + " must keep the unwrapped save");
        }
    }

    /** A see-only personal surface refuses every write with 403, so there is no collision to map. */
    @Test
    void aReadOnlyPersonalSurfaceGetsNoneOfIt() throws Exception {
        Map<String, Object> context = selfServiceContext();
        context.put("personalReadOnly", "true");

        String rendered = render(BASE + "EntityMyController.java.template", context);

        assertFalse(rendered.contains("duplicateKeyOr"), "no write reaches a statement here: " + rendered);
    }

    /**
     * The rendered key map, compiled into a {@code JavaRepository} and run through the platform's
     * {@code duplicateKeyOr} - the very path every write of a generated repository takes - so a test
     * runs the mapping instead of restating its rule.
     */
    private static final class Mapping {

        private static final String HARNESS = """
                public class DuplicateMapping extends org.eclipse.dirigible.components.data.store.java.repository.JavaRepository<Object> {

                    public DuplicateMapping() {
                        super(Object.class);
                    }

                    /** A Hibernate ConstraintViolationException stands in by the only thing the mapping reads: its name. */
                    static final class StubConstraintViolationException extends RuntimeException {
                        StubConstraintViolationException(String message, Throwable cause) {
                            super(message, cause);
                        }
                    }

                    /** The message the mapping answers with, or null when it left the violation untouched. */
                    public static String answer(String hibernateMessage, String driverMessage, String sqlState) {
                        Throwable driver = driverMessage == null ? null : new java.sql.SQLException(driverMessage, sqlState);
                        RuntimeException violation = new StubConstraintViolationException(hibernateMessage, driver);
                        RuntimeException answered = new DuplicateMapping().duplicateKeyOr(violation);
                        return answered == violation ? null : answered.getMessage();
                    }

                %s
                }
                """;

        private final Method answer;

        private Mapping(String rendered) throws Exception {
            String source = HARNESS.formatted(mappingSource(rendered));
            this.answer = compile(source).getMethod("answer", String.class, String.class, String.class);
        }

        /** The answer for a violation whose driver message is also the one Hibernate re-reported. */
        private String answerFor(String driverMessage, String sqlState) throws Exception {
            return answerFor(driverMessage, driverMessage, sqlState);
        }

        private String answerFor(String hibernateMessage, String driverMessage, String sqlState) throws Exception {
            return (String) answer.invoke(null, hibernateMessage, driverMessage, sqlState);
        }

        /**
         * The rendered members from the key map down to the end of the {@code duplicateKeys()} override.
         */
        private static String mappingSource(String rendered) {
            int from = rendered.indexOf(
                    "    private static final java.util.Map<String, java.util.function.Supplier<org.eclipse.dirigible.sdk.db.ValidationException>> DUPLICATE_KEYS");
            assertTrue(from > 0, "the rendered repository carries no key map: " + rendered);
            int last = rendered.indexOf(
                    "protected java.util.Map<String, java.util.function.Supplier<org.eclipse.dirigible.sdk.db.ValidationException>> duplicateKeys() {",
                    from);
            assertTrue(last > 0, "the rendered repository does not hand its key map to the platform: " + rendered);
            return rendered.substring(from, endOfBlock(rendered, rendered.indexOf('{', last)) + 1);
        }

        private static int endOfBlock(String source, int open) {
            int depth = 0;
            for (int at = open; at < source.length(); at++) {
                char character = source.charAt(at);
                if (character == '{') {
                    depth++;
                } else if (character == '}' && --depth == 0) {
                    return at;
                }
            }
            return fail("the rendered method is not closed: " + source.substring(open));
        }

        private static Class<?> compile(String source) throws Exception {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler, "the tests must run on a JDK - there is no system Java compiler");
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            Map<String, ByteArrayOutputStream> compiled = new HashMap<>();
            JavaFileObject unit = sourceFile("DuplicateMapping", source);
            try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, null);
                    ForwardingJavaFileManager<StandardJavaFileManager> files = collectingInto(standard, compiled)) {
                boolean compiledCleanly = compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null, List.of(unit))
                                                  .call();
                assertTrue(compiledCleanly, "the extracted mapping must compile: " + diagnostics.getDiagnostics() + "\n" + source);
            }
            ClassLoader loader = new ClassLoader(Mapping.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    ByteArrayOutputStream bytes = compiled.get(name);
                    if (bytes == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] bytecode = bytes.toByteArray();
                    return defineClass(name, bytecode, 0, bytecode.length);
                }
            };
            return loader.loadClass("DuplicateMapping");
        }

        private static ForwardingJavaFileManager<StandardJavaFileManager> collectingInto(StandardJavaFileManager standard,
                Map<String, ByteArrayOutputStream> compiled) {
            return new ForwardingJavaFileManager<>(standard) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                        FileObject sibling) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    compiled.put(className, bytes);
                    return new SimpleJavaFileObject(URI.create("bytes:///" + className.replace('.', '/') + kind.extension), kind) {
                        @Override
                        public OutputStream openOutputStream() {
                            return bytes;
                        }
                    };
                }
            };
        }
    }

    private static JavaFileObject sourceFile(String name, String content) {
        return new SimpleJavaFileObject(URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return content;
            }
        };
    }

    /** The syntax diagnostics of the rendered source - the parse phase reports nothing else. */
    private static List<String> syntaxErrors(String rendered) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "the tests must run on a JDK - there is no system Java compiler");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, List.of("-proc:none"), null,
                List.of(sourceFile("PublicHolidayController", rendered)));
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

    private Mapping mapping(Map<String, Object> parameters) throws Exception {
        return new Mapping(render(REPOSITORY_TEMPLATE, parameters));
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

    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("refusals.put") || line.contains("already exists")) {
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

    /**
     * The shape {@code EdmIntentGenerator} plus {@code ModelParameterProcessor} put on a composite
     * {@code unique:} declaration. The message carries a quote on purpose: the name and the message are
     * both written into Java string literals, so only the escaped twins the processor derives (#7241)
     * leave the mapping compilable - and this test compiles what it renders.
     */
    private static Map<String, Object> compositeKey() {
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("name", "PublicHoliday_Company_Day");
        constraint.put("nameJavaLiteral", "PublicHoliday_Company_Day");
        constraint.put("columns", List.of(Map.of("name", "PUBLIC_HOLIDAY_COMPANY"), Map.of("name", "PUBLIC_HOLIDAY_DAY")));
        constraint.put("message", COMPOSITE_KEY_MESSAGE);
        constraint.put("messageJavaLiteral", COMPOSITE_KEY_MESSAGE.replace("\"", "\\\""));
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

    /** The reported entity, owned by an identity - what the personal and partner templates ask for. */
    private static Map<String, Object> selfServiceContext() {
        Map<String, Object> parameters = context();
        parameters.put("personalProperty", "Employee");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.vacations.data.vacations.EmployeeRepository");
        parameters.put("partnerProperty", "Employee");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.vacations.data.vacations.EmployeeRepository");
        return parameters;
    }
}
