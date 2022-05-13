/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.runtime.core.services;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.eclipse.dirigible.commons.api.service.AbstractExceptionHandler;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class GeneralExceptionHandler.
 */
@Provider
public class RepositoryNotFoundExceptionHandler extends AbstractExceptionHandler<RepositoryNotFoundException> {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryNotFoundExceptionHandler.class);

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.AbstractExceptionHandler#getType()
	 */
	@Override
	public Class<? extends AbstractExceptionHandler<RepositoryNotFoundException>> getType() {
		return RepositoryNotFoundExceptionHandler.class;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.AbstractExceptionHandler#getLogger()
	 */
	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void logErrorMessage(Logger logger, RepositoryNotFoundException exception) {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.AbstractExceptionHandler#getResponseStatus(java.lang.Throwable)
	 */
	@Override
	protected Status getResponseStatus(RepositoryNotFoundException exception) {
		return Status.NOT_FOUND;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.commons.api.service.AbstractExceptionHandler#getResponseMessage(java.lang.Throwable)
	 */
	@Override
	protected String getResponseMessage(RepositoryNotFoundException exception) {
		return exception.getMessage();
	}

}
