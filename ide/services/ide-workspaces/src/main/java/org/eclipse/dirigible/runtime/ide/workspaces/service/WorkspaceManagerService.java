/*
 * Copyright (c) 2010-2020 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2020 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.runtime.ide.workspaces.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.api.v3.security.UserFacade;
import org.eclipse.dirigible.api.v3.utils.UrlFacade;
import org.eclipse.dirigible.commons.api.service.AbstractRestService;
import org.eclipse.dirigible.commons.api.service.IRestService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.eclipse.dirigible.runtime.ide.workspaces.processor.WorkspaceProcessor;
import org.eclipse.dirigible.runtime.ide.workspaces.processor.WorkspaceSourceTargetPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * Front facing RPC service serving the Workspace actions.
 */
@Singleton
@Path("/ide/workspace")
@RolesAllowed({ "Developer" })
@Api(value = "IDE - Workspace Manager", authorizations = { @Authorization(value = "basicAuth", scopes = {}) })
@ApiResponses({ @ApiResponse(code = 401, message = "Unauthorized"), @ApiResponse(code = 403, message = "Forbidden"),
		@ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 500, message = "Internal Server Error") })
public class WorkspaceManagerService extends AbstractRestService implements IRestService {

	private static final String ERROR_PATH_DOES_NOT_EXISTS = "Path does not exists.";
	
	private static final String ERROR_INVALID_PROJECT_NAME = "Invalid project name";

	private static final String ERROR_TARGET_PATH_POINTS_TO_A_NON_EXISTING_FOLDER = "Target path points to a non-existing folder";

	private static final String ERROR_TARGET_PATH_IS_EMPTY = "Target path is empty";

	private static final String ERROR_SOURCE_PATH_IS_EMPTY = "Source path is empty";

	private static final String ERROR_SOURCE_AND_TARGET_PATHS_HAVE_TO_BE_PRESENT_IN_THE_BODY_OF_THE_REQUEST = "Source and Target paths have to be present in the body of the request";

