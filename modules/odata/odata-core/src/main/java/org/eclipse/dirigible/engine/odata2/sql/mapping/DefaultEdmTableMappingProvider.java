/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.mapping;

import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.fqn;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingFactory;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;

/**
 * The Class DefaultEdmTableMappingProvider.
 */
public class DefaultEdmTableMappingProvider implements EdmTableBindingProvider {

    /** The bindings. */
    private final Map<String, EdmTableBinding> bindings;

    /** The mapping not found msg. */
    private final String MAPPING_NOT_FOUND_MSG = "Invalid table mapping configuration for entity %s";


    /**
     * Instantiates a new default edm table mapping provider.
     *
     * @param resources the resources
     * @throws ODataException the o data exception
     */
    public DefaultEdmTableMappingProvider(String... resources) throws ODataException {
        bindings = new HashMap<String, EdmTableBinding>();

        fillBindings(resources);
    }

    /**
     * Fill bindings.
     *
     * @param resources the resources
     * @throws ODataException the o data exception
     */
    protected void fillBindings(String... resources) throws ODataException {
        EdmTableBindingFactory tableBindingFactory = new EdmTableBindingFactory();

        for (String resource : resources) {
            EdmTableBinding binding =
                    loadEdmTableBinding(tableBindingFactory, DefaultEdmTableMappingProvider.class.getClassLoader(), resource);
            String fqn = binding.getEdmFullyQualifedName();
            bindings.put(fqn, binding);
        }
    }

    /**
     * Gets the bindings.
     *
     * @return the bindings
     */
    protected Map<String, EdmTableBinding> getBindings() {
        return bindings;
    }

    /**
     * Gets the edm table binding.
     *
     * @param entityType the entity type
     * @return the edm table binding
     */
    @Override
    public EdmTableBinding getEdmTableBinding(EdmStructuralType entityType) {
        return this.getTableBinding(fqn(entityType));

    }

    /**
     * Load edm table binding.
     *
     * @param tableBindingFactory the table binding factory
     * @param mappingsLoader the mappings loader
     * @param resource the resource
     * @return the edm table binding
     */
    private EdmTableBinding loadEdmTableBinding(EdmTableBindingFactory tableBindingFactory, ClassLoader mappingsLoader, String resource) {
        try {
            return tableBindingFactory.createTableBinding(mappingsLoader, resource);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(MAPPING_NOT_FOUND_MSG, resource));
        }
    }

    /**
     * Load edm table binding.
     *
     * @param tableBindingFactory the table binding factory
     * @param resource the resource
     * @return the edm table binding
     */
    public EdmTableBinding loadEdmTableBinding(EdmTableBindingFactory tableBindingFactory, String resource) {
        try {
            return tableBindingFactory.createTableBinding(new ByteArrayInputStream(resource.getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(MAPPING_NOT_FOUND_MSG, resource));
        }
    }

    /**
     * Gets the table binding.
     *
     * @param fqn the fqn
     * @return the table binding
     */
    private EdmTableBinding getTableBinding(String fqn) {
        EdmTableBinding binding = bindings.get(fqn);
        if (binding == null) {
            throw new IllegalArgumentException("Unable to find binding for FQN " + fqn);
        } else {
            return binding;
        }

    }

}
