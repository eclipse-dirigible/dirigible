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

import java.util.List;

import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;

/**
 * The from-status guard of a create-from (issue #7068) - the one rule, resolved once, for both
 * halves of the action: the generated controller that refuses the run with 409, and the contributed
 * button that stops offering it.
 *
 * <p>
 * A {@code generates} used to be unconditional. With a {@code sourceStatus:} completion hook it
 * flipped the source once the target existed - and then went on offering the same button on the
 * flipped record, so a second click (or a second POST) minted a second document: another invoice
 * for a proforma already INVOICED, in the customer's hands. The hook declared what "already done"
 * looks like; nothing consulted it.
 *
 * <p>
 * Two shapes, one guard. {@code fromStatus: [...]} is the explicit allow-list, the {@code from:} of
 * a transition (spelled differently only because {@code from:} on a create-from already names the
 * source ENTITY). Absent it, a declared {@code sourceStatus} IMPLIES the deny-list of exactly that
 * status - the minimal refusal, and the one the author already declared: a source standing at its
 * post-generation status has been generated from.
 */
public final class GeneratesGuardSupport {

    private GeneratesGuardSupport() {}

    /**
     * A resolved guard over the SOURCE's {@code EntityStatus} foreign key: the record may run the
     * create-from while its status is one of {@code allowed} (when an allow-list was authored) and
     * while it is none of {@code blocked} (the implied one). Exactly one of the two is non-empty.
     *
     * @param statusProperty the source's status FK, PascalCase (e.g. {@code Status})
     * @param allowed the authored allow-list of status seed ids, empty when the guard is implied
     * @param blocked the implied deny-list of status seed ids, empty when an allow-list was authored
     */
    public record Guard(String statusProperty, List<Integer> allowed, List<Integer> blocked) {

        /** The guard as a Java boolean expression over an {@code int currentStatus} local. */
        public String expression() {
            StringBuilder terms = new StringBuilder();
            for (Integer status : allowed.isEmpty() ? blocked : allowed) {
                if (terms.length() > 0) {
                    terms.append(allowed.isEmpty() ? " && " : " || ");
                }
                terms.append("currentStatus ")
                     .append(allowed.isEmpty() ? "!= " : "== ")
                     .append(status);
            }
            return terms.toString();
        }

        /** The human half of the refusal, read after the action's name in the 409 body. */
        public String text(String fromEntity) {
            return allowed.isEmpty()
                    ? "was already generated from this " + fromEntity + " (its status is the one the completion hook writes)"
                    : "is allowed only from status [" + join(allowed) + "]";
        }

        /** The status ids the guard names, for the javadoc of the generated controller. */
        public String statuses() {
            return join(allowed.isEmpty() ? blocked : allowed);
        }

        private static String join(List<Integer> ids) {
            StringBuilder out = new StringBuilder();
            for (Integer id : ids) {
                if (out.length() > 0) {
                    out.append(", ");
                }
                out.append(id);
            }
            return out.toString();
        }
    }

    /**
     * The guard of a create-from, or {@code null} when it has none: nothing to check (no allow-list and
     * no completion hook), no status column on the source to check it against, a {@code page}-scoped
     * action, which acts on the view rather than on a record, or an event-driven one with no button at
     * all - the guard is on the click, and an event trigger carries its own at-most-once guard.
     *
     * @param g the create-from
     * @param statusProperty the source's status FK as the caller already resolved it (may be empty)
     * @return the guard, or null
     */
    public static Guard of(GeneratesIntent g, String statusProperty) {
        if (statusProperty == null || statusProperty.isEmpty() || !"entity".equals(g.getScope()) || !g.hasButton()) {
            return null;
        }
        if (g.hasFromStatus()) {
            return new Guard(statusProperty, List.copyOf(g.getFromStatus()), List.of());
        }
        return g.getSourceStatus() == null ? null : new Guard(statusProperty, List.of(), List.of(g.getSourceStatus()));
    }

    /**
     * The SOURCE's {@code function: EntityStatus} foreign key, PascalCase - read off this model for a
     * local source, and off the owner's already-generated {@code .model} for a cross-model one (the
     * status FK is author-named, so it is never guessed). Empty when the source declares none, or when
     * the owner model is not resolvable here - the button then carries no guard and the generated
     * controller's 409 stays the contract.
     *
     * <p>
     * An entity declaring two {@code function: EntityStatus} relations resolves to the FIRST, through
     * {@link LifecycleStages#statusRelation}: one rule, shared with every other reader of THE status
     * (the transitions guard, the abort support, the lifecycle stages), so the guard the controller
     * enforces and the guard the button mirrors can never be read off different columns (issue #7150).
     *
     * @param g the create-from
     * @param model the model being generated
     * @param context the generation context (may be null outside a Generate)
     * @return the status property, or an empty string
     */
    public static String statusProperty(GeneratesIntent g, IntentModel model, IntentGenerationContext context) {
        if (g.getFrom() == null || g.getFrom()
                                    .isBlank()) {
            return "";
        }
        if (g.isCrossModelSource()) {
            UsesIntent uses = null;
            for (UsesIntent candidate : model.getUses()) {
                if (g.getFromUses()
                     .equals(candidate.getModel())) {
                    uses = candidate;
                }
            }
            if (uses == null || context == null) {
                return "";
            }
            CrossModelSupport.TargetInfo owner = CrossModelSupport.resolve(context, uses, g.getFrom());
            return owner == null || owner.statusProperty() == null ? "" : owner.statusProperty();
        }
        for (EntityIntent entity : model.getEntities()) {
            if (g.getFrom()
                 .equals(entity.getName())) {
                RelationIntent status = LifecycleStages.statusRelation(entity);
                return status == null ? "" : IntentNaming.pascalCase(status.getName());
            }
        }
        return "";
    }

}
