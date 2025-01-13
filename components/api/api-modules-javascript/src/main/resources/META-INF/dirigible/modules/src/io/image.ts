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
/**
 * API Image
 */
import { InputStream } from "sdk/io/streams";

const ImageFacade = Java.type("org.eclipse.dirigible.components.api.io.ImageFacade");

export class Image {

	public static resize(original: InputStream, type: string, width: number, height: number): InputStream {
		const native = ImageFacade.resize(original.native, type, width, height);
		return new InputStream(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Image;
}
