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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.ImportCamt053Intent;
import org.eclipse.dirigible.components.intent.model.ImportCsvIntent;
import org.eclipse.dirigible.components.intent.model.ImportIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.sdk.imports.Camt053;
import org.eclipse.dirigible.sdk.imports.Mt940;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code imports:} construct - a file a user uploads (or a job hands over) read into the rows
 * of an entity - shared by the parser, which validates it, and the glue generator, which plans the
 * generated {@code <Name>Import} class from it. What the parser accepts and what the generator
 * emits are the same grammar, so both read it here.
 *
 * <p>
 * The statement vocabularies are the SDK readers' own ({@link Mt940#KEYS}, {@link Camt053#KEYS}): a
 * key the parser accepted is a key the reader produces, by construction.
 */
public final class ImportSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportSupport.class);

    /** A delimited text file read by the declared locale rules. */
    static final String CSV = "csv";
    /** A SWIFT MT940 customer statement. */
    static final String MT940 = "mt940";
    /** An ISO 20022 camt.053 bank-to-customer statement. */
    static final String CAMT053 = "camt053";
    private static final Set<String> FORMATS = Set.of(CSV, MT940, CAMT053);

    /** The source key of a record's 1-based position in the file, for any format. */
    static final String LINE = "_line";
    /** The source key of the uploaded file's name, for any format. */
    static final String FILE = "_file";
    /** The source key of the uploaded file's SHA-256 checksum, for any format. */
    static final String CHECKSUM = "_checksum";

    /** {@code camt053: { records: entries }} - one record per {@code Ntry}, the default. */
    static final String ENTRIES = "entries";
    /** {@code camt053: { records: transactions }} - one record per {@code Ntry/NtryDtls/TxDtls}. */
    static final String TRANSACTIONS = "transactions";

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final String DEFAULT_SEPARATOR = ",";
    private static final String DEFAULT_DECIMAL = ".";

    private ImportSupport() {}

    /**
     * The entities some import writes into - each gets a sibling import ledger table.
     *
     * @param model the parsed intent
     * @return the entity names, in declaration order
     */
    public static Set<String> targets(IntentModel model) {
        Set<String> targets = new LinkedHashSet<>();
        for (ImportIntent declared : model.getImports()) {
            if (declared.getInto() != null) {
                targets.add(declared.getInto());
            }
        }
        return targets;
    }

    /**
     * Validate every declared import. An import must name a local entity whose rows it can create on
     * its own, read a known format by rules that parse, and map onto real fields from keys the format
     * produces - so a mistake fails at Generate rather than on the first uploaded file.
     *
     * @param model the parsed intent
     * @param issues the issue sink
     */
    public static void validate(IntentModel model, List<String> issues) {
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        Set<String> names = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (ImportIntent declared : model.getImports()) {
            String name = declared.getName();
            if (name == null || name.isBlank()) {
                issues.add("import has no name");
                continue;
            }
            String subject = "import [" + name + "]";
            if (!names.add(name)) {
                issues.add("duplicate import [" + name + "]");
            } else if (!classNames.add(IntentNaming.pascalCase(name))) {
                issues.add(subject + " generates the class " + IntentNaming.pascalCase(name)
                        + "Import, which another import's name generates too - rename one");
            }
            EntityIntent into = declared.getInto() == null ? null : byName.get(declared.getInto());
            if (into == null) {
                issues.add(subject + " imports into unknown entity [" + declared.getInto() + "]");
            } else {
                validateTarget(into, compositionParents, subject, issues);
            }
            String format = declared.getFormat();
            if (format == null || !FORMATS.contains(format)) {
                issues.add(subject + " has format [" + format + "] - use one of csv, mt940, camt053");
                continue;
            }
            validateEncoding(declared, format, subject, issues);
            validateCsv(declared, format, subject, issues);
            validateCamt053(declared, format, subject, issues);
            validateWhere(declared, format, subject, issues);
            if (into != null) {
                validateMap(declared, format, into, subject, issues);
                validateDuplicateKey(declared, subject, issues);
            }
        }
    }

    /**
     * An import creates the rows of its target and fills nothing but their fields, so the target must
     * be a table of this model a row can be inserted into on its own.
     */
    private static void validateTarget(EntityIntent into, Map<String, String> compositionParents, String subject, List<String> issues) {
        if (into.getExtend() != null) {
            issues.add(subject + " imports into [" + into.getName() + "], which extends another model's entity and owns no table");
            return;
        }
        if (compositionParents.containsKey(into.getName())) {
            issues.add(subject + " imports into [" + into.getName() + "], a composition child - its rows are created under a ["
                    + compositionParents.get(into.getName()) + "], which an import does not fill");
            return;
        }
        for (RelationIntent relation : into.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && relation.isRequired()) {
                issues.add(subject + " imports into [" + into.getName() + "], whose relation [" + relation.getName()
                        + "] is required - an import fills fields only, so every row would be refused");
            }
        }
        if (IntentEntities.primaryKeyOf(into) == null) {
            issues.add(subject + " imports into [" + into.getName() + "], which declares no primary key");
        }
    }

    private static void validateEncoding(ImportIntent declared, String format, String subject, List<String> issues) {
        String encoding = declared.getEncoding();
        if (encoding == null) {
            return;
        }
        if (CAMT053.equals(format)) {
            issues.add(subject + " declares an encoding, but a camt053 file is XML and declares its own");
            return;
        }
        if (!supported(encoding)) {
            issues.add(subject + " has unknown encoding [" + encoding + "] - use a Java charset name such as UTF-8 or windows-1251");
        }
    }

    private static boolean supported(String encoding) {
        try {
            return !encoding.isBlank() && Charset.isSupported(encoding);
        } catch (IllegalCharsetNameException e) {
            LOGGER.debug("Charset name [{}] is not a legal name", LoggedValue.of(encoding), e);
            return false;
        }
    }

    private static void validateCsv(ImportIntent declared, String format, String subject, List<String> issues) {
        ImportCsvIntent csv = declared.getCsv();
        if (csv == null) {
            return;
        }
        if (!CSV.equals(format)) {
            issues.add(subject + " declares csv rules, but reads format " + format);
            return;
        }
        String separator = csv.getSeparator();
        if (separator != null && (separator.length() != 1 || separator.equals("\"") || separator.equals("\n") || separator.equals("\r"))) {
            issues.add(subject + " csv separator [" + separator + "] must be one character other than a quote or a line break");
        }
        String decimal = csv.getDecimal();
        if (decimal != null && (decimal.length() != 1 || Character.isDigit(decimal.charAt(0)))) {
            issues.add(subject + " csv decimal [" + decimal + "] must be one character other than a digit");
        }
        String grouping = csv.getGrouping();
        if (grouping != null && (grouping.length() != 1 || Character.isDigit(grouping.charAt(0)))) {
            issues.add(subject + " csv grouping [" + grouping + "] must be one character other than a digit");
        }
        if (grouping != null && grouping.equals(decimal == null ? DEFAULT_DECIMAL : decimal)) {
            issues.add(subject + " csv grouping and decimal are both [" + grouping + "] - a number could not be read");
        }
        validatePattern(csv.getDateFormat(), "dateFormat", subject, issues);
        validatePattern(csv.getTimestampFormat(), "timestampFormat", subject, issues);
        if (csv.getSkipLines() != null && csv.getSkipLines() < 0) {
            issues.add(subject + " csv skipLines [" + csv.getSkipLines() + "] must not be negative");
        }
    }

    private static void validateCamt053(ImportIntent declared, String format, String subject, List<String> issues) {
        ImportCamt053Intent camt053 = declared.getCamt053();
        if (camt053 == null) {
            return;
        }
        if (!CAMT053.equals(format)) {
            issues.add(subject + " declares camt053 rules, but reads format " + format);
            return;
        }
        String records = camt053.getRecords();
        if (records != null && !ENTRIES.equals(records) && !TRANSACTIONS.equals(records)) {
            issues.add(subject + " camt053 records [" + records + "] - use entries (one record per Ntry) or transactions (one per TxDtls)");
        }
    }

    /**
     * {@code where: { <source key>: <value> | [<value>, ...] }} - a record is imported only when every
     * named key holds one of its values; the rest are skipped and counted.
     */
    private static void validateWhere(ImportIntent declared, String format, String subject, List<String> issues) {
        boolean header = header(declared);
        for (Map.Entry<String, Object> entry : declared.getWhere()
                                                       .entrySet()) {
            String key = entry.getKey();
            String where = subject + " where [" + key + "]";
            if (key == null || key.isBlank()) {
                issues.add(subject + " where names a blank key");
                continue;
            }
            if (LINE.equals(key) || FILE.equals(key) || CHECKSUM.equals(key)) {
                issues.add(where + " filters on " + key + ", which is the same for every record or only a position - filter on a key"
                        + " the file's records carry");
                continue;
            }
            validateSource(key, declared, format, header, where, issues);
            if (whereValues(entry.getValue()).isEmpty()) {
                issues.add(where + " names no value - a scalar, or a list of scalars any of which the record may hold");
            }
        }
    }

    /**
     * The values a {@code where:} key allows, as the record's text holds them; empty when malformed.
     */
    private static List<String> whereValues(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                String text = scalar(item);
                if (text == null) {
                    return List.of();
                }
                values.add(text);
            }
            return values;
        }
        String text = scalar(value);
        return text == null ? List.of() : List.of(text);
    }

    private static String scalar(Object value) {
        if (value instanceof String text) {
            return text.isEmpty() ? null : text;
        }
        if (value instanceof Boolean flag) {
            return flag.toString();
        }
        if (value instanceof Number number) {
            return number.doubleValue() == Math.rint(number.doubleValue()) ? Long.toString(number.longValue()) : number.toString();
        }
        return null;
    }

    private static boolean perTransaction(ImportIntent declared) {
        return CAMT053.equals(declared.getFormat()) && declared.getCamt053() != null && TRANSACTIONS.equals(declared.getCamt053()
                                                                                                                    .getRecords());
    }

    private static void validatePattern(String pattern, String key, String subject, List<String> issues) {
        if (pattern == null) {
            return;
        }
        try {
            DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
        } catch (IllegalArgumentException e) {
            issues.add(subject + " csv " + key + " [" + pattern + "] is not a valid date-time pattern: " + e.getMessage());
        }
    }

    private static void validateMap(ImportIntent declared, String format, EntityIntent into, String subject, List<String> issues) {
        Map<String, Object> map = declared.getMap();
        if (map.isEmpty()) {
            issues.add(subject + " maps nothing - declare map: { <field>: <source> }");
            return;
        }
        boolean header = header(declared);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            FieldIntent field = fieldOf(into, entry.getKey());
            if (field == null) {
                issues.add(subject + " map [" + entry.getKey() + "] is not a field of [" + into.getName() + "]"
                        + (relationOf(into, entry.getKey()) != null ? " - an import fills fields only, not relations" : ""));
                continue;
            }
            if (field.isPrimaryKey()) {
                issues.add(subject + " map [" + entry.getKey() + "] fills the primary key, which is generated on insert");
                continue;
            }
            String source = source(entry.getValue());
            if (source == null) {
                issues.add(
                        subject + " map [" + entry.getKey() + "] names no source - a column, a statement key, _line, _file or _checksum");
                continue;
            }
            if (LINE.equals(source) || FILE.equals(source) || CHECKSUM.equals(source)) {
                continue;
            }
            validateSource(source, declared, format, header, subject + " map [" + entry.getKey() + "]", issues);
        }
        for (FieldIntent field : into.getFields()) {
            if (mustBeMapped(field) && !map.containsKey(field.getName())) {
                issues.add(subject + " does not map [" + field.getName() + "], a required field of [" + into.getName()
                        + "] with no default - every row would be refused");
            }
        }
    }

    private static void validateSource(String source, ImportIntent declared, String format, boolean header, String where,
            List<String> issues) {
        switch (format) {
            case CSV -> {
                if (!header && !position(source)) {
                    issues.add(where + " reads [" + source + "], but the file has no header - name a 1-based column position");
                }
            }
            case MT940 -> {
                if (!Mt940.KEYS.contains(source)) {
                    issues.add(where + " reads [" + source + "], which an mt940 statement line does not carry - use one of "
                            + sorted(Mt940.KEYS) + ", _line, _file, _checksum");
                }
            }
            default -> {
                if (!Camt053.KEYS.contains(source)) {
                    issues.add(where + " reads [" + source + "], which a camt053 entry does not carry - use one of " + sorted(Camt053.KEYS)
                            + ", _line, _file, _checksum");
                } else if (Camt053.TRANSACTION_KEYS.contains(source) && !perTransaction(declared)) {
                    issues.add(where + " reads [" + source + "], which only a record per transaction carries - declare camt053: { records:"
                            + " transactions }");
                }
            }
        }
    }

    private static void validateDuplicateKey(ImportIntent declared, String subject, List<String> issues) {
        Set<String> seen = new HashSet<>();
        for (String field : declared.getDuplicateKey()) {
            if (!seen.add(field)) {
                issues.add(subject + " duplicateKey names [" + field + "] twice");
            } else if (!declared.getMap()
                                .containsKey(field)) {
                issues.add(subject + " duplicateKey [" + field + "] is not a mapped field - a key the import does not fill identifies"
                        + " nothing");
            }
        }
    }

    /**
     * A field every inserted row must carry a value for, which nothing but the import could supply: a
     * required, non-key field without a default, a calculation or a document number.
     */
    private static boolean mustBeMapped(FieldIntent field) {
        return field.isRequired() && !field.isPrimaryKey() && field.getDefaultValue() == null && field.getCalculatedOnCreate() == null
                && field.getCalculatedActionOnCreate() == null && field.getNumber() == null;
    }

    /**
     * Plan the generated import classes - one {@code <Name>Import} per declared import, a controller
     * with the upload endpoint and a bean method a job can call.
     *
     * @param model the parsed intent
     * @param byName all local entities by name
     * @param compositionParents composition-parent map
     * @param settings the project settings (the per-import opt-out)
     * @param context the generation context
     * @return the glue descriptors
     */
    static List<Map<String, Object>> buildImports(IntentModel model, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> imports = new ArrayList<>();
        if (model.getImports()
                 .isEmpty()) {
            return imports;
        }
        PermissionSupport.Gates gates = PermissionSupport.gates(model, settings);
        // The same project name the EDM generator prefixes the entity's convention roles with.
        String project = context.getProjectName();
        String projectName = project != null && !project.isBlank() ? project : IntentNaming.baseName(context);
        for (ImportIntent declared : model.getImports()) {
            EntityIntent into = declared.getInto() == null ? null : byName.get(declared.getInto());
            if (declared.getName() == null || into == null || declared.getFormat() == null) {
                continue;
            }
            if (!settings.shouldGenerate("imports", declared.getName())) {
                LOGGER.info("Settings opt-out: keeping existing import class for [{}] (not generated)", LoggedValue.of(declared.getName()));
                continue;
            }
            String entity = into.getName();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", declared.getName());
            entry.put("nameLiteral", NotificationSupport.quote(declared.getName()));
            entry.put("className", IntentNaming.pascalCase(declared.getName()));
            entry.put("entity", entity);
            entry.put("perspective", IntentEntities.resolvePerspective(entity, compositionParents, model));
            entry.put("format", declared.getFormat());
            entry.put("table", IntentNaming.tableName(context, entity));
            entry.put("readerExpression", readerExpression(declared));
            entry.put("valuesExpression", valuesExpression(declared));
            String rolePerspective = IntentEntities.compositionPerspective(entity, compositionParents);
            entry.put("roles", rolesLiteral(gates.writeGate(projectName, rolePerspective, entity)));
            entry.put("mapFields", mapFields(declared, into));
            String whereExpression = whereExpression(declared);
            entry.put("hasWhere", whereExpression != null);
            entry.put("whereExpression", whereExpression == null ? "true" : whereExpression);
            List<Map<String, Object>> keyFields = new ArrayList<>();
            for (String key : declared.getDuplicateKey()) {
                Map<String, Object> keyField = new LinkedHashMap<>();
                keyField.put("property", IntentNaming.pascalCase(key));
                keyField.put("label", key);
                keyFields.add(keyField);
            }
            entry.put("hasDuplicateKey", !keyFields.isEmpty());
            entry.put("duplicateKey", keyFields);
            imports.add(entry);
        }
        return imports;
    }

    private static List<Map<String, Object>> mapFields(ImportIntent declared, EntityIntent into) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Map.Entry<String, Object> entry : declared.getMap()
                                                       .entrySet()) {
            FieldIntent field = fieldOf(into, entry.getKey());
            String source = source(entry.getValue());
            if (field == null || source == null) {
                continue;
            }
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("property", IntentNaming.pascalCase(field.getName()));
            mapped.put("rawExpression", rawExpression(source));
            mapped.put("conversion", conversion(field.getType()));
            fields.add(mapped);
        }
        return fields;
    }

    /** The Java expression reading the source value of one record, as text. */
    private static String rawExpression(String source) {
        if (LINE.equals(source)) {
            return "Integer.toString(index + 1)";
        }
        if (FILE.equals(source)) {
            return "fileName";
        }
        if (CHECKSUM.equals(source)) {
            return "checksum";
        }
        return "record.get(" + NotificationSupport.quote(source) + ")";
    }

    /** The {@code ImportValues} method converting a raw value to the field's type. */
    private static String conversion(String type) {
        return switch (type == null ? "string" : type) {
            case "integer", "int" -> "integer";
            case "long" -> "longInteger";
            // `double` is a DECIMAL column like `decimal`, so both properties are BigDecimal.
            case "decimal", "double" -> "decimal";
            case "boolean" -> "bool";
            case "date" -> "date";
            case "timestamp" -> "timestamp";
            default -> "text"; // string / text / uuid / month / week
        };
    }

    /**
     * The Java expression producing the file's records ({@code List<Map<String, String>>}) from
     * {@code content}.
     */
    private static String readerExpression(ImportIntent declared) {
        String encoding = NotificationSupport.quote(declared.getEncoding() == null ? DEFAULT_ENCODING : declared.getEncoding());
        return switch (declared.getFormat()) {
            case MT940 -> "Mt940.read(ImportText.decode(content, " + encoding + "))";
            case CAMT053 -> perTransaction(declared) ? "Camt053.readTransactions(content)" : "Camt053.read(content)";
            default -> {
                ImportCsvIntent csv = declared.getCsv() == null ? new ImportCsvIntent() : declared.getCsv();
                Set<String> columns = new LinkedHashSet<>();
                for (Object value : declared.getMap()
                                            .values()) {
                    String source = source(value);
                    if (source != null && !LINE.equals(source) && !FILE.equals(source) && !CHECKSUM.equals(source)) {
                        columns.add(source);
                    }
                }
                columns.addAll(declared.getWhere()
                                       .keySet());
                List<String> literals = columns.stream()
                                               .map(NotificationSupport::quote)
                                               .toList();
                yield "DelimitedText.read(ImportText.decode(content, " + encoding + "), "
                        + charLiteral(csv.getSeparator() == null ? DEFAULT_SEPARATOR : csv.getSeparator()) + ", " + header(declared) + ", "
                        + (csv.getSkipLines() == null ? 0 : csv.getSkipLines()) + ", java.util.List.of(" + String.join(", ", literals)
                        + "))";
            }
        };
    }

    /**
     * The {@code where:} filter as one Java boolean expression over {@code record}, or {@code null}
     * when the import declares none. Each key must hold one of its values; a record without the key
     * holds none of them.
     */
    private static String whereExpression(ImportIntent declared) {
        List<String> conditions = new ArrayList<>();
        for (Map.Entry<String, Object> entry : declared.getWhere()
                                                       .entrySet()) {
            List<String> values = whereValues(entry.getValue());
            if (entry.getKey() == null || values.isEmpty()) {
                continue;
            }
            String read = "record.get(" + NotificationSupport.quote(entry.getKey()) + ")";
            List<String> alternatives = new ArrayList<>();
            for (String value : values) {
                alternatives.add(NotificationSupport.quote(value) + ".equals(" + read + ")");
            }
            conditions.add(alternatives.size() == 1 ? alternatives.get(0) : "(" + String.join(" || ", alternatives) + ")");
        }
        return conditions.isEmpty() ? null : String.join(" && ", conditions);
    }

    /** The Java expression building the import's value rules. */
    private static String valuesExpression(ImportIntent declared) {
        if (!CSV.equals(declared.getFormat())) {
            return "ImportValues.canonical()";
        }
        ImportCsvIntent csv = declared.getCsv() == null ? new ImportCsvIntent() : declared.getCsv();
        return "ImportValues.of(" + charLiteral(csv.getDecimal() == null ? DEFAULT_DECIMAL : csv.getDecimal()) + ", "
                + NotificationSupport.quote(csv.getGrouping() == null ? "" : csv.getGrouping()) + ", "
                + nullableLiteral(csv.getDateFormat()) + ", " + nullableLiteral(csv.getTimestampFormat()) + ")";
    }

    private static String rolesLiteral(String roles) {
        List<String> literals = new ArrayList<>();
        for (String role : roles.split(",")) {
            if (!role.isBlank()) {
                literals.add(NotificationSupport.quote(role.trim()));
            }
        }
        return String.join(", ", literals);
    }

    private static String nullableLiteral(String value) {
        return value == null ? "null" : NotificationSupport.quote(value);
    }

    private static String charLiteral(String value) {
        char c = value.charAt(0);
        return switch (c) {
            case '\'' -> "'\\''";
            case '\\' -> "'\\\\'";
            case '\t' -> "'\\t'";
            default -> c < 0x20 || c > 0x7E ? String.format(Locale.ROOT, "'\\u%04x'", (int) c) : "'" + c + "'";
        };
    }

    private static boolean header(ImportIntent declared) {
        return declared.getCsv() == null || declared.getCsv()
                                                    .getHeader() == null
                || declared.getCsv()
                           .getHeader();
    }

    /**
     * An authored source: a non-blank string, or a whole number (a column position without a header).
     */
    private static String source(Object value) {
        if (value instanceof String text) {
            return text.isBlank() ? null : text.trim();
        }
        if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
            return Long.toString(number.longValue());
        }
        return null;
    }

    private static boolean position(String source) {
        try {
            return Integer.parseInt(source) >= 1;
        } catch (NumberFormatException e) {
            LOGGER.trace("Import source [{}] is not a column position", LoggedValue.of(source), e);
            return false;
        }
    }

    private static List<String> sorted(Set<String> keys) {
        return keys.stream()
                   .sorted()
                   .toList();
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name != null && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent relationOf(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            if (name != null && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }
}
