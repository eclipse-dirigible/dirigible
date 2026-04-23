/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.sharepoint.repository;

import org.eclipse.dirigible.components.api.sharepoint.SharepointFacade;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.ObjectType;

import java.io.IOException;

public class CmisSharepointObject implements CmisObject {

    private final String id;

    private final String name;

    private boolean typeCollection = false;

    public CmisSharepointObject(String id, String name) {
        id = sanitize(id);
        this.id = id;
        this.name = name;
        this.typeCollection = id.endsWith("/");
    }

    @Override
    public String sanitize(String path) {
        return path.replace("\\", "")
                   .replace("//", "/");
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ObjectType getType() {
        return this.isCollection() ? ObjectType.FOLDER : ObjectType.DOCUMENT;
    }

    protected boolean isCollection() {
        return typeCollection;
    }

    @Override
    public void delete(boolean allVersions) throws IOException {
        delete();
    }

    @Override
    public void delete() {
        if (this.typeCollection) {
            SharepointFacade.deleteFolder(this.id);
        } else {
            SharepointFacade.delete(this.id);
        }
    }

    @Override
    public void rename(String newName) {
        throw new UnsupportedOperationException("Rename not supported");
    }

    @Override
    public String toString() {
        return "CmisSharepointObject{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", typeCollection=" + typeCollection + '}';
    }
}
