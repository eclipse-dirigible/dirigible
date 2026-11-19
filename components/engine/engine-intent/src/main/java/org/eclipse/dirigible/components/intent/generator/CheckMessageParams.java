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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;

/**
 * The record's values a {@code checks:} message names (dirigible #8665): {@code {taxRateCap}} - a
 * field or to-one of the record - and {@code {Category.code}} - a value one to-one hop away. They
 * are resolved against the record as the check judged it and filled in AFTER translation, exactly
 * as the generator's own {@code {count}} / {@code {match}} are (#7611), so a translation may move
 * them; the 400 / 428 body carries them in {@code messageParams}.
 *
 * <p>
 * The generator's own placeholders ({@link #RESERVED}) are left to the code that fills them, so a
 * message that uses only those generates exactly what it did.
 */
public final class CheckMessageParams {

    /** The placeholders the generated code fills itself, never read off the record. */
    public static final Set<String> RESERVED = Set.of("count", "match", "remaining", "capacity", "requested");

    /**
     * {@code {name}} and its i18next spelling {@code {{name}}} - the shapes {@code CheckMessages}
     * fills.
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}|\\{([\\w.]+)\\}");

    private CheckMessageParams() {}

    /**
     * The record placeholders of a message, in order of first appearance, the reserved ones left out.
     *
     * @param message the authored message, may be null
     * @return the placeholder names, never null
     */
    public static List<String> placeholders(String message) {
        Set<String> names = new LinkedHashSet<>();
        if (message == null) {
            return new ArrayList<>();
        }
        Matcher matcher = PLACEHOLDER.matcher(message);
        while (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (!RESERVED.contains(name)) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Why a placeholder does not name a value of the record, or {@code null} when it does: a field or
     * to-one of the record, or one to-one hop and a field or to-one of the record it reaches.
     *
     * @param entity the record's entity
     * @param byName the model's entities by name
     * @param placeholder the placeholder name
     * @return the reason, or {@code null}
     */
    public static String failure(EntityIntent entity, Map<String, EntityIntent> byName, String placeholder) {
        if (placeholder.split("\\.", -1).length > 2) {
            return "{" + placeholder + "} walks more than one hop - a message names a value of the record or of the record one"
                    + " to-one relation points at";
        }
        ResolvePathSupport.Path path = ResolvePathSupport.walker(entity, byName, Map.of(), null)
                                                         .resolve(placeholder);
        return path.resolved() ? null : "{" + placeholder + "}: " + path.failure();
    }

    /**
     * The placeholders as the template-ready entries the message arguments are built from: each carries
     * its {@code name} and the {@code property} read; a value one hop away also carries the {@code hop}
     * that loads the record it is read off - the foreign key's {@code sourceExpression} and the reached
     * entity's coordinates ({@code entity}, {@code perspective}, {@code crossModel},
     * {@code targetModel}), which the template layer turns into the generated repository class.
     *
     * @param entity the record's entity
     * @param byName the model's entities by name
     * @param compositionParents composition-parent map (a hop's perspective)
     * @param crossModel resolver for a cross-model hop
     * @param message the authored message
     * @return the entries, never null; a placeholder that does not resolve is left out (the parser
     *         reported it)
     */
    public static List<Map<String, Object>> params(EntityIntent entity, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, NotificationSupport.CrossModelLookup crossModel, String message) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (String placeholder : placeholders(message)) {
            if (placeholder.split("\\.", -1).length > 2) {
                continue;
            }
            ResolvePathSupport.Walker walker = ResolvePathSupport.walker(entity, byName, compositionParents, crossModel);
            ResolvePathSupport.Path path = walker.resolve(placeholder);
            if (!path.resolved()) {
                continue;
            }
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("name", placeholder);
            param.put("property", path.property());
            if (!walker.hops()
                       .isEmpty()) {
                ResolvePathSupport.Hop hop = walker.hops()
                                                   .get(0);
                Map<String, Object> load = new LinkedHashMap<>();
                load.put("sourceExpression", hop.sourceExpression());
                load.put("entity", hop.entity());
                load.put("perspective", hop.perspective());
                load.put("crossModel", hop.crossModel());
                load.put("targetModel", hop.targetModel());
                param.put("hop", load);
            }
            params.add(param);
        }
        return params;
    }
}
