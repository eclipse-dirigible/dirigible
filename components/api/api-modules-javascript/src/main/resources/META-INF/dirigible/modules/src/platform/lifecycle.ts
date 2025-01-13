/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const LifecycleFacade = Java.type("org.eclipse.dirigible.components.api.platform.LifecycleFacade");

export class Lifecycle {

    public static publish(user: string, workspace: string, project: string = "*"): boolean {
        return LifecycleFacade.publish(user, workspace, project);
    }

    public static unpublish(project: string = "*"): boolean {
        return LifecycleFacade.unpublish(project);
    }
}


// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Lifecycle;
}
