/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.processor.ODataProcessor;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLQueryBuilder;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * The Interface SQLProcessor.
 */
public interface SQLProcessor extends ODataProcessor {

    /**
     * Getter for the default data source.
     *
     * @return the default data source
     */
    DataSource getDataSource();


    /**
     * Getter for the default sql builder.
     *
     * @return the default sql builder
     */
    SQLQueryBuilder getSQLQueryBuilder();

    /**
     * This callback method can be used to customize the property value that is being written. The
     * default implementation returns the provided value. Note that this method is dependent on the
     * sequence of the property in the entity. If this property depends on other properties
     *
     * @param entityType edm entity type
     * @param property EDM property of the model that is currently being filled with data.
     * @param entityInstance The EDM instance which will is being populated with the DB data
     * @param value The DB value of the property. <code>null</code> if the property is not mapped to DB.
     * @return the customized property value. If you do not customize then just return the
     *         <code>value</code>
     * @throws EdmException in case of an error
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    Object onCustomizePropertyValue(EdmStructuralType entityType, EdmProperty property, Object entityInstance, Object value)
            throws EdmException, SQLException, IOException;


    /**
     * On customize expanded navigaton property.
     *
     * @param entityType the entity type
     * @param expandType the expand type
     * @param expandInstance the expand instance
     * @return the customized navigation property
     * @throws EdmException in case of an error
     */
    Map<String, Object> onCustomizeExpandedNavigatonProperty(EdmStructuralType entityType, EdmStructuralType expandType,
            Map<String, Object> expandInstance) throws EdmException;

}
