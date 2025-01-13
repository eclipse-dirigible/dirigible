/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.componenets.api.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.api.io.ImageFacade;
import org.junit.jupiter.api.Test;

public class ImageFacadeTest {

    @Test
    public void resizeTest() throws IOException {
        InputStream original = ImageFacadeTest.class.getResourceAsStream("/dirigible.png");
        InputStream result = ImageFacade.resize(original, "png", 300, 155);
        FileOutputStream out = new FileOutputStream("./target/dirigible_output.png");
        IOUtils.copy(result, out);
    }

}
