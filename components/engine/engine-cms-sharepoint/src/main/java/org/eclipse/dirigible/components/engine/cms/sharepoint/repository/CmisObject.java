package org.eclipse.dirigible.components.engine.cms.sharepoint.repository;

import java.time.OffsetDateTime;

public record CmisObject(String id, String name, String path, boolean isFolder, long size, String contentType,
        OffsetDateTime lastModified) {
}
