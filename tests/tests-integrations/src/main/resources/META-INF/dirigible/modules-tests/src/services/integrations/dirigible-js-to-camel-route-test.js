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
import { test, assertEquals } from "sdk/junit"
import { Integrations } from "sdk/integrations"

test('dirigible-js-to-camel-route-test', () => {
    const message = "Initial Message";
    const expected = `${message} -> camel route inbound1 handled this message`;
    const actual = Integrations.invokeRoute('direct:inbound1', message, []);
    assertEquals("Received an unexpected message from route inbound1 ", expected, actual);
});
