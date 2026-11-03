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
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.report.ReportIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.ExportFieldIntent;
import org.eclipse.dirigible.components.intent.model.ExportIntent;
import org.eclipse.dirigible.components.intent.model.ExportParameterIntent;
import org.eclipse.dirigible.components.intent.model.ExportRecordIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ReportParameterIntent;
import org.eclipse.dirigible.sdk.export.ExportField;

/**
 * The {@code exports:} construct (#7855): a file in an exacting format over a query, recorded on a
 * ledger entity per run.
 *
 * <p>
 * This class owns the construct end to end on the intent side - the rules the parser enforces
 * ({@link #validate}) and the {@code exports} glue collection the events template renders
 * ({@link #build}) - so the two read one resolution of every reference and cannot drift: a
 * reference the parser accepts is exactly one the generator can resolve. The glue carries facts and
 * readings only; the Java, and the formatting itself (the SDK {@code ExportFile}), live in the
 * template layer and the platform.
 */
public final class ExportSupport {

    /** The {@code .settings} category an export is opted out under. */
    public static final String CATEGORY = "exports";

    /** The local each source row is held in by the generated code. */
    static final String ROW = "row";

    private static final String FIXED_WIDTH = "fixedWidth";
    private static final String DELIMITED = "delimited";
    private static final String XML = "xml";
    private static final Set<String> FORMATS = Set.of(FIXED_WIDTH, DELIMITED, XML);
    private static final List<String> RECORD_TYPES = List.of("header", "line", "trailer");
    private static final Set<String> OPS = Set.of("eq", "ne", "gt", "ge", "lt", "le");
    private static final Set<String> DISALLOWED = Set.of("strip", "space", "fail");
    private static final Set<String> NUMERIC_TYPES = Set.of("integer", "int", "long", "decimal", "double");

