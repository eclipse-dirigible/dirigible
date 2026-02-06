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
