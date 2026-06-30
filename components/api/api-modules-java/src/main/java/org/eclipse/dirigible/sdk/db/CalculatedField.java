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

/**
 * Server-side contract for computing a calculated field with hand-written Java, used when the value
 * is harder to model than to code (e.g. conditional number generation, lookups against other
 * tables). It is the call-out alternative to the neutral arithmetic expression that the generated
 * repository otherwise evaluates via {@code org.eclipse.dirigible.sdk.utils.Calc}.
 *
 * <p>
 * Unlike a neutral expression - which the generated UI mirrors and previews on the client - an
 * action runs only on the server: the repository invokes it on create and/or update (per the
 * model's calculated-field configuration) just before persisting, and the UI shows the result only
 * after the round-trip.
 *
 * <p>
 * The implementation is a {@link org.eclipse.dirigible.sdk.component.Component @Component} resolved
 * by the generated repository through {@link org.eclipse.dirigible.sdk.component.Beans Beans}, so
 * it can inject other beans; the generated entity type is referenced from the entity-level
 * <em>Imports</em> declared on the model.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class OrderNumberAction implements CalculatedField&lt;OrderEntity, String&gt; {
 *     public String calculate(OrderEntity entity) { ... }
 * }
 * </pre>
 *
 * @param <E> the entity type whose fields supply the inputs
 * @param <T> the type of the calculated field this action populates
 */
public interface CalculatedField<E, T> {

    /**
     * Computes the value to assign to the calculated field.
     *
     * @param entity the entity being persisted, with its other fields already populated
     * @return the value for the calculated field
     */
    T calculate(E entity);

}
