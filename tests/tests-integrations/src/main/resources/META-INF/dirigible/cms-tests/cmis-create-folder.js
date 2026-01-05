/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Cmis as cmis } from '@aerokit/sdk/cms/cmis';
import { Assert } from 'test/assert';

const session = cmis.getSession();

const rootFolder = session.getRootFolder();

const properties = {};
properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_FOLDER;
properties[cmis.NAME] = 'test1';
const result = rootFolder.createFolder(properties);

Assert.assertTrue(result !== null && result !== undefined);
