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
import { getBrandingJs } from './branding.mjs';

rs.service()
    .resource('')
    .get(function (_ctx, _request, response) {
        response.setContentType("text/javascript");
        response.println(getBrandingJs());
    })
    .execute();
