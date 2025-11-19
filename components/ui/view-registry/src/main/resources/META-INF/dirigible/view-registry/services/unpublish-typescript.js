/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { repository } from "@aerokit/sdk/platform"

const path = __context.get('path');

if (path && path.endsWith(".ts")) {
    const jsPath = path.replace(".ts", ".js");
    repository.deleteResource(jsPath);
}
