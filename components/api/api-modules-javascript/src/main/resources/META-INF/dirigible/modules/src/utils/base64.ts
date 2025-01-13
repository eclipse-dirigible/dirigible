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
import { Streams } from "sdk/io/streams";
import { Bytes } from "sdk/io/bytes";

const Base64Facade = Java.type("org.eclipse.dirigible.components.api.utils.Base64Facade");

export class Base64 {

	/**
	 * Encode the input (text or byte array) as text
	 */
	public static encode(input: string | any[]): string {
		return Bytes.byteArrayToText(Base64.encodeAsNativeBytes(input));
	}

	/**
	 * Encode the input (text or byte array) as byte array
	 */
	public static encodeAsBytes(input: string | any[]): any[] {
		return Bytes.toJavaScriptBytes(Base64.encodeAsNativeBytes(input));
	}

	/**
	 * Encode the input (text or byte array) as java native byte array
	 */
	public static encodeAsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}

		return Base64Facade.encodeNative(native);
	}

	/**
	 * Decode the input (text or byte array) as text
	 */
	public static decode(input: string | any[]): any[] {
		const output = Base64.decodeAsNativeBytes(input);
		if (output) {
			return Bytes.toJavaScriptBytes(output);
		}
		return output;
	}

	/**
	 * Decode the input (text or byte array) as java native byte array
	 */
	public static decodeAsNativeBytes(input: string | any[]): any[] {
		const data = input;
		let native;
		if (typeof data === 'string') {
			const baos = Streams.createByteArrayOutputStream();
			baos.writeText(data);
			native = baos.getBytesNative();
		} else if (Array.isArray(data)) {
			native = Bytes.toJavaBytes(data);
		}
		return Base64Facade.decodeNative(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Base64;
}
