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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.ide.template.domain.GenerationTemplateMetadataSource;
import org.eclipse.dirigible.components.ide.template.service.GenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The per-generation consumed-attributes manifest: which of the attributes a model file sets are
 * actually read by the template that generates code from it, and which are read by nobody
 * (dirigible #6543).
 *
 * <p>
 * The failure class this closes is silent degradation. An attribute that no consumer reads ships
 * with every step green - the model carries it, generation succeeds, and the behaviour it asks for
 * is simply absent from the generated code. Two real shapes of it: a <b>stale registry template</b>
 * that predates the attribute (the platform emits it, the published template never mentions it),
 * and <b>producer/consumer drift</b> (the generator emits {@code fooBar}, the template reads
 * {@code fooBarBaz}). Output oracles only cover the tokens somebody thought to assert; this covers
 * every attribute the model sets, by construction.
 *
 * <p>
 * An attribute counts as <em>claimed</em> when any of three consumers names it:
 * <ol>
 * <li>the <b>template</b> - its name appears as a token in a source the template descriptor lists,
 * or in one of their {@code rename} expressions. This is the half that is observed per generation,
 * against the template actually published in this registry, which is what makes a stale template
 * visible.</li>
 * <li>the <b>pipeline</b> - {@link #PIPELINE_CLAIMED}, the attributes the Java generation stages
 * read and turn into something else, so the template never names them itself.</li>
 * <li>the <b>editor</b> - {@link #EDITOR_OWNED}, attributes the entity editor keeps in the model
 * for its own authoring surface and generation is not expected to read at all.</li>
 * </ol>
 * Anything else the model sets is reported.
 *
 * <p>
 * The audit is against the template <b>this project actually generates with</b>, so it is only as
 * broad as that template: a project whose recipe is a narrow template (the schema alone, say)
 * legitimately reads none of the UI attributes, and the report says so. The recipe an intent
 * project scaffolds - and the one this is calibrated against - is the full-stack template.
 *
 * <p>
 * Only attributes with a <em>meaningful</em> value are considered: an attribute left null, blank,
 * {@code false} or zero asks for nothing, so nothing can be missing from the generated code.
 * Reporting is one line per distinct attribute (naming the first occurrence and the total), because
 * an unread attribute is one defect however many properties carry it.
 */
@Component
public class ConsumedAttributesAudit {

    private static final Logger logger = LoggerFactory.getLogger(ConsumedAttributesAudit.class);

    /** Identifier tokens in a template source - how an attribute name is spotted in it. */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** The action of a source that is copied verbatim: it renders nothing, so it reads nothing. */
    private static final String ACTION_COPY = "copy";

    /**
     * Attributes read by the Java generation stages rather than by a template. The stages derive
     * something else from them - a Java type, a column, a filter expression, a perspective descriptor -
     * so the attribute's own name never appears in a template source and the template scan cannot see
     * the read.
     *
     * <p>
     * This is the hand-declared half of the manifest, and it is declared rather than observed because
     * the reads happen in compiled code. An attribute wrongly listed here costs a warning that would
     * have been true; an attribute missing from here costs a warning that a human triages once. Keep it
     * in step with what the stages in this package actually read.
     */
    private static final Set<String> PIPELINE_CLAIMED = Set.of("dataCount", "dataNullable", "dataNotNull", "dataScale", "dataPrecision",
            "dataOrderBy", "dataOrderBySort", "dataUnique", "generateBusinessKey", "generateDefaultRoles", "generateEvents",
            "generateReopens", "generateReport", "identityProperty", "immutableStatusProperty", "immutableStatusValues", "locksWithMaster",
            "periodClosedValues", "periodEndProperty", "periodLockDateProperty", "periodLockEntity", "periodStartProperty",
            "periodStatusProperty", "perspectiveIcon", "perspectiveLabel", "projectionReferencedModel", "extensionReferencedEntity",
            "extensionReferencedModel", "relationshipCardinality", "relationshipIdentityLabel", "relationshipIdentityProperty",
            "relationshipPartnerIdentityLabel", "relationshipPartnerIdentityProperty", "relationshipPartner", "relationshipPersonal",
            "relationshipPersonalReadOnly", "widgetDependsOnHeaderEntity", "widgetDependsOnValueBy", "widgetDependsOnValueByHeaderEntity",
            "widgetLength", "widgetOptionsEntityPerspectiveName", "widgetOptionsFilterBy", "widgetOptionsFilterValue",
            "widgetOptionsFilterValueJs");

    /**
     * Attributes the entity editor owns: it keeps them in the model for its own authoring surface
     * (labels it shows, the reference it navigates by, ordering in its trees) and no generator is
     * expected to read them. They are set on every model the editor or the intent's {@code .edm}
     * derivation writes, so without this set the audit would report the same handful on every
     * generation and be ignored - which is the one way a warning fails.
     */
    private static final Set<String> EDITOR_OWNED = Set.of("menuIndex", "menuKey", "tooltip", "navigationPath",
            "projectionReferencedEntity", "relationshipEntityPerspectiveLabel", "widgetDependsOnEntity", "widgetDependsOnValueByEntity");

    /** Renders/reads the template sources being audited. */
    private final ModelTemplateRenderer renderer;

    ConsumedAttributesAudit(ModelTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Audits one model file against the template that generates code from it.
     *
     * @param modelFileName the project-relative path of the model file, for the message
     * @param modelText the model file's content (extensions already folded in)
     * @param templateId the template's module path
     * @param parameters the generation parameters the descriptor may branch on
     * @return one warning per attribute the model sets that no consumer claims, empty when everything
     *         is claimed - and empty when the template's sources cannot be read at all, since a
     *         template nobody can load is not evidence that anything is unconsumed
     */
    public List<String> audit(String modelFileName, String modelText, String templateId, Map<String, Object> parameters) {
        Set<String> claimedByTemplate = readTemplateTokens(templateId, parameters);
        if (claimedByTemplate.isEmpty()) {
            return List.of();
        }
        return unconsumed(modelFileName, modelText, templateId, claimedByTemplate);
    }

    /**
     * The tokens appearing anywhere in the sources the template descriptor lists - the observed half of
     * the manifest.
     *
     * @param templateId the template's module path
     * @param parameters the generation parameters the descriptor may branch on
     * @return the tokens, or empty when the descriptor or every one of its sources is unreadable
     */
    private Set<String> readTemplateTokens(String templateId, Map<String, Object> parameters) {
        List<GenerationTemplateMetadataSource> sources;
        try {
            sources = GenerationService.getTemplateMetadata(templateId, parameters)
                                       .getSources();
        } catch (RuntimeException e) {
            logger.debug("Consumed-attributes audit skipped: template [{}] could not be read", templateId, e);
            return Set.of();
        }
        if (sources == null) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (GenerationTemplateMetadataSource source : sources) {
            collect(tokens, source.getRename());
            if (ACTION_COPY.equals(source.getAction())) {
                // Copied verbatim: it is not rendered against the model, so it reads nothing.
                continue;
            }
            try {
                collect(tokens, renderer.readTemplate(source.getLocation()));
            } catch (IOException | RuntimeException e) {
                logger.debug("Consumed-attributes audit: template source [{}] could not be read", source.getLocation(), e);
            }
        }
        return tokens;
    }

    private static void collect(Set<String> tokens, String content) {
        if (content == null) {
            return;
        }
        Matcher matcher = TOKEN.matcher(content);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
    }

    /**
     * The audit itself, over an already-collected template token set - the part that decides, kept free
     * of the repository so it can be exercised directly.
     *
     * @param modelFileName the model file's name, for the message
     * @param modelText the model file's content
     * @param templateId the template's module path, for the message
     * @param claimedByTemplate the tokens found in the template's sources
     * @return one warning per unclaimed attribute
     */
    static List<String> unconsumed(String modelFileName, String modelText, String templateId, Set<String> claimedByTemplate) {
        Map<String, Object> root;
        try {
            root = ModelJson.parseObject(modelText);
        } catch (RuntimeException e) {
            logger.debug("Consumed-attributes audit skipped: [{}] could not be parsed", modelFileName, e);
            return List.of();
        }
        if (root == null) {
            return List.of();
        }
        Map<String, Object> nested = ModelValues.asMap(root.get("model"));
        Map<String, Object> model = nested != null ? nested : root;
        // Insertion-ordered so the report is stable across generations: one entry per attribute, the
        // first place it was seen, and how many places set it.
        Map<String, Occurrence> unclaimed = new LinkedHashMap<>();
        for (Map<String, Object> entity : ModelValues.asMaps(model.get("entities"))) {
            String entityName = ModelValues.str(entity, "name");
            inspect(unclaimed, entity, claimedByTemplate, "entity", entityName);
            for (Map<String, Object> property : ModelValues.asMaps(entity.get("properties"))) {
                inspect(unclaimed, property, claimedByTemplate, "property", entityName + "." + ModelValues.str(property, "name"));
            }
        }
        for (Map<String, Object> perspective : ModelValues.asMaps(model.get("perspectives"))) {
            inspect(unclaimed, perspective, claimedByTemplate, "perspective", ModelValues.str(perspective, "name"));
        }
        for (Map<String, Object> navigation : ModelValues.asMaps(model.get("navigations"))) {
            inspect(unclaimed, navigation, claimedByTemplate, "navigation", ModelValues.str(navigation, "name"));
        }
        List<String> warnings = new ArrayList<>(unclaimed.size());
        unclaimed.forEach((attribute, occurrence) -> warnings.add(occurrence.describe(modelFileName, attribute, templateId)));
        return warnings;
    }

    /**
     * Records every attribute of one model element that carries a meaningful value and is claimed by
     * nobody.
     */
    private static void inspect(Map<String, Occurrence> unclaimed, Map<String, Object> element, Set<String> claimedByTemplate, String scope,
            String elementName) {
        for (Map.Entry<String, Object> attribute : element.entrySet()) {
            String name = attribute.getKey();
            if (!isMeaningful(attribute.getValue()) || claimedByTemplate.contains(name) || PIPELINE_CLAIMED.contains(name)
                    || EDITOR_OWNED.contains(name)) {
                continue;
            }
            unclaimed.computeIfAbsent(name, key -> new Occurrence(scope, elementName)).count++;
        }
    }

    /**
     * Whether an attribute's value asks for anything. A nested object or list is not an attribute; a
     * null, blank, {@code false} or zero value asks for no behaviour, so no behaviour can be missing
     * from the generated code because nobody read it.
     */
    private static boolean isMeaningful(Object value) {
        if (value == null || value instanceof Map || value instanceof Iterable) {
            return false;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0d;
        }
        String text = String.valueOf(value)
                            .trim();
        return !text.isEmpty() && !"false".equalsIgnoreCase(text);
    }

    /** The first place an unclaimed attribute was seen, and how many places set it. */
    private static final class Occurrence {

        private final String scope;
        private final String elementName;
        private int count;

        private Occurrence(String scope, String elementName) {
            this.scope = scope;
            this.elementName = elementName;
        }

        private String describe(String modelFileName, String attribute, String templateId) {
            String where = count == 1 ? scope + " [" + elementName + "]" : scope + " [" + elementName + "] and " + (count - 1) + " more";
            return "Attribute [" + attribute + "] is set on " + where + " in [" + modelFileName + "] but no source of template ["
                    + templateId + "] reads it and no generation stage claims it - what it asks for will be absent from the generated code";
        }
    }

}
