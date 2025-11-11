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
import { rs } from '@aerokit/sdk/http';
import { Configurations } from '@aerokit/sdk/core';

rs.service()
	.resource('')
	.post(function (ctx, request) {
		let data = request.getJSON();
		for (let i = 0; i < data.env.length; i++) {
			Configurations.set(data.env[i].key, data.env[i].value);
		}
	})
	.execute();
