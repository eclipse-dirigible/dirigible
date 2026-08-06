/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import java.util.List;

/**
 * A folder listing as both Documents user interfaces expect it.
 *
 * @param name the folder's name
 * @param id the CMIS object id
 * @param path the absolute CMS path
 * @param parentId the parent's CMIS object id, null for the root
 * @param children the visible children, ordered by path
 */
public record FolderDto(String name, String id, String path, String parentId, List<ChildDto> children) {
}
