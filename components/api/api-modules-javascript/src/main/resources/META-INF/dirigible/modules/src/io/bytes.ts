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
 * API Bytes
 */

/**
 * Convert the native JavaScript byte array to Java one. To be used internally by the API layer
 */

const JString = Java.type("java.lang.String");
const JByte = Java.type("java.lang.Byte");
const JArray = Java.type("java.lang.reflect.Array");
const BytesFacade = Java.type("org.eclipse.dirigible.components.api.io.BytesFacade");

export class Bytes {

	/**
	 * Convert the JavaScript byte array to a native Java one. To be used internally by the API layer
	 * 
	 * @param bytes 
	 * @returns 
	 */
	public static toJavaBytes(bytes: any[]): any[] {
		const internalBytes = JArray.newInstance(JByte.TYPE, bytes.length);
		for (let i = 0; i < bytes.length; i++) {
			internalBytes[i] = bytes[i];
		}
		return internalBytes;
	}

	/**
	 * Convert the Java byte array to a native JavaScript one. To be used internally by the API layer
	 * 
	 * @param internalBytes 
	 * @returns 
	 */
	public static toJavaScriptBytes(internalBytes: any[]): any[] {
		const bytes = [];
		for (let i = 0; i < internalBytes.length; i++) {
			bytes.push(internalBytes[i]);
		}
		return bytes;
	}

	/**
	 * Converts a text to a byte array
	 * 
	 * @param text 
	 * @returns 
	 */
	public static textToByteArray(text: string): any[] {
		const javaString = new JString(text);
		const native = BytesFacade.textToByteArray(text);
		return Bytes.toJavaScriptBytes(native);
	}

	/**
	 * Converts a byte array to text
	 * 
	 * @param data 
	 * @returns 
	 */
	public static byteArrayToText(data: any[]): string {
		const native = Bytes.toJavaBytes(data);
		return String.fromCharCode.apply(String, Bytes.toJavaScriptBytes(native));
	}

	/**
	 * Converts an integer to a byte array
	 * 
	 * @param value 
	 * @param byteOrder 
	 * @returns 
	 */
	public static intToByteArray(value: number, byteOrder:  "BIG_ENDIAN" | "LITTLE_ENDIAN"): any[] {
		return BytesFacade.intToByteArray(value, byteOrder)
	}

	/**
	 * Converts a byte array to integer
	 * 
	 * @param data 
	 * @param byteOrder 
	 * @returns 
	 */
	public static byteArrayToInt(data: any[], byteOrder: "BIG_ENDIAN" | "LITTLE_ENDIAN"): number {
		return BytesFacade.byteArrayToInt(data, byteOrder);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Bytes;
}
