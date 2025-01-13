/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.odata.api.ODataEntity;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.domain.ODataHandler;

/**
 * The Class OData2ODataHTransformer.
 */
public class OData2ODataHTransformer {

    /**
     * Transform.
     *
     * @param model the model
     * @return the list
     * @throws SQLException the SQL exception
     */
    public List<ODataHandler> transform(OData model) throws SQLException {

        List<ODataHandler> result = new ArrayList<>();

        for (ODataEntity entity : model.getEntities()) {
            String namespace = model.getNamespace();
            String name = entity.getName();

            entity.getHandlers()
                  .forEach(handler -> {
                      ODataMetadataUtil.validateHandlerDefinitionMethod(handler.getMethod(), entity.getName());
                      ODataMetadataUtil.validateHandlerDefinitionTypes(handler.getType(), entity.getName());

                      ODataHandler handlerModel = new ODataHandler();
                      handlerModel.setNamespace(namespace);
                      handlerModel.setName(name + "Type");
                      handlerModel.setMethod(handler.getMethod());
                      handlerModel.setKind(handler.getType());
                      handlerModel.setHandler(handler.getHandler());
                      result.add(handlerModel);
                  });
        }
        return result;
    }
}
