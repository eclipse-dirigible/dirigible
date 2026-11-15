/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

/**
 * What a provider answered.
 *
 * @param json the JSON answer following the requested schema
 * @param model the model that produced it
 * @param inputTokens tokens read
 * @param outputTokens tokens written
 */
public record CaptureAnswer(String json, String model, int inputTokens, int outputTokens) {
}
