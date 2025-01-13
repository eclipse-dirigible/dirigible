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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.eclipse.dirigible.components.api.io.FilesFacade;
import org.junit.jupiter.api.Test;

public class FilesFacadeTest {

    @Test
    public void traverseTest() throws IOException {
        String json = FilesFacade.traverse(".");
        System.out.println(json);
        assertTrue(json.contains("FilesFacadeTest.class"));
    }

    @Test
    public void listTest() throws IOException {
        String json = FilesFacade.list(".");
        System.out.println(json);
        assertTrue(json.contains("about.html"));
    }

}
