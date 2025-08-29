/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import java.io.IOException;

/**
 * The Interface CmisDocument.
 */
public interface CmisDocument extends CmisObject {
    /**
     * Returns the Path of this CmisDocument.
     *
     * @return the path
     */
    String getPath();

    /**
     * Returns the CmisContentStream representing the contents of this CmisDocument.
     *
     * @return Content Stream
     */
    CmisContentStream getContentStream() throws IOException;
}
