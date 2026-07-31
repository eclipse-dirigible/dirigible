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
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.NumberIntent;

/**
 * Builds the {@code numbering} glue collection: one descriptor per {@code number: { stampOn: issue
 * }} field, driving the generated {@code gen/events/<Entity>NumberStamp.java} delegate. The
 * document is created with a UUID placeholder (the uuid auto-fill); this delegate, wired as a
 * {@code delegate:} service task at the issue step, replaces it with the real formatted number -
 * idempotently, so a re-issue after an amend keeps the number.
 *
 * <p>
 * The number is allocated + formatted via {@code sdk.numbering.DocumentNumbers} (the shared
 * per-tenant counter) and written with the targeted {@code updateProperty} (a workflow system
 * write).
 */
final class NumberingSupport {

    private NumberingSupport() {}

    /**
     * One numbering descriptor per {@code stampOn: issue} number field in the model.
     *
     * @param model the parsed intent model
     * @param compositionParents each entity's transitive composition parent (perspective resolution)
     * @return the {@code numbering} collection (possibly empty)
     */
    static List<Map<String, Object>> buildNumbering(IntentModel model, Map<String, String> compositionParents) {
        List<Map<String, Object>> numbering = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            for (FieldIntent field : entity.getFields()) {
                NumberIntent number = field.getNumber();
                if (number == null || !"issue".equalsIgnoreCase(number.getStampOn())) {
                    continue; // stampOn:create is handled at insert by the DAO; only issue needs a step
                }
                List<String> scope = new ArrayList<>();
                if (number.getScope() != null) {
                    for (String scopeName : number.getScope()) {
                        scope.add("year".equalsIgnoreCase(scopeName) ? "year" : IntentNaming.pascalCase(scopeName));
                    }
                }
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("entity", entity.getName());
                descriptor.put("perspective", IntentEntities.resolvePerspective(entity.getName(), compositionParents));
                descriptor.put("masterPk", IntentEntities.keyFieldName(entity));
                descriptor.put("field", IntentNaming.pascalCase(field.getName()));
                descriptor.put("series", number.getSeries() == null ? entity.getName() : number.getSeries());
                descriptor.put("format", number.getFormat() == null ? "" : number.getFormat());
                descriptor.put("scope", scope);
                numbering.add(descriptor);
            }
        }
        return numbering;
    }

    /**
     * One descriptor per {@code number:} field REGARDLESS of {@code stampOn} - the series the
     * application declares. The stamp collection above deliberately covers only {@code stampOn: issue}
     * (a create-time number is stamped by the DAO and needs no process step), but the management
     * surface must list every series, including those whose first document does not exist yet, so
     * declaration needs the full set.
     *
     * @param model the parsed intent model
     * @return the {@code numberSeries} collection (possibly empty)
     */
    static List<Map<String, Object>> buildNumberSeries(IntentModel model) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            for (FieldIntent field : entity.getFields()) {
                NumberIntent number = field.getNumber();
                if (number == null) {
                    continue;
                }
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("entity", entity.getName());
                descriptor.put("field", IntentNaming.pascalCase(field.getName()));
                descriptor.put("series", number.getSeries() == null ? entity.getName() : number.getSeries());
                descriptor.put("format", number.getFormat() == null ? "" : number.getFormat());
                series.add(descriptor);
            }
        }
        return series;
    }
}
