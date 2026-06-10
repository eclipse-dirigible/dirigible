/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.io;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.dirigible.components.api.io.ImageFacade;

/**
 * Image resize helper. Reads from any {@link InputStream} (file, HTTP body, in-memory buffer),
 * scales the image to the requested dimensions, and returns a fresh stream you can pipe into the
 * next sink. {@code type} is the output format ({@code "png"}, {@code "jpg"}, ...) — the platform
 * uses {@link javax.imageio.ImageIO} under the hood, so the supported list matches whatever codecs
 * are on the JVM classpath.
 * <p>
 * For more advanced transformations (crop, rotate, compose) drop down to {@code ImageIO} or a
 * dedicated library and bring the bytes back through {@link Streams}.
 */
public final class Image {

    private Image() {}

    public static InputStream resize(InputStream original, String type, int width, int height) throws IOException {
        return ImageFacade.resize(original, type, width, height);
    }
}
