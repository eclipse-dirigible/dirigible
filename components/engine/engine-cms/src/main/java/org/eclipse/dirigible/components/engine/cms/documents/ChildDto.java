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

/**
 * A folder's child as both Documents user interfaces expect it.
 *
 * @param name the child's name
 * @param type the CMIS type id ({@code cmis:folder} or {@code cmis:document})
 * @param id the CMIS object id
 * @param path the absolute CMS path
 * @param readOnly whether the caller may not write it
 * @param readable whether the caller may read it
 */
public record ChildDto(String name, String type, String id, String path, boolean readOnly, boolean readable) {
}
