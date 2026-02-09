/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.watcher;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PlatformAssetsJsonLoader {

    public static List<PlatformAsset> loadAssetsFromJson() {

        try (InputStream is = PlatformAssetsJsonLoader.class.getResourceAsStream("/platform-links.json")) {

            if (is == null) {
                throw new IllegalStateException("platform-links.json not found on classpath");
            }

            ObjectMapper mapper = new ObjectMapper();

            List<PlatformAssetJson> raw = mapper.readValue(is, new TypeReference<List<PlatformAssetJson>>() {});

            List<PlatformAsset> assets = new ArrayList<>();

            for (PlatformAssetJson r : raw) {

                PlatformAsset.Type type = PlatformAsset.Type.valueOf(r.type);

                boolean module = Boolean.TRUE.equals(r.module);
                boolean defer = Boolean.TRUE.equals(r.defer);

                assets.add(new PlatformAsset(type, r.path, r.category, module, defer));
            }

            return assets;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load platform-links.json", e);
        }
    }

}
