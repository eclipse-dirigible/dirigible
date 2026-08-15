/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.List;

/**
 * The entity's declarative state machine: the WHOLE set of legal status edges, declared once over
 * the status nomenclature and enforced on every status write.
 *
 * <pre>
 * - name: SalesInvoice
 *   lifecycle:
 *     edges:
 *       - { from: DRAFT,  to: [ISSUED, CANCELLED] }
 *       - { from: ISSUED, to: [PAID, VOIDED] }
 * </pre>
 *
 * The graph is always over the entity's {@code function: EntityStatus} relation - the one FK its
 * lifecycle is expressed through - so it names no column of its own. (An {@code on:} key naming
 * that relation would be redundant, and YAML 1.1 reads a bare {@code on} as the boolean
 * {@code true}, so it could not even be authored unquoted; the parser rejects it rather than
 * dropping it silently.)
 *
 * Without it the status machinery is a set of point constructs - {@code init:} names the start,
 * {@code transitions:} guards the flips that happen to go through a button, a workflow branch's
 * {@code setRelationField} writes one unguarded - and nothing states which edges are legal at all,
 * so a glue action or an API write can jump a document from any status to any other. The lifecycle
 * closes that: an edge the graph does not declare is rejected wherever the write comes from (user
 * form, workflow setter, transition button, custom action), and a {@code transitions:} entry
 * becomes PRESENTATION over an edge - its {@code from}/{@code setStatus} pair must be one, so the
 * buttons cannot silently disagree with the graph.
 *
 * <p>
 * Statuses are referenced by their seeded name or by their seed id, like every other status site
 * (see {@code StatusSymbolResolver}). The nomenclature must be seeded in THIS model - a cross-model
 * status entity is seeded in its owner model, where its lifecycle belongs.
 *
 * <p>
 * Composes with the {@code stage:} classification: a stage says what a status MEANS (draft / live /
 * cancelled / void) and scopes reports by it; the lifecycle says how a record may MOVE between
 * them.
 */
public class LifecycleIntent {

    /** The legal edges - one entry per source status. */
    private List<LifecycleEdgeIntent> edges;

    public List<LifecycleEdgeIntent> getEdges() {
        return edges == null ? List.of() : edges;
    }

    public void setEdges(List<LifecycleEdgeIntent> edges) {
        this.edges = edges;
    }
}
