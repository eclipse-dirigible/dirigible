/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the refusal of a delete that other records still reference (intent
 * {@code whenTargetDeleted: restrict}, dirigible #7547), its message resolved for the language of
 * the request exactly as a generated check's is - see {@link CheckMessages} (dirigible #8569).
 *
 * <p>
 * One refusal names EVERY kind of record that references the deleted one, each with its count, so
 * the person deleting it learns at once what has to go first. The sentence, and the way one kind of
 * reference is written into it, are entries of the deleted entity's model catalog, under
 * {@code <catalog>.checks.deleteReferenced} and {@code <catalog>.checks.deleteReference}; the
 * entities are named by their labels in their own models' catalogs ({@code <catalog>.t.<name>}). A
 * language catalog that does not carry an entry falls back to the default-language text below, and
 * an entity without a catalog key to the English label it is given.
 */
public final class DeleteRefusals {

    /** The key, under a catalog's {@code checks}, of the sentence refusing a referenced record. */
    public static final String REFERENCED_KEY = "deleteReferenced";

    /**
     * The default-language sentence refusing a referenced record: {@code {entity}} is its label,
     * {@code {references}} the referencing kinds, each written as {@link #REFERENCE_TEXT}.
     */
    public static final String REFERENCED_TEXT = "This {entity} is referenced by {references} and cannot be deleted";

    /** The key, under a catalog's {@code checks}, of one kind of reference inside the sentence. */
    public static final String REFERENCE_KEY = "deleteReference";

    /**
     * The default-language text of one kind of reference: {@code {count}} records of the entity
     * labelled {@code {label}}.
     */
    public static final String REFERENCE_TEXT = "{count} {label} record(s)";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a record the database found
     * referenced by records no delete rule counts.
     */
    public static final String REFERENCED_BY_OTHERS_KEY = "deleteReferencedByOthers";

    /** The default-language sentence refusing a record referenced by records no rule counts. */
    public static final String REFERENCED_BY_OTHERS_TEXT = "This {entity} is referenced by other records and cannot be deleted";

    /** How the kinds of reference are joined inside the sentence. */
    private static final String SEPARATOR = ", ";

    /**
     * One kind of record referencing the record being deleted.
     *
     * @param label the referencing entity's default-language label, e.g. {@code Expense Claim}
     * @param labelKey the fully qualified catalog key of that label, or {@code null} when it has none
     * @param count how many records of that entity reference the deleted one
     */
    public record Reference(String label, String labelKey, int count) {
    }

    private DeleteRefusals() {}

    /**
     * The refusal of a record that the given records reference, resolved for the request's language.
     *
     * @param catalog the deleted entity's model catalog ({@code <project>:<catalog prefix>}), or
     *        {@code null} for the default-language text
     * @param entityLabel the deleted entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @param references every kind of record referencing it, in the order the refusal lists them
     * @return the exception to throw - mapped to HTTP 409 carrying the key and the parameters
     */
    public static DeleteRestrictionException referenced(String catalog, String entityLabel, String entityLabelKey,
            List<Reference> references) {
        return referenced(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey, references);
    }

    /**
     * The refusal of a record the database found referenced by records no delete rule counts - a
     * foreign key the generated rules do not know - resolved for the request's language.
     *
     * @param catalog the deleted entity's model catalog ({@code <project>:<catalog prefix>}), or
     *        {@code null} for the default-language text
     * @param entityLabel the deleted entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 409 carrying the key and the parameters
     */
    public static DeleteRestrictionException referencedByOthers(String catalog, String entityLabel, String entityLabelKey) {
        return referencedByOthers(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey);
    }

    static DeleteRestrictionException referenced(String language, CheckMessages.CatalogReader catalogs, String catalog, String entityLabel,
            String entityLabelKey, List<Reference> references) {
        String referenceKey = checkKey(catalog, REFERENCE_KEY);
        List<String> listed = new ArrayList<>(references.size());
        for (Reference reference : references) {
            String label = CheckMessages.resolve(language, catalogs, reference.labelKey(), reference.label(), Map.of());
            listed.add(CheckMessages.resolve(language, catalogs, referenceKey, REFERENCE_TEXT,
                    CheckMessages.params("count", reference.count(), "label", label)));
        }
        Map<String, Object> params = CheckMessages.params("entity", label(language, catalogs, entityLabelKey, entityLabel), "references",
                String.join(SEPARATOR, listed));
        return refusal(language, catalogs, checkKey(catalog, REFERENCED_KEY), REFERENCED_TEXT, params);
    }

    static DeleteRestrictionException referencedByOthers(String language, CheckMessages.CatalogReader catalogs, String catalog,
            String entityLabel, String entityLabelKey) {
        Map<String, Object> params = CheckMessages.params("entity", label(language, catalogs, entityLabelKey, entityLabel));
        return refusal(language, catalogs, checkKey(catalog, REFERENCED_BY_OTHERS_KEY), REFERENCED_BY_OTHERS_TEXT, params);
    }

    private static DeleteRestrictionException refusal(String language, CheckMessages.CatalogReader catalogs, String messageKey, String text,
            Map<String, Object> params) {
        return new DeleteRestrictionException(CheckMessages.resolve(language, catalogs, messageKey, text, params), messageKey, params);
    }

    private static String label(String language, CheckMessages.CatalogReader catalogs, String labelKey, String label) {
        return CheckMessages.resolve(language, catalogs, labelKey, label, Map.of());
    }

    /** The fully qualified key of a catalog's {@code checks} entry; {@code null} without a catalog. */
    private static String checkKey(String catalog, String key) {
        return catalog == null || catalog.isBlank() ? null : catalog + ".checks." + key;
    }
}
