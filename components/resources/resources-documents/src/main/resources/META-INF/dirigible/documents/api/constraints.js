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
import { rs } from "@aerokit/sdk/http";
import * as constraintsProcessor from "./processors/constraintsProcessor";

rs.service()
    .resource("")
    .get(function (ctx, request, response) {
        if (request.isUserInRole("Operator")) {
            let accessDefinitions = constraintsProcessor.getAccessDefinitions();
            response.println(JSON.stringify(accessDefinitions, null, 2));
        } else {
            response.setStatus(response.FORBIDDEN);
            response.println("Access forbidden");
        }
    })
    .put(function (ctx, request, response) {
        if (request.isUserInRole("Operator")) {
            let accessDefinitions = request.getJSON();
            constraintsProcessor.updateAccessDefinitions(accessDefinitions);
            response.println(JSON.stringify(accessDefinitions, null, 2));
        } else {
            response.setStatus(response.FORBIDDEN);
            response.println("Access forbidden");
        }
    })
    .execute();
