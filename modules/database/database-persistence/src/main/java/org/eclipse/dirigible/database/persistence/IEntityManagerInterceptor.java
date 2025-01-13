/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence;

import java.lang.reflect.Field;

/**
 * The Entity Manager Interceptor interface.
 */
public interface IEntityManagerInterceptor {

    /**
     * On set value before update.
     *
     * @param index the index
     * @param dataType the data type
     * @param value the value
     * @return the object
     */
    public Object onGetValueBeforeUpdate(int index, String dataType, Object value);

    /**
     * On set value after query.
     *
     * @param pojo the pojo
     * @param field the field
     * @param value the value
     * @return the object
     */
    public Object onSetValueAfterQuery(Object pojo, Field field, Object value);

}
