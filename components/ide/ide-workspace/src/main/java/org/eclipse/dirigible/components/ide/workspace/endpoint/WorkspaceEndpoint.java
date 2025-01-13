/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.endpoint;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.InputMismatchException;
import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.components.api.utils.UrlFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.workspace.domain.WorkspaceFromToPair;
import org.eclipse.dirigible.components.ide.workspace.domain.WorkspaceSelectionTargetPair;
import org.eclipse.dirigible.components.ide.workspace.domain.WorkspaceSelectionTargetPair.SelectedNode;
import org.eclipse.dirigible.components.ide.workspace.domain.WorkspaceSourceTargetPair;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

/**
 * The Class WorkspaceEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "workspace")
public class WorkspaceEndpoint {

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The publisher service. */
    @Autowired
    private PublisherService publisherService;

    /**
     * Copy.
     *
     * @param currentWorkspace the current workspace
     * @param content the content
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws DecoderException the decoder exception
     */
    @PostMapping("{workspace}/copy")
    public ResponseEntity<URI> copy(@PathVariable("workspace") String currentWorkspace,
            @Valid @RequestBody WorkspaceSourceTargetPair content)
            throws URISyntaxException, UnsupportedEncodingException, DecoderException {
        if ((content.getSources() == null) || (content.getTarget() == null) || (content.getSourceWorkspace() == null)
                || (content.getTargetWorkspace() == null)) {
            String error = "Sources and Target paths and workspaces have to be present in the body of the request";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
        if (targetPath.getSegments().length == 0) {
            String error = "Target path is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        String sourceWorkspace = content.getSourceWorkspace();
        if (sourceWorkspace.length() == 0) {
            String error = "Source workspace is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        String targetWorkspace = content.getTargetWorkspace();
        if (targetWorkspace.length() == 0) {
            String error = "Target workspace is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        for (String source : content.getSources()) {
            RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(source));
            if (sourcePath.getSegments().length == 0) {
                String error = "Source path is empty";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
            }

            String sourceProject = sourcePath.getSegments()[0];
            String targetProject = targetPath.getSegments()[0];
            if (sourcePath.getSegments().length == 1) {
                // a project is selected as a source
                workspaceService.copyProject(sourceWorkspace, targetWorkspace, sourceProject, targetProject);
                return ResponseEntity.created(workspaceService.getURI(targetWorkspace, targetProject, null))
                                     .build();
            }

            String targetFilePath = targetPath.constructPathFrom(1);
            if (targetFilePath.equals(targetPath.build())) {
                targetFilePath = IRepository.SEPARATOR;
            }
            if (!workspaceService.existsFolder(targetWorkspace, targetProject, targetFilePath)) {
                String error = "Target path points to a non-existing folder";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
            }

            String sourceFilePath = sourcePath.constructPathFrom(1);
            if (workspaceService.existsFile(sourceWorkspace, sourceProject, sourceFilePath)) {
                workspaceService.copyFile(sourceWorkspace, targetWorkspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
            } else {
                workspaceService.copyFolder(sourceWorkspace, targetWorkspace, sourceProject, sourceFilePath, targetProject,
                        targetFilePath + IRepositoryStructure.SEPARATOR, sourcePath.getLastSegment());
            }
        }

        return ResponseEntity.created(workspaceService.getURI(targetWorkspace, null, content.getTarget()))
                             .build();
    }

    /**
     * Copy selection of nodes.
     *
     * @param currentWorkspace the current workspace
     * @param content the content
     * @return the response
     * @throws Exception the exception
     */
    @PostMapping("{workspace}/copySelection")
    public ResponseEntity<URI> copySelection(@PathVariable("workspace") String currentWorkspace,
            @Valid @RequestBody WorkspaceSelectionTargetPair content) throws Exception {
        ArrayList<WorkspaceSelectionTargetPair.SelectedNode> sourceSelection = content.getSource();

        if ((content.getSource() == null) || (content.getTarget() == null) || (content.getSourceWorkspace() == null)
                || (content.getTargetWorkspace() == null)) {
            String error = "Source and Target paths and workspaces have to be present in the body of the request";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        if (sourceSelection.size() == 0) {
            String error = "Source path is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
        if (targetPath.getSegments().length == 0) {
            String error = "Target path is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        String sourceWorkspace = content.getSourceWorkspace();
        if (sourceWorkspace.length() == 0) {
            String error = "Source workspace is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        String targetWorkspace = content.getTargetWorkspace();
        if (targetWorkspace.length() == 0) {
            String error = "Target workspace is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        String targetProject = targetPath.getSegments()[1];
        WorkspaceSelectionTargetPair.SelectedNode nodeToCopy;

        for (SelectedNode element : sourceSelection) {

            nodeToCopy = element;
            RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(element.getPath()));
            String sourceProject = sourcePath.getSegments()[1];

            if (sourcePath.getSegments().length == 1) {
                // a project is selected as a source
                workspaceService.copyProject(sourceWorkspace, targetWorkspace, sourceProject, targetProject);
                return ResponseEntity.created(workspaceService.getURI(targetWorkspace, targetProject, null))
                                     .build();
            }

            String targetFilePath = targetPath.constructPathFrom(2);
            String relativePath = element.getRelativePath();
            if (targetFilePath.equals(targetPath.build())) {
                targetFilePath = IRepository.SEPARATOR;
            }
            targetFilePath = targetFilePath.concat(IRepository.SEPARATOR)
                                           .concat(nodeToCopy.getInternalPath())
                                           .replaceAll("^/+", "");

            String fileOrFolder = element.getNodeType();
            String conflictResolution = element.getResolution();
            String relativePathToTargetFile = Paths.get(targetFilePath)
                                                   .getParent()
                                                   .toString();
            String fileOrFolderName = Paths.get(targetFilePath)
                                           .getFileName()
                                           .toString();
            String skipPath;

            switch (fileOrFolder) {
                case "folder":
                    if (workspaceService.existsFile(targetWorkspace, targetProject, targetFilePath)) {
                        switch (conflictResolution) {
                            case "replace":
                                workspaceService.deleteFile(targetWorkspace, targetProject, targetFilePath);
                                workspaceService.createFolder(targetWorkspace, targetProject, targetFilePath);
                                break;
                            case "skip":
                                skipPath = element.getPath()
                                                  .concat(IRepository.SEPARATOR);
                                content.skipByPath(skipPath);
                                break;
                            default:
                                workspaceService.copyFolder(sourceWorkspace, targetWorkspace, sourceProject, relativePath, targetProject,
                                        relativePathToTargetFile.concat(IRepository.SEPARATOR), fileOrFolderName);
                                skipPath = element.getPath()
                                                  .concat(IRepository.SEPARATOR);
                                content.skipByPath(skipPath);

                        }
                    } else if (!workspaceService.existsFolder(targetWorkspace, targetProject, targetFilePath)) {
                        workspaceService.createFolder(targetWorkspace, targetProject, targetFilePath);
                    }
                    break;
                case "file":
                    if (workspaceService.existsFile(sourceWorkspace, sourceProject, relativePath)) {
                        switch (conflictResolution) {
                            case "replace":
                                if (workspaceService.existsFile(targetWorkspace, targetProject, targetFilePath))
                                    workspaceService.deleteFile(targetWorkspace, targetProject, targetFilePath);
                                else if (workspaceService.existsFolder(targetWorkspace, targetProject, targetFilePath))
                                    workspaceService.deleteFolder(targetWorkspace, targetProject, targetFilePath);
                                workspaceService.copyFile(sourceWorkspace, targetWorkspace, sourceProject, relativePath, targetProject,
                                        relativePathToTargetFile);
                                break;
                            case "skip":
                                break;
                            default:
                                workspaceService.copyFile(sourceWorkspace, targetWorkspace, sourceProject, relativePath, targetProject,
                                        relativePathToTargetFile);

                        }
                    }
                    break;
                default:
                    throw new InputMismatchException();
            }
        }
        return ResponseEntity.created(workspaceService.getURI(targetWorkspace, null, content.getTarget()))
                             .build();
    }

    /**
     * Move.
     *
     * @param workspace the workspace
     * @param content the content
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws DecoderException the decoder exception
     */
    @PostMapping(value = "{workspace}/move", produces = "application/json")
    public ResponseEntity<WorkspaceFromToPair[]> move(@PathVariable("workspace") String workspace,
            @Valid @RequestBody WorkspaceSourceTargetPair content)
            throws URISyntaxException, UnsupportedEncodingException, DecoderException {
        if ((content.getSources() == null) || (content.getTarget() == null) || (content.getSourceWorkspace() == null)
                || (content.getTargetWorkspace() == null)) {
            String error = "Sources and Target paths and workspaces have to be present in the body of the request";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
        if (targetPath.getSegments().length == 0) {
            String error = "Target path is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        ArrayList<WorkspaceFromToPair> result = new ArrayList<WorkspaceFromToPair>();

        for (String source : content.getSources()) {
            RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(source));
            if (sourcePath.getSegments().length == 0) {
                String error = "Source path is empty";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
            }

            String sourceProject = sourcePath.getSegments()[0];
            String targetProject = targetPath.getSegments()[0];
            if (sourcePath.getSegments().length == 1) {
                // a project is selected as a source
                workspaceService.moveProject(workspace, sourceProject, targetProject);
                result.add(new WorkspaceFromToPair(sourceProject, targetProject));
                return ResponseEntity.ok(result.toArray(new WorkspaceFromToPair[] {}));
            }

            String sourceFilePath = sourcePath.constructPathFrom(1);
            String targetFilePath = targetPath.constructPathFrom(1);
            boolean multiple = content.getSources().length > 1;
            if (workspaceService.existsFile(workspace, sourceProject, sourceFilePath)) {
                moveFile(workspace, result, sourceProject, targetProject, sourceFilePath, targetFilePath, multiple);
            } else if (workspaceService.existsFolder(workspace, sourceProject, sourceFilePath)) {
                moveFolder(workspace, result, sourceProject, targetProject, sourceFilePath, targetFilePath, multiple);
            } else {
                String error = "Path does not exists.";
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
            }
            publisherService.unpublish(sourcePath.getPath());
            publisherService.publish(workspace, targetPath.getPath(), "");
        }
        return ResponseEntity.ok(result.toArray(new WorkspaceFromToPair[] {}));
    }

    /**
     * Move folder.
     *
     * @param workspace the workspace
     * @param result the result
     * @param sourceProject the source project
     * @param targetProject the target project
     * @param sourceFilePath the source file path
     * @param targetFilePath the target file path
     * @param multiple the multiple
     */
    private void moveFolder(String workspace, ArrayList<WorkspaceFromToPair> result, String sourceProject, String targetProject,
            String sourceFilePath, String targetFilePath, boolean multiple) {
        if (sourceFilePath.toLowerCase()
                          .equals(targetFilePath.toLowerCase())
                && sourceProject.toLowerCase()
                                .equals(targetProject.toLowerCase())) {
            if (multiple) {
                // multiple source files, so the target must be a folder or a project
                if (workspaceService.existsFolder(workspace, targetProject, targetFilePath)) {
                    String targetCombinedFilePath =
                            targetFilePath + sourceFilePath.substring(sourceFilePath.lastIndexOf(IRepositoryStructure.SEPARATOR));
                    String casesensitivefix = targetCombinedFilePath + "_temp";
                    workspaceService.moveFolder(workspace, sourceProject, sourceFilePath, targetProject, casesensitivefix);
                    workspaceService.moveFolder(workspace, sourceProject, casesensitivefix, targetProject, targetFilePath);
                    result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetCombinedFilePath));
                } else {
                    String error = "Target path is not a folder or a project";
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
                }
            } else {
                String casesensitivefix = sourceFilePath + "_temp";
                workspaceService.moveFolder(workspace, sourceProject, sourceFilePath, targetProject, casesensitivefix);
                workspaceService.moveFolder(workspace, sourceProject, casesensitivefix, targetProject, targetFilePath);
                result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetFilePath));
            }
        } else {
            if (multiple) {
                // multiple source files, so the target must be a folder or a project
                if (workspaceService.existsFolder(workspace, targetProject, targetFilePath)) {
                    String targetCombinedFilePath =
                            targetFilePath + sourceFilePath.substring(sourceFilePath.lastIndexOf(IRepositoryStructure.SEPARATOR));
                    workspaceService.moveFolder(workspace, sourceProject, sourceFilePath, targetProject, targetCombinedFilePath);
                    result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetCombinedFilePath));
                } else {
                    String error = "Target path is not a folder or a project";
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
                }
            } else {
                workspaceService.moveFolder(workspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
                result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetFilePath));
            }
        }
    }

    /**
     * Move file.
     *
     * @param workspace the workspace
     * @param result the result
     * @param sourceProject the source project
     * @param targetProject the target project
     * @param sourceFilePath the source file path
     * @param targetFilePath the target file path
     * @param multiple the multiple
     */
    private void moveFile(String workspace, ArrayList<WorkspaceFromToPair> result, String sourceProject, String targetProject,
            String sourceFilePath, String targetFilePath, boolean multiple) {
        if (sourceFilePath.toLowerCase()
                          .equals(targetFilePath.toLowerCase())
                && sourceProject.toLowerCase()
                                .equals(targetProject.toLowerCase())) {
            if (multiple) {
                // multiple source files, so the target must be a folder or a project
                if (workspaceService.existsFolder(workspace, targetProject, targetFilePath)) {
                    String targetCombinedFilePath =
                            targetFilePath + sourceFilePath.substring(sourceFilePath.lastIndexOf(IRepositoryStructure.SEPARATOR));
                    String casesensitivefix = targetCombinedFilePath + "_temp";
                    workspaceService.moveFile(workspace, sourceProject, sourceFilePath, targetProject, casesensitivefix);
                    workspaceService.moveFile(workspace, sourceProject, casesensitivefix, targetProject, targetFilePath);
                    result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetCombinedFilePath));
                } else {
                    String error = "Target path is not a folder or a project";
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
                }
            } else {
                String casesensitivefix = sourceFilePath + "_temp";
                workspaceService.moveFile(workspace, sourceProject, sourceFilePath, targetProject, casesensitivefix);
                workspaceService.moveFile(workspace, sourceProject, casesensitivefix, targetProject, targetFilePath);
                result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetFilePath));
            }
        } else {
            if (multiple) {
                // multiple source files, so the target must be a folder or a project
                if (workspaceService.existsFolder(workspace, targetProject, targetFilePath)) {
                    String targetCombinedFilePath =
                            targetFilePath + sourceFilePath.substring(sourceFilePath.lastIndexOf(IRepositoryStructure.SEPARATOR));
                    workspaceService.moveFile(workspace, sourceProject, sourceFilePath, targetProject, targetCombinedFilePath);
                    result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetCombinedFilePath));
                } else {
                    String error = "Target path is not a folder or a project";
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
                }
            } else {
                workspaceService.moveFile(workspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
                result.add(new WorkspaceFromToPair(sourceProject + sourceFilePath, targetProject + targetFilePath));
            }
        }
    }

    /**
     * Rename.
     *
     * @param workspace the workspace
     * @param content the content
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws DecoderException the decoder exception
     */
    @PostMapping(value = "{workspace}/rename", produces = "application/json")
    public ResponseEntity<WorkspaceFromToPair[]> rename(@PathVariable("workspace") String workspace,
            @Valid @RequestBody WorkspaceSourceTargetPair content)
            throws URISyntaxException, UnsupportedEncodingException, DecoderException {
        if (content.getSources().length > 1) {
            String error = "Rename operation is only for a single file or folder";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        return move(workspace, content);
    }

    /**
     * Link project.
     *
     * @param workspace the workspace
     * @param content the content
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws DecoderException the decoder exception
     * @throws IOException IO error
     */
    @PostMapping("{workspace}/linkProject")
    public ResponseEntity<URI> link(@PathVariable("workspace") String workspace, @Valid @RequestBody WorkspaceSourceTargetPair content)
            throws URISyntaxException, DecoderException, IOException {
        if ((content.getSources() == null) || (content.getTarget() == null) || (content.getSourceWorkspace() == null)
                || (content.getTargetWorkspace() == null)) {
            String error = "Source and Target paths and workspaces have to be present in the body of the request";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
        if (targetPath.getSegments().length == 0) {
            String error = "Target path is empty";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        for (String source : content.getSources()) {

            RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(source));
            if (sourcePath.getSegments().length == 0) {
                String error = "Source path is empty";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
            }

            String sourceProject = sourcePath.getSegments()[0];
            String targetProject = targetPath.getPath();
            if (sourcePath.getSegments().length == 1) {
                // a project is selected as a source
                workspaceService.linkProject(workspace, sourceProject, targetProject);
                return ResponseEntity.created(workspaceService.getURI(workspace, sourceProject, null))
                                     .build();
            }
        }
        String error = "Invalid project name";
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }

}
