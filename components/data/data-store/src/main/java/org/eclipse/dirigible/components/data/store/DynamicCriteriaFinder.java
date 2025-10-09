/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

public class DynamicCriteriaFinder {

    /**
     * Executes a Find-by-Example query against a dynamic entity using HQL. HQL is required because the
     * entity is only defined by its 'entity-name' string.
     *
     * @param sessionFactory Hibernate Session Factory.
     * @param entityName The Hibernate entity-name (e.g., "Order" or "OrderItem").
     * @param exampleMap A Map containing the properties (keys) and values to match.
     * @param limit The max number of records
     * @param offset The starting number of records
     * @return A list of result Maps matching the criteria.
     */
    public static List<Map> findByExampleDynamic(SessionFactory sessionFactory, String entityName, Map<String, Object> exampleMap,
            int limit, int offset) {

        try (Session session = sessionFactory.openSession()) {

            StringBuilder hql = new StringBuilder("SELECT e FROM " + entityName + " e WHERE 1=1");

            Map<String, Object> params = new HashMap<>();
            int paramIndex = 0;

            for (Map.Entry<String, Object> entry : exampleMap.entrySet()) {
                String propertyName = entry.getKey();
                Object value = entry.getValue();

                // Ignore null values (standard QBE behavior)
                if (value != null) {
                    // Generate a unique parameter name (e.g., "name0", "orderId1")
                    String paramName = propertyName + paramIndex++;

                    // Append the equality condition to the HQL string
                    hql.append(" AND e.")
                       .append(propertyName)
                       .append(" = :")
                       .append(paramName);

                    // Store the value for setting the parameter later
                    params.put(paramName, value);
                }
            }

            Query<Map> query = session.createQuery(hql.toString(), Map.class);

            for (Map.Entry<String, Object> param : params.entrySet()) {
                query.setParameter(param.getKey(), param.getValue());
            }

            if (limit > 0) {
                query.setMaxResults(limit);
            }
            if (offset >= 0) {
                query.setFirstResult(offset);
            }

            return query.getResultList();
        }
    }

}
