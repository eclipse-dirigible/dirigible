/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const EscapeFacade = Java.type("org.eclipse.dirigible.components.api.utils.EscapeFacade");

export class Escape {

	public static escapeCsv(input: string): string {
		return EscapeFacade.escapeCsv(input);
	}

	/**
	 * Escapes a Javascript string
	 */
	public static escapeJavascript(input: string): string {
		return EscapeFacade.escapeJavascript(input);
	}

	/**
	 * Escapes a HTML3 string
	 */
	public static escapeHtml3(input: string): string {
		return EscapeFacade.escapeHtml3(input);
	}

	/**
	 * Escapes a HTML4 string
	 */
	public static escapeHtml4(input: string): string {
		return EscapeFacade.escapeHtml4(input);
	}

	/**
	 * Escapes a Java string
	 */
	public static escapeJava(input: string): string {
		return EscapeFacade.escapeJava(input);
	}

	/**
	 * Escapes a JSON string
	 */
	public static escapeJson(input: string): string {
		return EscapeFacade.escapeJson(input);
	}

	/**
	 * Escapes a XML string
	 */
	public static escapeXml(input: string): string {
		return EscapeFacade.escapeXml(input);
	}

	/**
	 * Unescapes a CSV string
	 */
	public static unescapeCsv(input: string): string {
		return EscapeFacade.unescapeCsv(input);
	}

	/**
	 * Unescapes a Javascript string
	 */
	public static unescapeJavascript(input: string): string {
		return EscapeFacade.unescapeJavascript(input);
	}

	/**
	 * Unescapes a HTML3 string
	 */
	public static unescapeHtml3(input: string): string {
		return EscapeFacade.unescapeHtml3(input);
	}

	/**
	 * Unescapes a HTML4 string
	 */
	public static unescapeHtml4(input: string): string {
		return EscapeFacade.unescapeHtml4(input);
	}

	/**
	 * Unescapes a Java string
	 */
	public static unescapeJava(input: string): string {
		return EscapeFacade.unescapeJava(input);
	}

	/**
	 * Unescapes a JSON string
	 */
	public static unescapeJson(input: string): string {
		return EscapeFacade.unescapeJson(input);
	}

	/**
	 * Unescapes a XML string
	 */
	public static unescapeXml(input: string): string {
		return EscapeFacade.unescapeXml(input);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Escape;
}
