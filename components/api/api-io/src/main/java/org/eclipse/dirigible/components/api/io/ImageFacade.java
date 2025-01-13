/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Facade for working with images.
 */
@Component
public class ImageFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(FilesFacade.class);

    /**
     * Resize an image to the given boundaries.
     *
     * @param original original image
     * @param type type of the image
     * @param width width of the new image
     * @param height height of the new image
     * @return the created input stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final InputStream resize(InputStream original, String type, int width, int height) throws IOException {
        BufferedImage bufferedImage = javax.imageio.ImageIO.read(original);
        Image scaledImage = bufferedImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);

        int bufferedImageType = bufferedImage.getType();
        BufferedImage buffer = new BufferedImage(width, height, bufferedImageType);
        buffer.getGraphics()
              .drawImage(scaledImage, 0, 0, null);

        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(buffer, type, temp);

        temp.flush();
        return temp.toInputStream();
    }

}
