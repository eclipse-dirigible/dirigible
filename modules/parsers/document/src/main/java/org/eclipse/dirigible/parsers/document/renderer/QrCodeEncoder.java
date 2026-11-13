/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.renderer;

/**
 * Turns the payload of a {@code <qrcode>} into an image the PDF renderer can embed.
 *
 * <p>
 * The library is dependency-free, so it models the element, keeps the payload's line breaks and
 * lays the symbol out, but leaves the encoding itself to the host - as it leaves reading an
 * {@code <image>} to an {@link ImageResolver}. An implementation returns a source the renderer
 * emits verbatim (in practice a {@code data:} URI carrying the image inline - the renderer's output
 * is a self-contained stylesheet, so nothing may be fetched later) or {@code null} to render
 * <b>nothing at all</b>: a payload the symbol cannot hold must not cost the document.
 */
@FunctionalInterface
public interface QrCodeEncoder {

    /**
     * Encodes nothing, so no {@code <qrcode>} is rendered - the default, for a renderer used without a
     * host.
     */
    QrCodeEncoder NONE = (payload, level) -> null;

    /**
     * Encodes one payload.
     *
     * @param payload the bound payload, never blank; lines are separated by a line feed
     * @param level the error-correction level
     * @return the image source to emit, or {@code null} to omit the QR code entirely
     */
    String encode(String payload, QrCodeLevel level);
}
