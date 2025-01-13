/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.factory;

import java.util.List;

import org.apache.olingo.odata2.api.exception.ODataException;
import org.eclipse.dirigible.components.odata.domain.ODataMapping;
import org.eclipse.dirigible.components.odata.service.ODataMappingService;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBinding;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingFactory;
import org.eclipse.dirigible.engine.odata2.sql.mapping.DefaultEdmTableMappingProvider;

/**
 * The Class DirigibleEdmTableMappingProvider.
 */
public class ODataEdmTableMappingProvider extends DefaultEdmTableMappingProvider {

    /**
     * Instantiates a new o data edm table mapping provider.
     *
     * @throws ODataException the o data exception
     */
    public ODataEdmTableMappingProvider() throws ODataException {
        super();
    }

    /**
     * Fill bindings.
     *
     * @param resources the resources
     * @throws ODataException the o data exception
     */
    @Override
    protected void fillBindings(String... resources) throws ODataException {
        EdmTableBindingFactory tableBindingFactory = new EdmTableBindingFactory();

        List<ODataMapping> mappings = ODataMappingService.get()
                                                         .getAll();
        for (ODataMapping mapping : mappings) {
            EdmTableBinding binding = loadEdmTableBinding(tableBindingFactory, new String(mapping.getContent()));
            String fqn = binding.getEdmFullyQualifedName();
            getBindings().put(fqn, binding);
        }
    }


}
