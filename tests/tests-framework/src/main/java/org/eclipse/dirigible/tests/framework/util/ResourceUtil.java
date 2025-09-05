package org.eclipse.dirigible.tests.framework.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static String loadResource(String resourcePath) {
        try (InputStream inputStream = ResourceUtil.class.getResourceAsStream(resourcePath)) {
            if (null == inputStream) {
                throw new IllegalStateException("Missing resource with path " + resourcePath);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resource with path " + resourcePath, ex);
        }
    }

}
