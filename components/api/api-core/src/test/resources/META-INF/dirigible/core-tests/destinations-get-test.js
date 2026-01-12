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
import { Destinations } from '@aerokit/sdk/core/destinations';
import { Assert } from 'test/assert';

const destination = {};
destination.name1 = 'value1';
Destinations.set('destination1', destination);
const result = Destinations.get('destination1');

Assert.assertTrue(result.name1 === 'value1');
