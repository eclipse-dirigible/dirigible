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
 * What a provider is asked to read.
 *
 * @param instructions the complete instructions text
 * @param jsonSchema the JSON schema the answer must follow
 * @param fileName the document's file name, may be null
 * @param contentType the document's media type
 * @param content the document's bytes
 */
public record CaptureRequest(String instructions, String jsonSchema, String fileName, String contentType, byte[] content) {
}
