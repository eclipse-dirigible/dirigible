/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.core.generation.api;

/**
 * The Generation Exception.
 */
public class GenerationException extends RuntimeException {

	private static final long serialVersionUID = -2161860568272479874L;

	/**
	 * Instantiates a new Generation exception.
	 */
	public GenerationException() {
		super();
	}

	/**
	 * Instantiates a new Generation exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 * @param enableSuppression
	 *            the enable suppression
	 * @param writableStackTrace
	 *            the writable stack trace
	 */
	public GenerationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * Instantiates a new Generation exception.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public GenerationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new Generation exception.
	 *
	 * @param message
	 *            the message
	 */
	public GenerationException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new Generation exception.
	 *
	 * @param cause
	 *            the cause
	 */
	public GenerationException(Throwable cause) {
		super(cause);
	}

}
