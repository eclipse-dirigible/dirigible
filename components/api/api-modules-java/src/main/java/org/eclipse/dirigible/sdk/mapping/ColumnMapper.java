/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.mapping;

import java.util.Map;

/**
 * Server-side contract for computing one target column of a mapping with hand-written Java, used
 * when the value is harder to model than to code (a lookup, a conditional, a format conversion). It
 * is the call-out alternative to the neutral arithmetic expression that the generated mapper
 * otherwise evaluates via {@code org.eclipse.dirigible.sdk.utils.Calc}, and it backs a mapping
 * model column that declares a <em>module</em>.
 *
 * <p>
 * The implementation is a {@link org.eclipse.dirigible.sdk.component.Component @Component} resolved
 * by the generated mapper through {@link org.eclipse.dirigible.sdk.component.Beans Beans}, so it
 * can inject other beans.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class FullNameMapper implements ColumnMapper {
 *     public Object map(Map&lt;String, Object&gt; source, Map&lt;String, Object&gt; target, String columnName) {
 *         return source.get("FIRST_NAME") + " " + source.get("LAST_NAME");
 *     }
 * }
 * </pre>
 */
public interface ColumnMapper {

    /**
     * Computes the value to assign to a target column.
     *
     * @param source the record being mapped, keyed by source column name
     * @param target the record being built, carrying the columns mapped before this one
     * @param columnName the name of the target column being computed
     * @return the value for the target column
     */
    Object map(Map<String, Object> source, Map<String, Object> target, String columnName);

}
