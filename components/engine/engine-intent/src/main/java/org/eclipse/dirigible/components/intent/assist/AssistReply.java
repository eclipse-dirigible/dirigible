/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.util.List;

/**
 * The assistant's response for one turn.
 *
 * @param reply the assistant's text reply (explanation or answer)
 * @param proposedSource the complete proposed Java source when the assistant proposed an edit, else
 *        {@code null}
 * @param diagnostics the compiler errors still outstanding on {@code proposedSource} after the
 *        repair rounds; empty when it compiles. A proposal is never withheld because of them - the
 *        developer decides - but they are never hidden either
 */
record AssistReply(String reply, String proposedSource, List<AssistDiagnostic> diagnostics) {
}
