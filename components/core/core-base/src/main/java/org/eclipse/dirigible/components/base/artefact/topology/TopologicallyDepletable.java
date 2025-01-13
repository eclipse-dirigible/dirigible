/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.topology;

import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;

/**
 * The Interface ITopologicallyDepletable.
 */
public interface TopologicallyDepletable {

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId();

    /**
     * Complete.
     *
     * @param flow the flow
     * @return true, if successful
     */
    public boolean complete(ArtefactPhase flow);

}
