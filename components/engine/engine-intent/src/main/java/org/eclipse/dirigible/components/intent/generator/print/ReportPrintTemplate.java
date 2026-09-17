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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.model.ReportParameterIntent;

/**
 * The standard {@code .print} template of a REPORT - the layout a notify block's {@code attach: {
 * report, bind }} renders through. The document-master counterpart is {@link PrintIntentGenerator};
 * this is the same idea one shape over: where a document template binds one record's header plus
 * its line items, a report template binds the bound PARAMETERS as its header and the report's rows
 * as its table.
 *
 * <p>
 * It is written from the report's own resolved columns, at the point the {@code .report} is
 * assembled, rather than re-derived here: a column alias is what the generated query actually
 * SELECTs (a humanized measure, an {@code ageing(...)} bucket, a balance window column), and a
 * second derivation of that naming would drift into placeholders that silently render empty. That
 * is also why this class takes the emitted column maps instead of the {@link ReportIntent}.
 *
 * <p>
 * Like the document template it is a <b>scaffold written once</b> and developer-owned afterwards -
 * a statement mailed to a customer is a formatted business artifact, and a later Generate must not
 * overwrite a designed one.
 */
public final class ReportPrintTemplate {

    private ReportPrintTemplate() {}

    /**
     * The CMS-mirroring path of a report's print template under the project's {@code doc/} folder - the
     * same {@code Templates/<name>/Print/<lang>/standard.print} layout a document uses, which is what
     * lets {@code sdk.print.Print.render(<report>, ...)} resolve it by name.
     *
     * @param report the report's name
     * @return the project-relative file name
     */
    public static String fileName(String report) {
        return "doc/Templates/" + report + "/Print/en/standard.print";
    }

    /**
     * Builds the standard template for one report.
     *
     * @param report the report
     * @param columns the report's emitted columns ({@code alias} / {@code type} / {@code aggregate}
     *        entries, as written into the {@code .report})
     * @return the {@code .print} template source
     */
    public static String build(ReportIntent report, List<Map<String, Object>> columns) {
        String label = IntentNaming.humanize(report.getName());
        StringBuilder template = new StringBuilder(4096);
        template.append("<!-- Standard print template for the ")
                .append(escape(label))
                .append(" report, generated from the intent model.\n")
                .append("     It is what a notify block's `attach: { report, bind }` renders: the bound parameters are the\n")
                .append("     header, the report's rows are the table. The published copy is seeded into the CMS under\n")
                .append("     Templates/")
                .append(report.getName())
                .append("/Print/<lang>/ where it can be customized (download/upload) through the\n")
                .append("     Documents perspective. Written once - a later Generate will not overwrite it.\n")
                .append(PrintLogo.comment())
                .append("     -->\n");
        template.append("<document id=\"")
                .append(IntentNaming.upperSnake(report.getName())
                                    .toLowerCase(Locale.ROOT)
                                    .replace('_', '-'))
                .append("-report\">\n");
        // Landscape: a report is wider than a document - as many columns as the query SELECTs, where an
        // invoice has the four of a line item. A portrait page would crush them.
        template.append("    <page width=\"842\" height=\"595\" padding=\"40\">\n\n");

        template.append("        <header>\n");
        PrintLogo.append(template, "            ");
        template.append("            <text style=\"title\">")
                .append(escape(label))
                .append("</text>\n");
        template.append("            <line/>\n");
        template.append("        </header>\n\n");

        // The bound parameters as the header: which slice this PDF is. A report's rows carry no such
        // statement of their own, so without this the recipient cannot tell one statement from the next.
        List<ReportParameterIntent> parameters = report.getParameters();
        boolean ledger = report.isLedgerKind();
        if (!parameters.isEmpty() || ledger) {
            template.append("        <section>\n");
            if (ledger) {
                appendParameter(template, "fromDate");
                appendParameter(template, "toDate");
            }
            for (ReportParameterIntent parameter : parameters) {
                if (parameter.getName() != null && !parameter.getName()
                                                             .isBlank()) {
                    appendParameter(template, parameter.getName()
                                                       .trim());
                }
            }
            template.append("        </section>\n\n");
        }

        // The rows. Every alias is a key the report's own result set carries, so no placeholder here can
        // be dead - the contract the document scaffold keeps with its feeder, kept with the query.
        template.append("        <table source=\"items\">\n");
        for (int i = 0; i < columns.size(); i++) {
            String alias = String.valueOf(columns.get(i)
                                                 .get("alias"));
            String width = i == 0 ? "2*" : "*";
            String align = isNumeric(columns.get(i)) ? " align=\"right\"" : "";
            template.append("            <column width=\"")
                    .append(width)
                    .append("\"")
                    .append(align)
                    .append(" label=\"")
                    .append(escape(alias))
                    .append("\">{{")
                    .append(alias)
                    .append("}}</column>\n");
        }
        template.append("        </table>\n\n");

        template.append("        <footer>\n");
        template.append("            <text align=\"right\">")
                .append(escape(label))
                .append("</text>\n");
        template.append("        </footer>\n\n");
        template.append("    </page>\n");
        template.append("</document>\n");
        return template.toString();
    }

    /** One bound parameter as a label/value line of the header. */
    private static void appendParameter(StringBuilder template, String name) {
        template.append("            <text>")
                .append(escape(IntentNaming.humanize(name)))
                .append(": {{document.")
                .append(name)
                .append("}}</text>\n");
    }

    /**
     * Whether a column holds a figure, and so is right-aligned. Read off the emitted column rather than
     * guessed: an aggregate is always numeric, and so is a numeric SQL type.
     */
    private static boolean isNumeric(Map<String, Object> column) {
        Object aggregate = column.get("aggregate");
        if (aggregate != null && !"NONE".equals(aggregate)) {
            return true; // SUM / AVG / MIN / MAX / COUNT - every one of them a figure
        }
        String type = column.get("type") == null ? ""
                : String.valueOf(column.get("type"))
                        .toUpperCase(Locale.ROOT);
        return "DECIMAL".equals(type) || "DOUBLE".equals(type) || "INTEGER".equals(type) || "BIGINT".equals(type) || "SMALLINT".equals(type)
                || "TINYINT".equals(type) || "REAL".equals(type);
    }

    private static String escape(String value) {
        return value == null ? ""
                : value.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;");
    }
}
