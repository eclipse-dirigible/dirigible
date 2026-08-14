/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

/**
 * One requirement the intent DSL cannot express, reported alongside the proposal instead of being
 * quietly downgraded to the nearest expressible thing.
 *
 * <p>
 * This is the structured half of the honesty contract: a requirement that becomes a manual step, an
 * escape hatch or nothing at all must be visible <em>as such</em> to the developer - and
 * forwardable verbatim, because a boundary somebody actually hit is the only reliable signal of
 * which DSL gap is worth closing.
 *
 * @param requirement the developer's requirement, in their words
 * @param explanation why the intent layer does not express it, and what the proposal does instead
 * @param extensionKind the extension point that carries it - e.g. {@code calculatedAction},
 *        {@code delegate}, {@code camelRoute}, {@code printTemplate}, {@code customPage}, or
 *        {@code none} when the proposal simply omits it
 * @param suggestedClass the class the developer will hand-write, when the extension point is a Java
 *        one; {@code null} otherwise
 */
record AgentBoundary(String requirement, String explanation, String extensionKind, String suggestedClass) {
}