	private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);

	@Inject
	private WorkspaceProcessor processor;

	@Context
	private HttpServletResponse response;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.IRestService#getType()
	 */
	@Override
	public Class<? extends IRestService> getType() {
		return WorkspaceManagerService.class;
	}

	/**
	 * Copy.
	 *
	 * @param workspace
	 *            the workspace
	 * @param content
	 *            the content
	 * @param request
	 *            the request
	 * @return the response
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws DecoderException
	 *             the decoder exception
	 */
	@POST
	@Path("{workspace}/copy")
	public Response copy(@PathParam("workspace") String workspace, WorkspaceSourceTargetPair content, @Context HttpServletRequest request)
			throws URISyntaxException, UnsupportedEncodingException, DecoderException {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}

		if ((content.getSource() == null) || (content.getTarget() == null)) {
			return createErrorResponseBadRequest(ERROR_SOURCE_AND_TARGET_PATHS_HAVE_TO_BE_PRESENT_IN_THE_BODY_OF_THE_REQUEST);
		}

		RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(content.getSource()));
		if (sourcePath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_SOURCE_PATH_IS_EMPTY);
		}

		RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
		if (targetPath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_TARGET_PATH_IS_EMPTY);
		}

		String sourceProject = sourcePath.getSegments()[0];
		String targetProject = targetPath.getSegments()[0];
		if (sourcePath.getSegments().length == 1) {
			// a project is selected as a source
			processor.copyProject(workspace, sourceProject, targetProject);
			return Response.created(processor.getURI(workspace, targetProject, null)).build();
		}

		String targetFilePath = targetPath.constructPathFrom(1);
		if (targetFilePath.equals(targetPath.build())) {
			targetFilePath = IRepository.SEPARATOR;
		}
		if (!processor.existsFolder(workspace, targetProject, targetFilePath)) {
			return createErrorResponseBadRequest(ERROR_TARGET_PATH_POINTS_TO_A_NON_EXISTING_FOLDER);
		}

		String sourceFilePath = sourcePath.constructPathFrom(1);
		if (processor.existsFile(workspace, sourceProject, sourceFilePath)) {
			processor.copyFile(workspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
		} else {
			processor.copyFolder(workspace, sourceProject, sourceFilePath, targetProject,
					targetFilePath + IRepositoryStructure.SEPARATOR + sourcePath.getLastSegment());
		}

		return Response.created(processor.getURI(workspace, null, content.getTarget())).build();
	}

	/**
	 * Move.
	 *
	 * @param workspace
	 *            the workspace
	 * @param content
	 *            the content
	 * @param request
	 *            the request
	 * @return the response
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws DecoderException
	 *             the decoder exception
	 */
	@POST
	@Path("{workspace}/move")
	public Response move(@PathParam("workspace") String workspace, WorkspaceSourceTargetPair content, @Context HttpServletRequest request)
			throws URISyntaxException, UnsupportedEncodingException, DecoderException {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}

		if ((content.getSource() == null) || (content.getTarget() == null)) {
			return createErrorResponseBadRequest(ERROR_SOURCE_AND_TARGET_PATHS_HAVE_TO_BE_PRESENT_IN_THE_BODY_OF_THE_REQUEST);
		}

		RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(content.getSource()));
		if (sourcePath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_SOURCE_PATH_IS_EMPTY);
		}

		RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
		if (targetPath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_TARGET_PATH_IS_EMPTY);
		}

		String sourceProject = sourcePath.getSegments()[0];
		String targetProject = targetPath.getSegments()[0];
		if (sourcePath.getSegments().length == 1) {
			// a project is selected as a source
			processor.moveProject(workspace, sourceProject, targetProject);
			return Response.created(processor.getURI(workspace, targetProject, null)).build();
		}

		String sourceFilePath = sourcePath.constructPathFrom(1);
		String targetFilePath = targetPath.constructPathFrom(1);
		if (processor.existsFile(workspace, sourceProject, sourceFilePath)) {
			processor.moveFile(workspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
		} else if (processor.existsFolder(workspace, sourceProject, sourceFilePath)) {
			processor.moveFolder(workspace, sourceProject, sourceFilePath, targetProject, targetFilePath);
		} else {
			return createErrorResponseNotFound(ERROR_PATH_DOES_NOT_EXISTS);
		}

		return Response.created(processor.getURI(workspace, null, content.getTarget())).build();
	}

	/**
	 * Rename.
	 *
	 * @param workspace
	 *            the workspace
	 * @param content
	 *            the content
	 * @param request
	 *            the request
	 * @return the response
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 * @throws DecoderException
	 *             the decoder exception
	 */
	@POST
	@Path("{workspace}/rename")
	public Response rename(@PathParam("workspace") String workspace, WorkspaceSourceTargetPair content, @Context HttpServletRequest request)
			throws URISyntaxException, UnsupportedEncodingException, DecoderException {
		return move(workspace, content, request);
	}
	
	/**
	 * Link project.
	 *
	 * @param workspace
	 *            the workspace
	 * @param content
	 *            the content
	 * @param request
	 *            the request
	 * @return the response
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws DecoderException
	 *             the decoder exception
	 * @throws IOException
	 *             IO error
	 */
	@POST
	@Path("{workspace}/linkProject")
	public Response link(@PathParam("workspace") String workspace, WorkspaceSourceTargetPair content, @Context HttpServletRequest request)
			throws URISyntaxException, DecoderException, IOException {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}

		if ((content.getSource() == null) || (content.getTarget() == null)) {
			return createErrorResponseBadRequest(ERROR_SOURCE_AND_TARGET_PATHS_HAVE_TO_BE_PRESENT_IN_THE_BODY_OF_THE_REQUEST);
		}

		RepositoryPath sourcePath = new RepositoryPath(UrlFacade.decode(content.getSource()));
		if (sourcePath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_SOURCE_PATH_IS_EMPTY);
		}

		RepositoryPath targetPath = new RepositoryPath(UrlFacade.decode(content.getTarget()));
		if (targetPath.getSegments().length == 0) {
			return createErrorResponseBadRequest(ERROR_TARGET_PATH_IS_EMPTY);
		}

		String sourceProject = sourcePath.getSegments()[0];
		String targetProject = targetPath.getPath();
		if (sourcePath.getSegments().length == 1) {
			// a project is selected as a source
			processor.linkProject(workspace, sourceProject, targetProject);
			return Response.created(processor.getURI(workspace, sourceProject, null)).build();
		}
		return createErrorResponseBadRequest(ERROR_INVALID_PROJECT_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.AbstractRestService#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return logger;
	}

}
