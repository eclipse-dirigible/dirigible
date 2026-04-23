package org.eclipse.dirigible.components.api.sharepoint;

import com.microsoft.graph.drives.item.items.item.createuploadsession.CreateUploadSessionPostRequestBody;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class SharePointService {

    private static final Logger logger = LoggerFactory.getLogger(SharePointService.class);

    // Simple upload limit: 4 MB (Microsoft Graph recommendation)
    private static final long SIMPLE_UPLOAD_MAX_SIZE = 4 * 1024 * 1024;

    private final GraphServiceClient graphClient;

    private volatile String driveId;

    public SharePointService(GraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    public void uploadDirectory(Path localPath, String remotePath) {
        if (!Files.isDirectory(localPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + localPath);
        }
        logger.debug("Uploading directory {} to SharePoint path {}", localPath, remotePath);

        // Create the root remote folder
        createFolder(remotePath);

        try (Stream<Path> walker = Files.walk(localPath)) {
            walker.forEach(file -> {
                Path relative = localPath.relativize(file);
                String remoteItemPath = remotePath + "/" + relative.toString()
                                                                   .replace("\\", "/");

                if (Files.isDirectory(file)) {
                    createFolder(remoteItemPath);
                } else {
                    uploadFile(file, remoteItemPath);
                }
            });
        } catch (IOException e) {
            throw new SharePointOperationException("Failed to walk directory: " + localPath, e);
        }
    }

    public void createFolder(String remotePath) {
        String[] parts = sanitizePath(remotePath).split("/");
        String currentParentId = "root";

        for (String part : parts) {
            if (part.isBlank())
                continue;
            try {
                DriveItem folder = new DriveItem();
                folder.setName(part);
                folder.setFolder(new Folder());
                HashMap<String, Object> additionalData = new HashMap<>();
                additionalData.put("@microsoft.graph.conflictBehavior", "replace");
                folder.setAdditionalData(additionalData);

                DriveItem created = graphClient.drives()
                                               .byDriveId(getDriveId())
                                               .items()
                                               .byDriveItemId(currentParentId)
                                               .children()
                                               .post(folder);
                if (created != null) {
                    currentParentId = created.getId();
                }
            } catch (ApiException e) {
                throw new SharePointOperationException("Failed to create folder: " + part, e);
            }
        }
    }

    private void uploadFile(Path localFile, String remotePath) {
        try {
            long size = Files.size(localFile);
            String driveId = getDriveId();
            String itemPath = "root:/" + sanitizePath(remotePath) + ":";

            if (size <= SIMPLE_UPLOAD_MAX_SIZE) {
                // Simple upload for small files
                try (InputStream is = Files.newInputStream(localFile)) {
                    graphClient.drives()
                               .byDriveId(driveId)
                               .items()
                               .byDriveItemId(itemPath)
                               .content()
                               .put(is);
                }
                logger.debug("Uploaded (simple): {}", remotePath);
            } else {
                // Upload session for large files
                CreateUploadSessionPostRequestBody body = new CreateUploadSessionPostRequestBody();
                DriveItemUploadableProperties props = new DriveItemUploadableProperties();
                HashMap<String, Object> propsData = new HashMap<>();
                propsData.put("@microsoft.graph.conflictBehavior", "replace");
                props.setAdditionalData(propsData);
                body.setItem(props);

                UploadSession session = graphClient.drives()
                                                   .byDriveId(driveId)
                                                   .items()
                                                   .byDriveItemId(itemPath)
                                                   .createUploadSession()
                                                   .post(body);

                // Use the Graph SDK's LargeFileUploadTask
                try (InputStream is = Files.newInputStream(localFile)) {
                    var uploadTask = new com.microsoft.graph.core.tasks.LargeFileUploadTask<>(graphClient.getRequestAdapter(), session, is,
                            size, DriveItem::createFromDiscriminatorValue);
                    uploadTask.upload(10, null);
                }
                logger.debug("Uploaded (session): {}", remotePath);
            }
        } catch (IOException e) {
            throw new SharePointOperationException("Failed to read local file: " + localFile, e);
        } catch (ApiException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | InterruptedException e) {
            throw new SharePointOperationException("Failed to upload file: " + remotePath, e);
        }
    }

    public void deleteFolder(String nameOrId) {
        logger.debug("Deleting folder: {}", nameOrId);
        deleteItem(nameOrId);
    }

    private void deleteItem(String nameOrId) {
        try {
            if (looksLikeId(nameOrId)) {
                graphClient.drives()
                           .byDriveId(getDriveId())
                           .items()
                           .byDriveItemId(nameOrId)
                           .delete();
            } else {
                graphClient.drives()
                           .byDriveId(getDriveId())
                           .items()
                           .byDriveItemId("root:/" + sanitizePath(nameOrId) + ":")
                           .delete();
            }
        } catch (ApiException e) {
            if (isNotFound(e)) {
                throw new CmisObjectNotFoundException("Object not found: " + nameOrId, e);
            }
            throw new SharePointOperationException("Failed to delete: " + nameOrId, e);
        }
    }

    /**
     * Lazily resolves and caches the default document library drive ID for the configured SharePoint
     * site.
     */
    private String getDriveId() {
        if (driveId == null) {
            synchronized (this) {
                if (driveId == null) {
                    logger.debug("Resolving SharePoint site drive for {}{}", getSiteHostname(), getSitePath());
                    String siteId = getSiteHostname() + ":" + getSitePath() + ":";
                    var site = graphClient.sites()
                                          .bySiteId(siteId)
                                          .get();
                    String returnedSiteId = site.getId();
                    if (site == null || returnedSiteId == null) {
                        throw new SharePointOperationException("Could not resolve SharePoint site: " + siteId + ". Site: " + site
                                + " , returned site id: " + returnedSiteId);
                    }
                    var drive = graphClient.sites()
                                           .bySiteId(returnedSiteId)
                                           .drive()
                                           .get();
                    String driveId = drive.getId();
                    if (drive == null || driveId == null) {
                        throw new SharePointOperationException("Could not resolve default drive for site " + returnedSiteId + ". Drive: "
                                + drive + ", drive id: " + driveId);
                    }
                    logger.debug("Resolved drive ID: {} for site: {}", driveId, returnedSiteId);
                    this.driveId = driveId;
                }
            }
        }
        return driveId;
    }

    private String getSitePath() {
        return DirigibleConfig.MS_SHAREPOINT_SITE_PATH.getMandatoryStringValue();
    }

    private String getSiteHostname() {
        return DirigibleConfig.MS_SHAREPOINT_SITE_HOSTNAME.getMandatoryStringValue();
    }

    private String sanitizePath(String path) {
        if (path == null)
            return "";
        String cleaned = path.replace("\\", "/");
        // Remove leading/trailing slashes
        while (cleaned.startsWith("/"))
            cleaned = cleaned.substring(1);
        while (cleaned.endsWith("/"))
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        return cleaned;
    }

    /**
     * Heuristic: Graph item IDs are long alphanumeric strings. Paths typically contain '/' or '.'
     * characters.
     */
    private boolean looksLikeId(String value) {
        return value != null && !value.contains("/") && !value.contains("\\") && value.length() > 20;
    }

    private boolean isNotFound(ApiException e) {
        return e.getResponseStatusCode() == 404;
    }

    public void deleteFile(String nameOrId) {
        logger.debug("Deleting file: {}", nameOrId);
        deleteItem(nameOrId);
    }

    public byte[] getFileContent(String nameOrId) {
        logger.debug("Getting file content bytes: {}", nameOrId);
        try (InputStream is = getFileInputStream(nameOrId)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new SharePointOperationException("Failed to read file content: " + nameOrId, e);
        }
    }

    public InputStream getFileInputStream(String nameOrId) {
        logger.debug("Getting file input stream: {}", nameOrId);
        try {
            if (looksLikeId(nameOrId)) {
                return graphClient.drives()
                                  .byDriveId(getDriveId())
                                  .items()
                                  .byDriveItemId(nameOrId)
                                  .content()
                                  .get();
            } else {
                return graphClient.drives()
                                  .byDriveId(getDriveId())
                                  .items()
                                  .byDriveItemId("root:/" + sanitizePath(nameOrId) + ":")
                                  .content()
                                  .get();
            }
        } catch (ApiException e) {
            if (isNotFound(e)) {
                throw new CmisObjectNotFoundException("File not found: " + nameOrId, e);
            }
            throw new SharePointOperationException("Failed to get file stream: " + nameOrId, e);
        }
    }

    public void updateFileContent(String nameOrId, InputStream content) {
        logger.debug("Updating file content: {}", nameOrId);
        putContent(nameOrId, content);
    }

    private void putContent(String nameOrId, InputStream content) {
        logger.debug("Putting file: {}", nameOrId);
        try {
            byte[] bytes = content.readAllBytes();
            if (looksLikeId(nameOrId)) {
                graphClient.drives()
                           .byDriveId(getDriveId())
                           .items()
                           .byDriveItemId(nameOrId)
                           .content()
                           .put(new ByteArrayInputStream(bytes));
            } else {
                graphClient.drives()
                           .byDriveId(getDriveId())
                           .items()
                           .byDriveItemId("root:/" + sanitizePath(nameOrId) + ":")
                           .content()
                           .put(new ByteArrayInputStream(bytes));
            }
        } catch (IOException e) {
            throw new SharePointOperationException("Failed to read input stream for: " + nameOrId, e);
        } catch (ApiException e) {
            if (isNotFound(e)) {
                throw new CmisObjectNotFoundException("File not found: " + nameOrId, e);
            }
            throw new SharePointOperationException("Failed to update content: " + nameOrId, e);
        }
    }

    public void updateFileContent(String nameOrId, InputStream content, String contentType) {
        // Microsoft Graph infers content type from the file extension.
        // If you need to set it explicitly, you can update the file metadata afterward.
        logger.debug("Updating file content: {} (contentType={})", nameOrId, contentType);
        putContent(nameOrId, content);
    }

    public List<DriveItem> listObjects(String path) {
        logger.debug("Listing objects at path: {}", path);
        try {
            String driveId = getDriveId();
            DriveItemCollectionResponse response;

            if (path == null || path.isBlank() || "/".equals(path)) {
                response = graphClient.drives()
                                      .byDriveId(driveId)
                                      .items()
                                      .byDriveItemId("root")
                                      .children()
                                      .get();
            } else {
                response = graphClient.drives()
                                      .byDriveId(driveId)
                                      .items()
                                      .byDriveItemId("root:/" + sanitizePath(path) + ":")
                                      .children()
                                      .get();
            }

            List<DriveItem> result = new ArrayList<>();
            if (response != null && response.getValue() != null) {
                result.addAll(response.getValue());
            }

            // TODO:
            // Handle pagination
            // In production, iterate response.getOdataNextLink() for complete results
            return result;
        } catch (ApiException e) {
            if (isNotFound(e)) {
                logger.debug("Missing folder with path: {}. Returning empty collection.", path, e);
                return Collections.emptyList();
            }
            throw new SharePointOperationException("Failed to list objects at: " + path, e);
        }
    }

    public boolean exists(String nameOrId) {
        logger.debug("Checking existence of {}", nameOrId);
        return getById(nameOrId).isPresent();
    }

    public Optional<DriveItem> getById(String nameOrId) {
        try {
            if (looksLikeId(nameOrId)) {
                return Optional.of(graphClient.drives()
                                              .byDriveId(getDriveId())
                                              .items()
                                              .byDriveItemId(nameOrId)
                                              .get());
            } else {
                return Optional.of(graphClient.drives()
                                              .byDriveId(getDriveId())
                                              .items()
                                              .byDriveItemId("root:/" + sanitizePath(nameOrId) + ":")
                                              .get());
            }
        } catch (ApiException e) {
            if (isNotFound(e)) {
                logger.debug("Missing entry with id: {}. Returning empty optional", nameOrId, e);
                return Optional.empty();
            }
            throw new SharePointOperationException("Failed to get: " + nameOrId, e);
        }
    }

    public String getContentType(String nameOrId) {
        logger.debug("Getting content type of [{}]", nameOrId);
        DriveItem item = getItem(nameOrId);
        if (item.getFolder() != null) {
            return "application/vnd.sharepoint.folder";
        }
        if (item.getFile() != null && item.getFile()
                                          .getMimeType() != null) {
            return item.getFile()
                       .getMimeType();
        }
        return "application/octet-stream";
    }

    private DriveItem getItem(String nameOrId) {
        try {
            if (looksLikeId(nameOrId)) {
                return graphClient.drives()
                                  .byDriveId(getDriveId())
                                  .items()
                                  .byDriveItemId(nameOrId)
                                  .get();
            } else {
                return graphClient.drives()
                                  .byDriveId(getDriveId())
                                  .items()
                                  .byDriveItemId("root:/" + sanitizePath(nameOrId) + ":")
                                  .get();
            }
        } catch (ApiException e) {
            if (isNotFound(e)) {
                throw new CmisObjectNotFoundException("Object not found: " + nameOrId, e);
            }
            throw new SharePointOperationException("Failed to get object: " + nameOrId, e);
        }
    }
}
