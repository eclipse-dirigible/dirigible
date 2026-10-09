/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class ConfigurationsFacadeTest {

    @AfterEach
    void cleanUp() {
        Configuration.remove("DIRIGIBLE_MAIL_PASSWORD");
    }

    @Test
    void groupsAreJson() {
        JsonArray groups = JsonParser.parseString(ConfigurationsFacade.getGroups())
                                     .getAsJsonArray();
        assertEquals("instance", groups.get(0)
                                       .getAsJsonObject()
                                       .get("id")
                                       .getAsString());
    }

    @Test
    void descriptorsFilterByGroupAndMaskSecrets() {
        Configuration.set("DIRIGIBLE_MAIL_PASSWORD", "hunter2");
        String json = ConfigurationsFacade.getDescriptors("mail,branding");
        assertFalse(json.contains("hunter2"));
        JsonArray groups = JsonParser.parseString(json)
                                     .getAsJsonArray();
        assertEquals(2, groups.size());
        JsonObject mail = groups.get(1)
                                .getAsJsonObject();
        assertEquals("mail", mail.get("group")
                                 .getAsString());
        assertTrue(mail.get("entries")
                       .getAsJsonArray()
                       .size() > 0);
        assertTrue(JsonParser.parseString(ConfigurationsFacade.getDescriptors(null))
                             .getAsJsonArray()
                             .size() > 2);
    }

}