    /** An identifier the generated code can carry as a Java local and a query parameter. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    /** A property path: a field, or one {@code relation.field} hop. */
    private static final Pattern PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?");
    /** An XML name as the export writes one - no namespace prefix. */
    private static final Pattern XML_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]*");

    private ExportSupport() {}

    /** The rows an export reads: an entity, or a plain report and its column aliases. */
    private record Source(EntityIntent entity, ReportIntent report, Map<String, String> reportColumns) {

        boolean isReport() {
            return report != null;
        }

        String name() {
            return isReport() ? report.getName() : entity.getName();
        }
    }

    /** A total over the rows: {@code sum(...)} or {@code count()}. */
    private record Aggregate(String kind, String of) {
    }

    /**
     * The entities the declared exports record their runs on - the ones the EDM generator gives the
     * injected ledger columns.
     *
     * @param model the intent
     * @return the ledger entity names
     */
    public static Set<String> ledgerEntities(IntentModel model) {
        Set<String> ledgers = new LinkedHashSet<>();
        for (ExportIntent export : model.getExports()) {
            if (!isBlank(export.getLedger())) {
                ledgers.add(export.getLedger()
                                  .trim());
            }
        }
        return ledgers;
    }

    /**
     * The file extension of a format's default file name.
     *
     * @param format the declared format
     * @return {@code txt}, {@code csv} or {@code xml}
     */
    static String extension(String format) {
        return switch (format) {
            case DELIMITED -> "csv";
            case XML -> "xml";
            default -> "txt";
        };
    }

    // ---------------------------------------------------------------------------------------------
    // validation
    // ---------------------------------------------------------------------------------------------

    /**
     * Collects every structural issue of the declared exports.
     *
     * @param model the intent
     * @param issues the collecting issue list
     */
    public static void validate(IntentModel model, List<String> issues) {
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Set<String> taken = otherResourceNames(model);
        Set<String> names = new HashSet<>();
        for (ExportIntent export : model.getExports()) {
            String name = export.getName();
            if (isBlank(name)) {
                issues.add("export has no name");
                continue;
            }
            String subject = "export [" + name + "]";
            if (!IDENTIFIER.matcher(name)
                           .matches()) {
                issues.add(subject + " name must be an identifier (letters, digits, underscore) - it names the generated class");
            }
            if (!names.add(name)) {
                issues.add("duplicate export [" + name + "]");
            }
            if (taken.contains(name)) {
                issues.add(subject + " shares its name with an entity, report, process or form - the name is its permission resource" + " ("
                        + name + ":read), so it must be unique among them");
            }
            if (export.getSubmit() != null) {
                issues.add(subject + " declares submit:, the signed-submission step, which is not implemented yet - remove it for now");
            }
            Source source = validateSource(model, export, subject, issues);
            validateFormat(export, subject, issues);
            validateLedger(export, byName, source, subject, issues);
            validateParameters(export, source, subject, issues);
            validateOrderBy(export, source, subject, issues);
            validateFileName(export, subject, issues);
            validateRecords(model, export, byName, source, subject, issues);
        }
    }

    private static Source validateSource(IntentModel model, ExportIntent export, String subject, List<String> issues) {
        String sourceName = trimmed(export.getSource());
        if (sourceName == null) {
            issues.add(subject + " has no source - name the entity or report whose rows it writes");
            return null;
        }
        EntityIntent entity = IntentEntities.byName(model)
                                            .get(sourceName);
        ReportIntent report = reportNamed(model, sourceName);
        if (entity != null && report != null) {
            issues.add(subject + " source [" + sourceName + "] names both an entity and a report");
            return null;
        }
        if (entity == null && report == null) {
            issues.add(subject + " source [" + sourceName + "] is neither a declared entity nor a declared report");
            return null;
        }
        if (report != null && report.isLedgerKind()) {
            issues.add(subject + " source [" + sourceName + "] is a kind: " + report.getKind()
                    + " report, whose columns are fixed by its ledger shape - export over a plain report or over the entity itself");
            return null;
        }
        return report != null ? new Source(null, report, ReportIntentGenerator.outputAliases(report)) : new Source(entity, null, null);
    }

    private static void validateFormat(ExportIntent export, String subject, List<String> issues) {
        String format = export.getFormat();
        if (format == null || !FORMATS.contains(format)) {
            issues.add(subject + " format [" + format + "] must be one of fixedWidth, delimited, xml");
            return;
        }
        if (export.getEncoding() != null) {
            try {
                if (!Charset.isSupported(export.getEncoding()
                                               .trim())) {
                    issues.add(subject + " encoding [" + export.getEncoding() + "] is not supported by the platform");
                }
            } catch (IllegalCharsetNameException ex) {
                issues.add(subject + " encoding [" + export.getEncoding() + "] is not a character encoding name");
            }
        }
        boolean xml = XML.equals(format);
        if (export.getLineEnding() != null) {
            if (xml) {
                issues.add(subject + " declares lineEnding, which only a fixedWidth or delimited file has");
            } else if (!"crlf".equals(export.getLineEnding()) && !"lf".equals(export.getLineEnding())) {
                issues.add(subject + " lineEnding [" + export.getLineEnding() + "] must be crlf or lf");
            }
        }
        boolean delimited = DELIMITED.equals(format);
        refuseUnless(delimited, export.getDelimiter(), "delimiter", "a delimited", subject, issues);
        refuseUnless(delimited, export.getQuote(), "quote", "a delimited", subject, issues);
        refuseUnless(delimited, export.getQuoting(), "quoting", "a delimited", subject, issues);
        refuseUnless(xml, export.getRoot(), "root", "an xml", subject, issues);
        if (delimited) {
            String delimiter = export.getDelimiter() == null ? "," : export.getDelimiter();
            String quote = export.getQuote() == null ? "\"" : export.getQuote();
            if (delimiter.isEmpty() || delimiter.indexOf('\r') >= 0 || delimiter.indexOf('\n') >= 0) {
                issues.add(subject + " delimiter must be a non-empty text without line breaks");
            }
            if (quote.length() > 1) {
                issues.add(subject + " quote [" + quote + "] must be a single character, or empty for none");
            } else if (!quote.isEmpty() && delimiter.contains(quote)) {
                issues.add(subject + " quote [" + quote + "] must differ from the delimiter");
            }
            if (export.getQuoting() != null && !"minimal".equals(export.getQuoting()) && !"all".equals(export.getQuoting())) {
                issues.add(subject + " quoting [" + export.getQuoting() + "] must be minimal or all");
            }
        }
        if (xml && export.getRoot() != null && !XML_NAME.matcher(export.getRoot())
                                                        .matches()) {
            issues.add(subject + " root [" + export.getRoot() + "] is not an XML element name");
        }
    }

    private static void validateLedger(ExportIntent export, Map<String, EntityIntent> byName, Source source, String subject,
            List<String> issues) {
        String ledger = trimmed(export.getLedger());
        if (ledger == null) {
            issues.add(subject + " has no ledger - name the entity every run is recorded on (declare it with no fields; its columns"
                    + " - Export, Format, Parameters, Outcome, RecordCount, Checksum, ErrorMessage and the stored file - are injected)");
            return;
        }
        EntityIntent entity = byName.get(ledger);
        if (entity == null) {
            issues.add(subject + " ledger [" + ledger + "] is not a declared entity");
            return;
        }
        if (!entity.getFields()
                   .isEmpty()
                || !entity.getRelations()
                          .isEmpty()) {
            issues.add("entity [" + ledger + "] is the ledger of " + subject
                    + " and must declare no fields and no relations - every column of a run record is injected");
        }
        if (source != null && !source.isReport() && ledger.equals(source.name())) {
            issues.add(subject + " records its runs on its own source [" + ledger + "]");
        }
    }

    private static void validateParameters(ExportIntent export, Source source, String subject, List<String> issues) {
        Set<String> names = new HashSet<>();
        for (ExportParameterIntent parameter : export.getParameters()) {
            String name = parameter.getName();
            if (isBlank(name) || !IDENTIFIER.matcher(name)
                                            .matches()) {
                issues.add(subject + " parameter [" + name + "] must be named by an identifier");
                continue;
            }
            String at = subject + " parameter [" + name + "]";
            if (!names.add(name)) {
                issues.add(subject + " declares parameter [" + name + "] twice");
            }
            if (source == null) {
                continue;
            }
            if (source.isReport()) {
                if (parameter.getField() != null || parameter.getOp() != null) {
                    issues.add(at + " declares field/op, but a report source filters by its own parameters - the export only passes"
                            + " the value on");
                }
                if (reportParameter(source.report(), name) == null) {
                    issues.add(at + " is not a parameter of report [" + source.name() + "]" + declaredReportParameters(source.report()));
                }
                continue;
            }
            String field = parameter.getField() == null ? name : parameter.getField();
            if (filterType(source.entity(), field) == null) {
                issues.add(at + " filters [" + field + "], which is neither a field nor a to-one relation of [" + source.name() + "]");
            } else if ("timestamp".equals(filterType(source.entity(), field))) {
                issues.add(at + " filters the timestamp [" + field + "], which a single text value cannot bound exactly"
                        + " - filter on a date field");
            }
            if (parameter.getOp() != null && !OPS.contains(parameter.getOp())) {
                issues.add(at + " op [" + parameter.getOp() + "] must be one of eq, ne, gt, ge, lt, le");
            }
        }
    }

    private static void validateOrderBy(ExportIntent export, Source source, String subject, List<String> issues) {
        if (export.getOrderBy()
                  .isEmpty()
                || source == null) {
            return;
        }
        if (source.isReport()) {
            issues.add(subject + " declares orderBy, but its report source orders its own rows");
            return;
        }
        for (String property : export.getOrderBy()) {
            if (property == null || fieldOf(source.entity(), property) == null) {
                issues.add(subject + " orderBy [" + property + "] is not a field of [" + source.name() + "]");
            }
        }
    }

    private static void validateFileName(ExportIntent export, String subject, List<String> issues) {
        if (export.getFileName() == null) {
            return;
        }
        try {
            fileNameParts(export.getFileName(), declaredParameters(export));
        } catch (IllegalArgumentException ex) {
            issues.add(subject + " " + ex.getMessage());
        }
    }

    private static void validateRecords(IntentModel model, ExportIntent export, Map<String, EntityIntent> byName, Source source,
            String subject, List<String> issues) {
        if (export.getRecords()
                  .stream()
                  .noneMatch(record -> "line".equals(record.getType()))) {
            issues.add(subject + " declares no line record - the layout each row is written in");
        }
        boolean xml = XML.equals(export.getFormat());
        Set<String> parameters = declaredParameters(export);
        int index = 0;
        for (ExportRecordIntent record : export.getRecords()) {
            index++;
            String at = subject + " record " + index;
            if (!RECORD_TYPES.contains(record.getType())) {
                issues.add(at + " type [" + record.getType() + "] must be one of header, line, trailer");
                continue;
            }
            at = subject + " " + record.getType() + " record " + index;
            if (record.getElement() != null) {
                if (!xml) {
                    issues.add(at + " declares element, which only an xml record has");
                } else if (!XML_NAME.matcher(record.getElement())
                                    .matches()) {
                    issues.add(at + " element [" + record.getElement() + "] is not an XML element name");
                }
            }
            if (record.getFields()
                      .isEmpty()) {
                issues.add(at + " declares no fields");
            }
            boolean perRow = "line".equals(record.getType());
            int fieldIndex = 0;
            for (ExportFieldIntent field : record.getFields()) {
                fieldIndex++;
                validateField(model, export, field, at + " field " + fieldIndex, perRow, xml, parameters, byName, source, issues);
            }
        }
    }

    private static void validateField(IntentModel model, ExportIntent export, ExportFieldIntent field, String at, boolean perRow,
            boolean xml, Set<String> parameters, Map<String, EntityIntent> byName, Source source, List<String> issues) {
        int sources = (field.getFrom() == null ? 0 : 1) + (field.getLiteral() == null ? 0 : 1) + (field.getParam() == null ? 0 : 1);
        if (sources != 1) {
            issues.add(at + " must declare exactly one of from, literal, param");
        } else if (field.getParam() != null && !parameters.contains(field.getParam())) {
            issues.add(at + " param [" + field.getParam() + "] is not a declared parameter of the export");
        } else if (field.getFrom() != null && source != null) {
            try {
                Aggregate aggregate = aggregate(field.getFrom(), source);
                if (aggregate != null) {
                    validateAggregate(model, aggregate, source, byName);
                } else if (!perRow) {
                    issues.add(at + " reads [" + field.getFrom() + "] off a row, but a header or trailer is written once - use a literal,"
                            + " a param or a total (sum(...), count())");
                } else {
                    rowReading(model, field.getFrom(), source, byName, new LinkedHashMap<>());
                }
            } catch (IllegalArgumentException ex) {
                issues.add(at + " " + ex.getMessage());
            }
        }
        String name = effectiveName(field);
        if (xml && name == null) {
            issues.add(at + " needs a name - the XML element it is written as");
        } else if (xml && !XML_NAME.matcher(name)
                                   .matches()) {
            issues.add(at + " name [" + name + "] is not an XML name");
        }
        boolean fixedWidth = FIXED_WIDTH.equals(export.getFormat());
        if (fixedWidth && (field.getWidth() == null || field.getWidth() < 1)) {
            issues.add(at + " needs a width of at least 1 - every field of a fixedWidth record occupies a fixed number of characters");
        }
        refuseUnless(fixedWidth, field.getWidth(), "width", "a fixedWidth", at, issues);
        refuseUnless(fixedWidth, field.getAlign(), "align", "a fixedWidth", at, issues);
        refuseUnless(fixedWidth, field.getPad(), "pad", "a fixedWidth", at, issues);
        refuseUnless(fixedWidth, field.getTruncate(), "truncate", "a fixedWidth", at, issues);
        refuseUnless(xml, field.getAttribute(), "attribute", "an xml", at, issues);
        if (field.getAlign() != null && !"left".equals(field.getAlign()) && !"right".equals(field.getAlign())) {
            issues.add(at + " align [" + field.getAlign() + "] must be left or right");
        }
        if (field.getPad() != null && (field.getPad()
                                            .length() != 1
                || Character.isISOControl(field.getPad()
                                               .charAt(0)))) {
            issues.add(at + " pad [" + field.getPad() + "] must be exactly one printable character");
        }
        if (field.getScale() != null && field.getScale() < 0) {
            issues.add(at + " scale must not be negative");
        }
        if (field.getDecimalSeparator() != null && field.getScale() == null) {
            issues.add(at + " declares decimalSeparator without a scale");
        }
        if (field.getPattern() != null) {
            if (field.getScale() != null) {
                issues.add(at + " declares both a date pattern and a decimal scale");
            }
            try {
                DateTimeFormatter.ofPattern(field.getPattern());
            } catch (IllegalArgumentException ex) {
                issues.add(at + " pattern [" + field.getPattern() + "] is not a date/time pattern: " + ex.getMessage());
            }
        }
        validateNormalisation(field, at, issues);
    }

    /**
     * The text normalisation of a field (#7925). Each key is refused where it could not act: on a
     * number or a date (the field is not text), {@code onDisallowed} without {@code allowed}, and
     * {@code onLoss} on a field whose normalisation only cases and spaces - neither changes a letter.
     */
    private static void validateNormalisation(ExportFieldIntent field, String at, List<String> issues) {
        if (!normalises(field)) {
            return;
        }
        if (field.getScale() != null || field.getPattern() != null) {
            issues.add(at + " normalises its text, but it writes a " + (field.getScale() != null ? "number" : "date"));
        }
        if (field.getTextCase() != null && !"upper".equals(field.getTextCase()) && !"lower".equals(field.getTextCase())) {
            issues.add(at + " case [" + field.getTextCase() + "] must be upper or lower");
        }
        if (field.getReplace() != null && field.getReplace()
                                               .containsKey("")) {
            issues.add(at + " replace has an empty key - there is no text to replace");
        }
        if (field.getAllowed() != null) {
            // Read by the SDK field itself, so what parses here is what the run accepts.
            try {
                ExportField.named(effectiveName(field) == null ? "value" : effectiveName(field))
                           .allowed(field.getAllowed());
            } catch (IllegalArgumentException ex) {
                issues.add(at + ": " + ex.getMessage());
            }
        }
        if (field.getOnDisallowed() != null && !DISALLOWED.contains(field.getOnDisallowed())) {
            issues.add(at + " onDisallowed [" + field.getOnDisallowed() + "] must be strip, space or fail");
        } else if (field.getOnDisallowed() != null && field.getAllowed() == null) {
            issues.add(at + " declares onDisallowed without the allowed characters");
        }
        if (field.getOnLoss() != null && !"fail".equals(field.getOnLoss())) {
            issues.add(at + " onLoss [" + field.getOnLoss() + "] must be fail");
        } else if (field.getOnLoss() != null && field.getAllowed() == null && field.getReplace() == null) {
            issues.add(at + " declares onLoss, but only replace or allowed can change a letter or a digit");
        }
    }

    /** Whether a field declares any text normalisation. */
    private static boolean normalises(ExportFieldIntent field) {
        return field.getTextCase() != null || field.getAllowed() != null || field.getReplace() != null || field.getOnDisallowed() != null
                || Boolean.TRUE.equals(field.getCollapseSpaces()) || field.getOnLoss() != null;
    }

    /**
     * Whether an export writes any field through a text normalisation - its runs record what it
     * changed.
     */
    private static boolean normalises(ExportIntent export) {
        return export.getRecords()
                     .stream()
                     .flatMap(record -> record.getFields()
                                              .stream())
                     .anyMatch(ExportSupport::normalises);
    }

    /**
     * The ledgers of the exports that normalise a field's text - the ones the EDM generator gives the
     * column recording what each run changed (#7925).
     *
     * @param model the intent
     * @return the ledger entity names
     */
    public static Set<String> normalisingLedgers(IntentModel model) {
        Set<String> ledgers = new LinkedHashSet<>();
        for (ExportIntent export : model.getExports()) {
            if (!isBlank(export.getLedger()) && normalises(export)) {
                ledgers.add(export.getLedger()
                                  .trim());
            }
        }
        return ledgers;
    }

    private static void validateAggregate(IntentModel model, Aggregate aggregate, Source source, Map<String, EntityIntent> byName) {
        if ("count".equals(aggregate.kind())) {
            return;
        }
        if (source.isReport()) {
            rowReading(model, aggregate.of(), source, byName, new LinkedHashMap<>());
            return;
        }
        String type = pathType(aggregate.of(), source.entity(), byName);
        if (type == null) {
            throw new IllegalArgumentException("sums [" + aggregate.of() + "], which is not a field of [" + source.name() + "]");
        }
        if (!NUMERIC_TYPES.contains(type)) {
            throw new IllegalArgumentException("sums [" + aggregate.of() + "], which is a " + type + " field, not a number");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // generation
    // ---------------------------------------------------------------------------------------------

    /**
     * The {@code exports} glue collection: one descriptor per declared export, each rendered as a
     * controller (the download) that is also the bean any job or custom code calls.
     *
     * @param model the intent
     * @param byName the entities by name
     * @param compositionParents the composition parents
     * @param settings the project's settings
     * @param context the generation context, for dropped-glue issues; may be {@code null} in tests
     * @return the descriptors
     */
    static List<Map<String, Object>> build(IntentModel model, Map<String, EntityIntent> byName, Map<String, String> compositionParents,
            IntentSettings settings, IntentGenerationContext context) {
        List<Map<String, Object>> exports = new ArrayList<>();
        PermissionSupport.Gates gates = PermissionSupport.gates(model, settings);
        for (ExportIntent export : model.getExports()) {
            if (isBlank(export.getName())) {
                continue;
            }
            if (!settings.shouldGenerate(CATEGORY, export.getName())) {
                continue;
            }
            try {
                exports.add(descriptor(model, export, byName, compositionParents, gates));
            } catch (IllegalArgumentException ex) {
                String message = "Export [" + export.getName() + "]: " + ex.getMessage() + " - the export was NOT generated";
                if (context != null) {
                    context.addIssue(message);
                }
            }
        }
        return exports;
    }

    private static Map<String, Object> descriptor(IntentModel model, ExportIntent export, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, PermissionSupport.Gates gates) {
        List<String> issues = new ArrayList<>();
        Source source = validateSource(model, export, "", issues);
        if (source == null) {
            throw new IllegalArgumentException(String.join("; ", issues));
        }
        String format = export.getFormat();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", export.getName());
        entry.put("className", IntentNaming.pascalCase(export.getName()));
        entry.put("format", format);
        entry.put("encoding", export.getEncoding() == null ? "UTF-8"
                : export.getEncoding()
                        .trim());
        entry.put("lineEnding", "lf".equals(export.getLineEnding()) ? "\n" : "\r\n");
        entry.put("delimiter", export.getDelimiter() == null ? "," : export.getDelimiter());
        entry.put("quote", export.getQuote() == null ? "\"" : export.getQuote());
        entry.put("quoteAll", "all".equals(export.getQuoting()));
        entry.put("root", export.getRoot() == null ? export.getName() : export.getRoot());
        String ledger = export.getLedger()
                              .trim();
        entry.put("ledger", ledger);
        entry.put("ledgerPerspective", IntentEntities.resolvePerspective(ledger, compositionParents, model));
        if (source.isReport()) {
            entry.put("sourceKind", "report");
            entry.put("report", source.name());
        } else {
            entry.put("sourceKind", "entity");
            entry.put("entity", source.name());
            entry.put("perspective", IntentEntities.resolvePerspective(source.name(), compositionParents, model));
            List<String> order = new ArrayList<>();
            for (String property : export.getOrderBy()) {
                order.add(IntentNaming.pascalCase(property));
            }
            if (order.isEmpty()) {
                order.add(IntentNaming.pascalCase(IntentEntities.keyFieldName(source.entity())));
            }
            entry.put("orderBy", order);
        }
        entry.put("parameters", parameters(export, source));
        Map<String, NotificationSupport.RelationLoad> loads = new LinkedHashMap<>();
        List<Map<String, Object>> aggregates = new ArrayList<>();
        Map<String, List<Map<String, Object>>> records = new LinkedHashMap<>();
        for (String type : RECORD_TYPES) {
            records.put(type, new ArrayList<>());
        }
        for (ExportRecordIntent record : export.getRecords()) {
            List<Map<String, Object>> ofType = records.get(record.getType());
            Map<String, Object> layout = new LinkedHashMap<>();
            layout.put("constant", record.getType()
                                         .toUpperCase(Locale.ROOT)
                    + "_" + ofType.size());
            layout.put("element", record.getElement() == null ? record.getType() : record.getElement());
            List<Map<String, Object>> fields = new ArrayList<>();
            int index = 0;
            for (ExportFieldIntent field : record.getFields()) {
                index++;
                fields.add(field(model, field, index, source, byName, loads, aggregates));
            }
            layout.put("fields", fields);
            ofType.add(layout);
        }
        if (normalises(export)) {
            // The runs record what the normalisation changed; without it the glue is as it was.
            entry.put("normalises", true);
        }
        entry.put("headerRecords", records.get("header"));
        entry.put("lineRecords", records.get("line"));
        entry.put("trailerRecords", records.get("trailer"));
        entry.put("aggregates", aggregates);
        List<Map<String, Object>> loadList = new ArrayList<>();
        for (NotificationSupport.RelationLoad load : loads.values()) {
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("local", load.local());
            descriptor.put("fkProperty", load.fkProperty());
            descriptor.put("targetEntity", load.targetEntity());
            descriptor.put("targetPerspective", load.targetPerspective());
            loadList.add(descriptor);
        }
        entry.put("loads", loadList);
        entry.put("fileNameReading", fileNameReading(export));
        if (gates.covers(export.getName())) {
            entry.put("roles", gates.appendRead(export.getName(), gates.readRoles(export.getName())));
        }
        return entry;
    }

    private static List<Map<String, Object>> parameters(ExportIntent export, Source source) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        for (ExportParameterIntent declared : export.getParameters()) {
            Map<String, Object> parameter = new LinkedHashMap<>();
            parameter.put("name", declared.getName());
            parameter.put("local", parameterLocal(declared.getName()));
            parameter.put("required", Boolean.TRUE.equals(declared.getRequired()));
            if (source.isReport()) {
                parameter.put("kind", "report");
            } else {
                String field = declared.getField() == null ? declared.getName() : declared.getField();
                String type = filterType(source.entity(), field);
                parameter.put("kind", "filter");
                parameter.put("property", IntentNaming.pascalCase(field));
                parameter.put("op", declared.getOp() == null ? "eq" : declared.getOp());
                parameter.put("convert", Readings.convert(type));
            }
            parameters.add(parameter);
        }
        return parameters;
    }

    private static Map<String, Object> field(IntentModel model, ExportFieldIntent field, int index, Source source,
            Map<String, EntityIntent> byName, Map<String, NotificationSupport.RelationLoad> loads, List<Map<String, Object>> aggregates) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        String name = effectiveName(field);
        descriptor.put("name", name == null ? "Field" + index : name);
        if (field.getWidth() != null) {
            descriptor.put("width", String.valueOf(field.getWidth()));
        }
        descriptor.put("alignRight", "right".equals(field.getAlign()));
        if (field.getPad() != null) {
            descriptor.put("pad", field.getPad());
        }
        descriptor.put("truncate", Boolean.TRUE.equals(field.getTruncate()));
        if (field.getScale() != null) {
            descriptor.put("scale", String.valueOf(field.getScale()));
        }
        if (field.getDecimalSeparator() != null) {
            descriptor.put("decimalSeparator", field.getDecimalSeparator());
        }
        if (field.getPattern() != null) {
            descriptor.put("pattern", field.getPattern());
        }
        descriptor.put("attribute", Boolean.TRUE.equals(field.getAttribute()));
        if (field.getReplace() != null) {
            List<Map<String, String>> replacements = new ArrayList<>();
            field.getReplace()
                 .forEach((target,
                         replacement) -> replacements.add(Map.of("target", target, "replacement", replacement == null ? "" : replacement)));
            descriptor.put("replace", replacements);
        }
        if (field.getTextCase() != null) {
            descriptor.put("case", field.getTextCase());
        }
        if (field.getAllowed() != null) {
            descriptor.put("allowed", field.getAllowed());
            descriptor.put("onDisallowed", field.getOnDisallowed() == null ? "fail" : field.getOnDisallowed());
        }
        if (Boolean.TRUE.equals(field.getCollapseSpaces())) {
            descriptor.put("collapseSpaces", true);
        }
        if ("fail".equals(field.getOnLoss())) {
            descriptor.put("failOnLoss", true);
        }
        descriptor.put("reading", valueReading(model, field, source, byName, loads, aggregates));
        return descriptor;
    }

    private static Map<String, Object> valueReading(IntentModel model, ExportFieldIntent field, Source source,
            Map<String, EntityIntent> byName, Map<String, NotificationSupport.RelationLoad> loads, List<Map<String, Object>> aggregates) {
        if (field.getLiteral() != null) {
            return Readings.string(field.getLiteral());
        }
        if (field.getParam() != null) {
            return Readings.local(parameterLocal(field.getParam()));
        }
        Aggregate aggregate = aggregate(field.getFrom(), source);
        if (aggregate == null) {
            return rowReading(model, field.getFrom(), source, byName, loads);
        }
        String local = "total" + aggregates.size();
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("local", local);
        total.put("kind", aggregate.kind());
        if ("sum".equals(aggregate.kind())) {
            total.put("reading", rowReading(model, aggregate.of(), source, byName, loads));
        }
        aggregates.add(total);
        return Readings.local(local);
    }

    /**
     * The reading of a value off one source row: an entity field ({@code row.Property}), one
     * {@code relation.field} hop off it (the related record loaded into a local named after the
     * relation), or a report column by its alias.
     */
    private static Map<String, Object> rowReading(IntentModel model, String from, Source source, Map<String, EntityIntent> byName,
            Map<String, NotificationSupport.RelationLoad> loads) {
        String path = from.trim();
        if (source.isReport()) {
            String alias = source.reportColumns()
                                 .get(ReportIntentGenerator.columnKey(path));
            if (alias == null) {
                throw new IllegalArgumentException("[" + path + "] is not a dimension or measure of report [" + source.name() + "]"
                        + (source.reportColumns()
                                 .isEmpty() ? "" : " (it declares: " + declaredReportColumns(source.report()) + ")"));
            }
            return Readings.column(ROW, alias);
        }
        if (!PATH.matcher(path)
                 .matches()) {
            throw new IllegalArgumentException("[" + path + "] is neither a field nor a relation.field path");
        }
        EntityIntent entity = source.entity();
        int dot = path.indexOf('.');
        if (dot < 0) {
            if (fieldOf(entity, path) == null) {
                throw new IllegalArgumentException("[" + path + "] is not a field of [" + entity.getName() + "]");
            }
            return Readings.read(ROW, IntentNaming.pascalCase(path));
        }
        String relationName = path.substring(0, dot);
        String fieldName = path.substring(dot + 1);
        RelationIntent relation = toOneRelation(entity, relationName);
        if (relation == null || relation.getTo() == null) {
            throw new IllegalArgumentException("[" + relationName + "] is not a to-one relation of [" + entity.getName() + "]");
        }
        if (relation.isCrossModel()) {
            throw new IllegalArgumentException("[" + path + "] crosses into model [" + relation.getModel()
                    + "], which an export does not read - export over a report that joins it");
        }
        EntityIntent target = byName.get(relation.getTo());
        if (target == null || fieldOf(target, fieldName) == null) {
            throw new IllegalArgumentException("[" + fieldName + "] is not a field of [" + relation.getTo() + "]");
        }
        String local = "hop" + IntentNaming.pascalCase(relationName);
        loads.computeIfAbsent(relationName,
                name -> new NotificationSupport.RelationLoad(local, relation.getTo(),
                        IntentEntities.resolvePerspective(relation.getTo(), IntentEntities.compositionParents(model), model),
                        IntentNaming.pascalCase(name), false, "", ""));
        return Readings.hop(local, IntentNaming.pascalCase(fieldName));
    }

    private static Map<String, Object> fileNameReading(ExportIntent export) {
        String pattern = export.getFileName() == null ? export.getName() + "." + extension(export.getFormat()) : export.getFileName();
        List<Map<String, Object>> parts = new ArrayList<>();
        for (String[] part : fileNameParts(pattern, declaredParameters(export))) {
            parts.add("param".equals(part[0]) ? Readings.part(Readings.local(parameterLocal(part[1])), null) : Readings.string(part[1]));
        }
        return Readings.concat(parts, true);
    }

    /**
     * Splits a {@code fileName:} pattern into its literal pieces and its {@code {parameter}} tokens,
     * each as {@code [kind, text]}.
     */
    private static List<String[]> fileNameParts(String pattern, Set<String> parameters) {
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        List<String[]> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int index = 0;
        while (index < pattern.length()) {
            char character = pattern.charAt(index);
            if (character == '}') {
                throw new IllegalArgumentException("fileName [" + pattern + "] closes a token it never opened");
            }
            if (character == '/' || character == '\\') {
                throw new IllegalArgumentException("fileName [" + pattern + "] carries a path separator - it names a file, not a folder");
            }
            if (character != '{') {
                literal.append(character);
                index++;
                continue;
            }
            int close = pattern.indexOf('}', index);
            if (close < 0) {
                throw new IllegalArgumentException("fileName [" + pattern + "] opens a token it never closes");
            }
            String token = pattern.substring(index + 1, close)
                                  .trim();
            if (!parameters.contains(token)) {
                throw new IllegalArgumentException(
                        "fileName [" + pattern + "] token {" + token + "} is not a declared parameter of the export");
            }
            if (!literal.isEmpty()) {
                parts.add(new String[] {"literal", literal.toString()});
                literal.setLength(0);
            }
            parts.add(new String[] {"param", token});
            index = close + 1;
        }
        if (!literal.isEmpty()) {
            parts.add(new String[] {"literal", literal.toString()});
        }
        return parts;
    }

    // ---------------------------------------------------------------------------------------------
    // shared helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code sum(x)} / {@code count()}, or {@code null} for a plain reference. Over a report a column
     * is matched FIRST: the report's own {@code count(*)} or {@code sum(taxBase)} measure is a value of
     * each row, not a total of the file - which is why the inner expression of a sum may itself carry
     * parentheses there, {@code sum(sum(taxBase))} totalling that measure over the rows.
     */
    private static Aggregate aggregate(String from, Source source) {
        String text = from.trim();
        if (source.isReport() && source.reportColumns()
                                       .containsKey(ReportIntentGenerator.columnKey(text))) {
            return null;
        }
        int open = text.indexOf('(');
        if (open < 0 || !text.endsWith(")")) {
            return null;
        }
        String kind = text.substring(0, open)
                          .trim()
                          .toLowerCase(Locale.ROOT);
        String of = text.substring(open + 1, text.length() - 1)
                        .trim();
        if ("count".equals(kind)) {
            if (!of.isEmpty() && !"*".equals(of)) {
                throw new IllegalArgumentException("count() counts the rows and takes no argument - got [" + from + "]");
            }
            return new Aggregate(kind, null);
        }
        if ("sum".equals(kind)) {
            if (of.isEmpty()) {
                throw new IllegalArgumentException("sum() needs the value it adds up");
            }
            return new Aggregate(kind, of);
        }
        throw new IllegalArgumentException("[" + from + "] is not a total - an export field totals with sum(...) or count()");
    }

    /**
     * The name a field is written under: the authored name, else the parameter it writes, else the leaf
     * of the property it reads; {@code null} for a literal or a total without a name.
     */
    private static String effectiveName(ExportFieldIntent field) {
        if (!isBlank(field.getName())) {
            return field.getName()
                        .trim();
        }
        if (!isBlank(field.getParam())) {
            return field.getParam()
                        .trim();
        }
        if (!isBlank(field.getFrom()) && PATH.matcher(field.getFrom()
                                                           .trim())
                                             .matches()) {
            String from = field.getFrom()
                               .trim();
            return from.substring(from.lastIndexOf('.') + 1);
        }
        return null;
    }

    private static String parameterLocal(String name) {
        return "param" + IntentNaming.pascalCase(name);
    }

    /** The intent type a parameter filtering this field or to-one relation compares against. */
    private static String filterType(EntityIntent entity, String name) {
        FieldIntent field = fieldOf(entity, name);
        if (field != null) {
            return field.getType() == null ? "string"
                    : field.getType()
                           .toLowerCase(Locale.ROOT);
        }
        RelationIntent relation = toOneRelation(entity, name);
        return relation == null || relation.isCrossModel() ? null : "relation";
    }

    /** The type of a field or one-hop path, or {@code null} when it does not resolve. */
    private static String pathType(String path, EntityIntent entity, Map<String, EntityIntent> byName) {
        int dot = path.indexOf('.');
        FieldIntent field;
        if (dot < 0) {
            field = fieldOf(entity, path);
        } else {
            RelationIntent relation = toOneRelation(entity, path.substring(0, dot));
            EntityIntent target = relation == null || relation.isCrossModel() ? null : byName.get(relation.getTo());
            field = target == null ? null : fieldOf(target, path.substring(dot + 1));
        }
        return field == null ? null
                : field.getType() == null ? "string"
                        : field.getType()
                               .toLowerCase(Locale.ROOT);
    }

    private static Set<String> declaredParameters(ExportIntent export) {
        Set<String> names = new LinkedHashSet<>();
        for (ExportParameterIntent parameter : export.getParameters()) {
            if (parameter.getName() != null) {
                names.add(parameter.getName());
            }
        }
        return names;
    }

    private static Set<String> otherResourceNames(IntentModel model) {
        Set<String> names = new HashSet<>();
        for (EntityIntent entity : model.getEntities()) {
            names.add(entity.getName());
        }
        for (ReportIntent report : model.getReports()) {
            names.add(report.getName());
        }
        for (ProcessIntent process : model.getProcesses()) {
            names.add(process.getName());
        }
        for (FormIntent form : model.getForms()) {
            names.add(form.getName());
        }
        return names;
    }

    private static ReportIntent reportNamed(IntentModel model, String name) {
        for (ReportIntent report : model.getReports()) {
            if (name.equals(report.getName())) {
                return report;
            }
        }
        return null;
    }

    private static ReportParameterIntent reportParameter(ReportIntent report, String name) {
        for (ReportParameterIntent parameter : report.getParameters()) {
            if (name.equals(parameter.getName())) {
                return parameter;
            }
        }
        return null;
    }

    private static String declaredReportParameters(ReportIntent report) {
        List<String> names = new ArrayList<>();
        for (ReportParameterIntent parameter : report.getParameters()) {
            names.add(parameter.getName());
        }
        return names.isEmpty() ? " (it declares none)" : " (declared: " + String.join(", ", names) + ")";
    }

    private static String declaredReportColumns(ReportIntent report) {
        List<String> columns = new ArrayList<>();
        columns.addAll(report.getDimensions());
        columns.addAll(report.getMeasures());
        return String.join(", ", columns);
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private static RelationIntent toOneRelation(EntityIntent entity, String name) {
        for (RelationIntent relation : entity.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static void refuseUnless(boolean allowed, Object value, String key, String where, String subject, List<String> issues) {
        if (!allowed && value != null) {
            issues.add(subject + " declares " + key + ", which only " + where + " export has");
        }
    }

    private static String trimmed(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
