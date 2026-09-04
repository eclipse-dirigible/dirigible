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
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.UsesIntent;

/**
 * The one question a {@code generates} whose ends may live in other models has to be asked before
 * anything is emitted for it: has the owner model been generated at all? (dirigible #6539)
 *
 * <p>
 * A MUTUAL cross-model pair has no project to generate first - model A mints a document into model
 * B while B holds a foreign key back to A - so the ordinary "generate the dependency first" is
 * advice neither side can follow. A declared BOOTSTRAP pass
 * ({@link IntentGenerationContext#isBootstrap()}) skips exactly that create-from; every other pass
 * fails with the recipe.
 *
 * <p>
 * It lives here, and not in either generator, because BOTH halves of a create-from must make the
 * same decision: the server controller comes from the {@code .glue}'s {@code generates} collection
 * and the client button from its own {@code .extension} descriptor, so a bootstrap pass that
 * emitted only the second would ship a button whose endpoint does not exist.
 */
public final class GeneratesBootstrap {

    private GeneratesBootstrap() {}

    /**
     * The cross-model owner of one end of a create-from whose {@code .model} is not there yet.
     *
     * @param role which end - {@code target} or {@code source} - reads into the message as authored
     * @param entity the entity that end names
     * @param alias the {@code uses:} alias owning it
     */
    public record AbsentOwner(String role, String entity, String alias) {
    }

    /**
     * The first end of a create-from - its {@code uses:} target, then its {@code fromUses:} source -
     * whose owner model has not been generated yet, or {@code null} when both are resolvable (or the
     * create-from is entirely local). The target is asked first because it is the end a mutual cycle is
     * normally authored on.
     *
     * <p>
     * Only the ABSENCE of the owner's {@code .model} counts: an owner that is present but declares no
     * such entity keeps failing loudly through the ordinary resolution, in a bootstrap pass as in any
     * other - "the dependency is not generated yet" and "the reference is wrong" want opposite answers,
     * and the second is the one a bootstrap flag could otherwise hide forever.
     *
     * @param g the create-from
     * @param model the intent model declaring it (for the {@code uses:} aliases)
     * @param context the generation context
     * @return the absent owner, or {@code null} when there is none
     */
    public static AbsentOwner absentOwner(GeneratesIntent g, IntentModel model, IntentGenerationContext context) {
        AbsentOwner target = absentOwner("target", g.getTo(), g.getUses(), model, context);
        return target != null ? target : absentOwner("source", g.getFrom(), g.getFromUses(), model, context);
    }

    /**
     * Whether this pass must leave the create-from out altogether: its owner model is missing AND the
     * caller declared a bootstrap.
     *
     * @param g the create-from
     * @param model the intent model declaring it
     * @param context the generation context
     * @return {@code true} when nothing is to be emitted for it
     */
    public static boolean skipped(GeneratesIntent g, IntentModel model, IntentGenerationContext context) {
        return context != null && context.isBootstrap() && absentOwner(g, model, context) != null;
    }

    /**
     * The refusal a default pass answers with - naming the cycle and the way out of it, because the
     * generic "generate the [alias] model first" is unactionable for a mutual pair.
     *
     * @param actionName the create-from's name
     * @param absent the end whose owner model is missing
     * @return the exception to throw
     */
    public static BootstrapRequiredException required(String actionName, AbsentOwner absent) {
        return new BootstrapRequiredException(List.of("generates [" + actionName + "] " + absent.role() + " [" + absent.entity()
                + "] is owned by the model [" + absent.alias()
                + "], whose .model exists neither in this workspace nor in the registry. Generate the [" + absent.alias()
                + "] model first - or, if [" + absent.alias()
                + "] references this model in turn (a mutual cross-model cycle), run this Generate once in BOOTSTRAP mode, which emits"
                + " everything except this create-from, then generate [" + absent.alias() + "], then regenerate here"));
    }

    /**
     * What a bootstrap pass reports about the create-from it left out - the generation succeeded, so
     * the drop must not live only in a log line.
     *
     * @param actionName the create-from's name
     * @param absent the end whose owner model is missing
     * @return the warning text
     */
    public static String skipWarning(String actionName, AbsentOwner absent) {
        return "generates [" + actionName + "] " + absent.role() + " [" + absent.entity() + "] is owned by the model [" + absent.alias()
                + "], which has not been generated yet - this bootstrap pass emitted everything else; generate [" + absent.alias()
                + "] and regenerate this project to emit the create-from - it was NOT generated";
    }

    private static AbsentOwner absentOwner(String role, String entity, String alias, IntentModel model, IntentGenerationContext context) {
        if (alias == null || alias.isBlank()) {
            return null; // a local end - nothing to resolve
        }
        UsesIntent uses = null;
        for (UsesIntent declared : model.getUses()) {
            if (alias.equals(declared.getModel())) {
                uses = declared;
            }
        }
        if (uses == null) {
            return null; // the parser already reported the unknown alias
        }
        return CrossModelSupport.ownerModelExists(context, uses) ? null : new AbsentOwner(role, entity, alias);
    }
}
