/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.camel;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

/**
 * The order replication that transforms and upserts in TypeScript loads the orders it was given,
 * order by order.
 */
public class CamelExtractTransformLoadTypescriptIT extends BaseExtractTransformLoadIT {

    private static final String PROJECT = "CamelExtractTransformLoadTypescriptIT";

    @Test
    void theOrdersAreReplicated() {
        projectDeployer.deploy(PROJECT);

        assertLogContainsMessage(camelLogAsserter, "Replicating orders from OpenCart using TypeScript", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "About to upsert Open cart order [1] using exchange rate", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "Upserted Open cart order [1]", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "About to upsert Open cart order [2] using exchange rate", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "Upserted Open cart order [2]", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Successfully replicated orders from OpenCart using TypeScript", Level.INFO);
        assertDatabaseETLCompletion();
    }
}
