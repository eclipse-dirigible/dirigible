/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the consumed-attributes manifest: which attributes a model sets that no consumer reads
 * (dirigible #6543).
 */
class ConsumedAttributesAuditTest {

    /** The tokens a template that reads the usual suspects would contribute. */
    private static final Set<String> READS_THE_USUAL =
            Set.of("name", "dataName", "dataType", "dataPrimaryKey", "widgetType", "perspectiveName", "entities", "properties");

    @Test
    void reportsAnAttributeNoTemplateSourceReads() {
        String model = """
                {"model":{"entities":[{"name":"Book","perspectiveName":"books","properties":[
                  {"name":"Title","dataName":"TITLE","dataType":"VARCHAR","widgetType":"TEXTBOX","widgetGlitter":"sparkly"}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js", READS_THE_USUAL);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0)
                           .contains("[widgetGlitter]"),
                warnings.get(0));
        assertTrue(warnings.get(0)
                           .contains("property [Book.Title]"),
                warnings.get(0));
        assertTrue(warnings.get(0)
                           .contains("[library.model]"),
                warnings.get(0));
        assertTrue(warnings.get(0)
                           .contains("template-x/template.js"),
                warnings.get(0));
    }

    @Test
    void staysSilentWhenTheTemplateReadsIt() {
        String model = """
                {"model":{"entities":[{"name":"Book","properties":[
                  {"name":"Title","dataType":"VARCHAR","widgetGlitter":"sparkly"}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js",
                Set.of("name", "dataType", "widgetGlitter"));

        assertEquals(List.of(), warnings);
    }

    @Test
    void catchesProducerConsumerDriftInTheAttributeName() {
        // The generator emits `numberStampOn`, the template reads `numberStampOnCreate`: a prefix
        // match is not a read, and the drift must not pass as one.
        String model = """
                {"model":{"entities":[{"name":"Invoice","properties":[
                  {"name":"Number","dataType":"VARCHAR","numberStampOn":"create"}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("billing.model", model, "template-x/template.js",
                Set.of("name", "dataType", "numberStampOnCreate"));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0)
                           .contains("[numberStampOn]"),
                warnings.get(0));
    }

    @Test
    void ignoresAttributesThatAskForNothing() {
        // Null, blank, false and zero ask for no behaviour, so no behaviour can be missing because
        // nobody read them - and every model file is full of them.
        String model = """
                {"model":{"entities":[{"name":"Book","properties":[{"name":"Title",
                  "widgetGlitterA":null,"widgetGlitterB":"","widgetGlitterC":false,"widgetGlitterD":"false","widgetGlitterE":0}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js", Set.of("name"));

        assertEquals(List.of(), warnings);
    }

    @Test
    void claimsWhatTheGenerationStagesAndTheEditorOwn() {
        // widgetLength is derived by the Java pipeline and menuIndex belongs to the entity editor:
        // neither is ever named in a template source, and neither is a defect.
        String model = """
                {"model":{"entities":[{"name":"Book","menuIndex":"100","tooltip":"A book","properties":[
                  {"name":"Title","widgetLength":"120","relationshipEntityPerspectiveLabel":"Books"}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js", Set.of("name"));

        assertEquals(List.of(), warnings);
    }

    @Test
    void reportsAnAttributeOncePerName() {
        // An unread attribute is one defect however many properties carry it - the report names the
        // first place and counts the rest, so it stays readable on a thirty-entity model.
        String model = """
                {"model":{"entities":[{"name":"Book","properties":[
                  {"name":"Title","widgetGlitter":"a"},{"name":"Author","widgetGlitter":"b"},{"name":"Isbn","widgetGlitter":"c"}]}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js", Set.of("name"));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0)
                           .contains("property [Book.Title] and 2 more"),
                warnings.get(0));
    }

    @Test
    void auditsEntityPerspectiveAndNavigationAttributesToo() {
        String model = """
                {"model":{"entities":[{"name":"Book","entityGlitter":"e"}],
                  "perspectives":[{"name":"books","perspectiveGlitter":"p"}],
                  "navigations":[{"name":"main","navigationGlitter":"n"}]}}
                """;

        List<String> warnings = ConsumedAttributesAudit.unconsumed("library.model", model, "template-x/template.js", Set.of("name"));

        assertEquals(3, warnings.size());
        assertTrue(warnings.get(0)
                           .contains("entity [Book]"),
                warnings.get(0));
        assertTrue(warnings.get(1)
                           .contains("perspective [books]"),
                warnings.get(1));
        assertTrue(warnings.get(2)
                           .contains("navigation [main]"),
                warnings.get(2));
    }

    @Test
    void staysSilentOnAModelItCannotParse() {
        assertEquals(List.of(),
                ConsumedAttributesAudit.unconsumed("library.model", "not json at all", "template-x/template.js", READS_THE_USUAL));
    }

}
