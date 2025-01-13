/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import org.eclipse.dirigible.repository.api.IRepository;

/**
 * The Class CmisInternalRepository.
 */
public class CmisInternalRepository implements CmisRepository {

    /** The internal repository. */
    private IRepository internalRepository;

    /**
     * Instantiates a new cmis internal repository.
     *
     * @param internalRepository the internal repository
     */
    public CmisInternalRepository(IRepository internalRepository) {
        super();
        this.internalRepository = internalRepository;
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    @Override
    public CmisInternalSession getSession() {
        return new CmisInternalSession(this);
    }

    /**
     * Gets the internal object.
     *
     * @return the internal object
     */
    @Override
    public Object getInternalObject() {
        return this.internalRepository;
    }

}
