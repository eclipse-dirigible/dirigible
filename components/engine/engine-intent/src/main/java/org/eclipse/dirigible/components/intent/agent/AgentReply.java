/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

import java.util.List;

/**
 * The assistant's response for one turn.
 *
 * @param reply the assistant's text reply (explanation or answer)
 * @param proposedYaml the complete proposed intent YAML when the assistant proposed an edit -
 *        materialized here when it arrived as anchored edits - else {@code null}, which also covers
 *        a patch that could not be applied and whose reasons are appended to {@code reply}
 * @param boundaries the requirements the proposal could <em>not</em> express, each naming the
 *        extension point that carries it; empty when the request fit entirely inside the DSL
 * @param coverage the proposal's requirement-by-requirement audit of the request (dirigible #6997)
 *        - which construct carries each requirement; empty when the reply proposed nothing or came
 *        from a contract without the audit
 */
record AgentReply(String reply, String proposedYaml, List<AgentBoundary> boundaries, List<AgentCoverage> coverage) {
}
