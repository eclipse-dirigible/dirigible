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
import { rs } from 'sdk/http';
import { Configurations } from 'sdk/core';

rs.service()
    .resource('')
    .get(function (ctx, request, response) {
        response.print(Configurations.get("DIRIGIBLE_TRACING_TASK_ENABLED"));
    })
    .post(function (ctx, request, response) {
        let flag = Configurations.get("DIRIGIBLE_TRACING_TASK_ENABLED");
        flag = flag === "true" ? "false" : "true";
        Configurations.set("DIRIGIBLE_TRACING_TASK_ENABLED", flag);
        response.print(flag);
    })
    .execute();