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
    .get(function (ctx, request, response) {
        response.setContentType('text/plain');
        response.print(Configurations.get('DIRIGIBLE_TRACING_TASK_ENABLED') ?? 'false');
    })
    .post(function (ctx, request, response) {
        const flag = Configurations.get('DIRIGIBLE_TRACING_TASK_ENABLED') === 'true' ? 'false' : 'true';
        Configurations.set('DIRIGIBLE_TRACING_TASK_ENABLED', flag);
        response.setContentType('text/plain');
        response.print(flag);
    })
    .execute();
