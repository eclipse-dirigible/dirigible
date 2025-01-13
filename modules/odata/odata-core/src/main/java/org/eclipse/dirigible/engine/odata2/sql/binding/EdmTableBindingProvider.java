/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.binding;

import org.apache.olingo.odata2.api.edm.EdmStructuralType;

/**
 * The Interface EdmTableBindingProvider.
 */
public interface EdmTableBindingProvider {

    /**
     * Gets the edm table binding.
     *
     * @param entityType the entity type
     * @return the edm table binding
     */
    public EdmTableBinding getEdmTableBinding(EdmStructuralType entityType);
}
