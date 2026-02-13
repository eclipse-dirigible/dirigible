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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PlatformAssetsJsonLoader {

    public static Map<String, List<PlatformAsset>> loadAssetsFromJson() {

        try (InputStream is = PlatformAssetsJsonLoader.class.getResourceAsStream("/platform-links.json")) {

            if (is == null) {
                throw new IllegalStateException("platform-links.json not found on classpath");
            }

            ObjectMapper mapper = new ObjectMapper();

            Map<String, List<PlatformAssetJson>> raw = mapper.readValue(is, new TypeReference<Map<String, List<PlatformAssetJson>>>() {});

            Map<String, List<PlatformAsset>> assets = new HashMap<>();

            for (Map.Entry<String, List<PlatformAssetJson>> entry : raw.entrySet()) {
                List<PlatformAsset> converted = new ArrayList<>();

                for (PlatformAssetJson r : entry.getValue()) {
                    PlatformAsset.Type type = PlatformAsset.Type.valueOf(r.type);
                    boolean module = Boolean.TRUE.equals(r.module);
                    boolean defer = Boolean.TRUE.equals(r.defer);
                    converted.add(new PlatformAsset(type, r.path, module, defer));
                }

                assets.put(entry.getKey(), converted);
            }

            return assets;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load platform-links.json", e);
        }
    }

}
