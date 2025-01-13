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
import { Bytes } from "sdk/io/bytes";

const StreamsFacade = Java.type("org.eclipse.dirigible.components.api.io.StreamsFacade");

export class Streams {

	public static copy(input: InputStream, output: OutputStream): void {
		StreamsFacade.copy(input.native, output.native);
	}

	public static copyLarge(input: InputStream, output: OutputStream) {
		StreamsFacade.copyLarge(input.native, output.native);
	}

	/**
	 * Get an ByteArrayInputStream for the provided resource
	 */
	public static getResourceAsByteArrayInputStream(path: string): InputStream {
		const native = StreamsFacade.getResourceAsByteArrayInputStream(path);
		return new InputStream(native);
	}

	/**
	 * Create an ByteArrayInputStream for byte array provided
	 */
	public static createByteArrayInputStream(data: any[]): InputStream {
		const array = Bytes.toJavaBytes(data);
		const native = StreamsFacade.createByteArrayInputStream(array);
		return new InputStream(native);
	}

	/**
	 * Create a ByteArrayOutputStream
	 */
	public static createByteArrayOutputStream(): OutputStream {
		const native = StreamsFacade.createByteArrayOutputStream();
		return new OutputStream(native);
	}

	/**
	 * Create an InputStream object by a native InputStream
	 */
	public static createInputStream(native: any) {
		const inputStream = new InputStream(native);
		return inputStream;
	}

	/**
	 * Create an OutputStream object by a native OutputStream
	 */
	public createOutputStream(native: any) {
		const outputStream = new OutputStream(native);
		return outputStream;
	}
}

/**
 * InputStream object. To be used internally by the API layer
 */
export class InputStream {

	public readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public read(): number {
		return StreamsFacade.read(this.native);
	}

	public readBytes(): any[] {
		const native = StreamsFacade.readBytes(this.native);
		return Bytes.toJavaScriptBytes(native);
	}

	public readBytesNative(): any[] {
		return StreamsFacade.readBytes(this.native);
	}

	public readText(): string {
		return StreamsFacade.readText(this.native);
	}

	public close(): void {
		StreamsFacade.close(this.native);
	}

	public isValid(): boolean {
		return this.native !== undefined && this.native !== null;
	}
}

/**
 * OutputStream object. To be used internally by the API layer
 */
export class OutputStream {

	public readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public write(byte: number): void {
		StreamsFacade.write(this.native, byte);
	}

	public writeBytes(data: any[]): void {
		const native = Bytes.toJavaBytes(data);
		StreamsFacade.writeBytes(this.native, native);
	}

	public writeBytesNative(data: any[]): void {
		StreamsFacade.writeBytes(this.native, data);
	}

	public writeText(text: string): void {
		StreamsFacade.writeText(this.native, text);
	}

	public close(): void {
		StreamsFacade.close(this.native);
	}

	public getBytes(): any[] {
		const native = StreamsFacade.getBytes(this.native);
		return Bytes.toJavaScriptBytes(native);
	}

	public getBytesNative(): any[] {
		return StreamsFacade.getBytes(this.native);
	}

	public getText(): string {
		return StreamsFacade.getText(this.native);
	}

	public isValid(): boolean {
		return this.native !== undefined && this.native !== null;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Streams;
}
