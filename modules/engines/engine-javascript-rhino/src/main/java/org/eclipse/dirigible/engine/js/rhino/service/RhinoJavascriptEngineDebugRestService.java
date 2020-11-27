/**
 * Copyright (c) 2010-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
package org.eclipse.dirigible.engine.js.rhino.service;

import java.util.List;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.dirigible.api.v3.security.UserFacade;
import org.eclipse.dirigible.commons.api.service.AbstractRestService;
import org.eclipse.dirigible.commons.api.service.IRestService;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.engine.js.debug.model.BreakpointsMetadata;
import org.eclipse.dirigible.engine.js.debug.model.DebugManager;
import org.eclipse.dirigible.engine.js.debug.model.DebugModel;
import org.eclipse.dirigible.engine.js.debug.model.DebugModelFacade;
import org.eclipse.dirigible.engine.js.debug.model.DebugSessionMetadata;
import org.eclipse.dirigible.engine.js.debug.model.DebugSessionModel;
import org.eclipse.dirigible.engine.js.debug.model.LinebreakMetadata;
import org.eclipse.dirigible.engine.js.debug.model.VariableValuesMetadata;
import org.eclipse.dirigible.engine.js.rhino.debugger.RhinoJavascriptDebugController;
import org.eclipse.dirigible.engine.js.rhino.debugger.RhinoJavascriptDebugProcessor;
import org.eclipse.dirigible.engine.js.rhino.processor.RhinoJavascriptEngineExecutor;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * Front facing REST service serving the Mozilla Rhino based Javascript backend services.
 */
@Singleton
@Path("/ide/debug/rhino")
@Api(value = "JavaScript Engine Debugger - Rhino", authorizations = { @Authorization(value = "basicAuth", scopes = {}) })
@ApiResponses({ @ApiResponse(code = 401, message = "Unauthorized"), @ApiResponse(code = 403, message = "Forbidden"),
		@ApiResponse(code = 404, message = "Not Found"), @ApiResponse(code = 500, message = "Internal Server Error") })
public class RhinoJavascriptEngineDebugRestService extends AbstractRestService implements IRestService {

	private static final Logger logger = LoggerFactory.getLogger(RhinoJavascriptEngineDebugRestService.class);

	@Context
	private HttpServletResponse response;

	/**
	 * Get active sessions list.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/sessions")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Returns the active debug sessions for the current user")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response getSessions() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				List<DebugSessionMetadata> sessions = debugModel.getSessionsMetadata();
				return Response.ok().entity(sessions).build();
			}
			return Response.ok().entity("[]").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}

	private DebugModel retrieveDebugModel(String user) {
		DebugModel debugModel = DebugModelFacade.getDebugModel(user);
		if (debugModel == null) {
			logger.debug("creating DebugModel ...");
			debugModel = DebugManager.getDebugModel(user);
			if (debugModel == null) {
				debugModel = DebugModelFacade.createDebugModel(user, new RhinoJavascriptDebugController(user));
			}
		}
		return debugModel;
	}
	
	/**
	 * Set default session for user.
	 *
	 * @param executionId
	 *            the executionId
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/activate/{executionId}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Set default session for the current user")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response activateSession(
			@ApiParam(value = "executionId", required = true) @PathParam("executionId") String executionId) {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				DebugSessionModel sessionModel = debugModel.getSessionByExecutionId(executionId);
				if (sessionModel == null) {
					throw new IllegalArgumentException("Debug session not known: " + executionId);
				}
				debugModel.setActiveSession(sessionModel);
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Step Into for the default active session.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/stepInto")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Step Into command for the current user's default session")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response stepInto() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugExecutor().stepInto();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Step Over for the default active session.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/stepOver")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Step Over command for the current user's default session")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response stepOver() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugExecutor().stepOver();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Continue for the default active session.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/continue")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Continue command for the current user's default session")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response continueExecution() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugExecutor().continueExecution();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Pause for the default active session.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/pause")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Pause command for the current user's default session")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response pauseExecution() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugExecutor().pauseExecution();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Resume for the default active session.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/resume")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Resume command for the current user's default session")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response resumeExecution() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugExecutor().resumeExecution();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Variable values list.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/variables")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("List the variable values")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response listVariableValues() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				VariableValuesMetadata variableValuesMetadata = debugModel.getActiveSession().getVariableValuesMetadata();
				return Response.ok().entity(variableValuesMetadata).build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Current line break.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/session/currentLine")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("List the variable values")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response getCurrentLineBreak() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				LinebreakMetadata linebreakMetadata = debugModel.getActiveSession().getCurrentLineBreak();
				return Response.ok().entity(linebreakMetadata).build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	
	
	
	/**
	 * Set a breakpoint.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/breakpoint/set/{row}/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Set a breakpoint")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response setBreakpoint(
			@ApiParam(value = "path", required = true) @PathParam("path") String path,
			@ApiParam(value = "row", required = true) @PathParam("row") int row) {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getDebugController().setBreakpoint(IRepositoryStructure.SEPARATOR + path, row);
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Remove a breakpoint.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/breakpoint/remove/{row}/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Remove a breakpoint")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response removeBreakpoint(
			@ApiParam(value = "path", required = true) @PathParam("path") String path,
			@ApiParam(value = "row", required = true) @PathParam("row") int row) {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getDebugController().removeBreakpoint(IRepositoryStructure.SEPARATOR + path, row);
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Remove all breakpoints.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/breakpoint/removeAll")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Remove a breakpoint")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response removeAllBreakpoints() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				debugModel.getActiveSession().getDebugController().removeAllBreakpoints();
				return Response.ok().build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Breakpoints list.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/breakpoints")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("List the breakpoints")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response listBreakpoints() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			DebugModel debugModel = retrieveDebugModel(user);
			if (debugModel != null && debugModel.getActiveSession() != null) {
				BreakpointsMetadata breakpointsMetadata = debugModel.getBreakpointsMetadata();
				return Response.ok().entity(breakpointsMetadata).build();
			}
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("No debug model present").build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Enable debugging.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/enable")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Enable debugging")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response enable() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			Configuration.set(RhinoJavascriptEngineExecutor.DIRIGBLE_JAVASCRIPT_RHINO_DEBUGGER_ENABLED, "true");
			return Response.ok().build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}
	
	/**
	 * Disable debugging.
	 *
	 * @return result of the execution of the service
	 */
	@GET
	@Path("/disable")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Disable debugging")
	@ApiResponses({ @ApiResponse(code = 200, message = "Execution Result") })
	public Response disable() {
		String user = UserFacade.getName();
		if (user == null) {
			return createErrorResponseForbidden(NO_LOGGED_IN_USER);
		}
		try {
			Configuration.set(RhinoJavascriptEngineExecutor.DIRIGBLE_JAVASCRIPT_RHINO_DEBUGGER_ENABLED, "false");
			RhinoJavascriptDebugProcessor.closeAll();
			return Response.ok().build();
		} catch (Throwable e) {
			String message = e.getMessage();
			logger.error(message, e);
			return createErrorResponseInternalServerError(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.IRestService#getType()
	 */
	@Override
	public Class<? extends IRestService> getType() {
		return RhinoJavascriptEngineDebugRestService.class;
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
