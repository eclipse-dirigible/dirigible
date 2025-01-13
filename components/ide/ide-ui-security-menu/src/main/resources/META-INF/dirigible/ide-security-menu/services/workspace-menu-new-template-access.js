/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
export function getTemplate() {
	return {
		name: "access",
		label: "Access Constraints",
		extension: "access",
		data: JSON.stringify({
			constraints: [{
				path: "/myproject/myfolder/myservice.js",
				method: "GET",
				scope: "HTTP",
				roles: [
					"administrator",
					"operator"
				]
			}]
		}, null, 4)
	};
};