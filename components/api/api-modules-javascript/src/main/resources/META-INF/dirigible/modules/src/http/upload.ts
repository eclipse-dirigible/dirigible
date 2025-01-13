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
 * HTTP API Upload
 *
 */
import { InputStream } from "sdk/io/streams"
import { Bytes } from "sdk/io/bytes"

const HttpUploadFacade = Java.type("org.eclipse.dirigible.components.api.http.HttpUploadFacade");

export class Upload {

    public static isMultipartContent(): boolean {
        return HttpUploadFacade.isMultipartContent();
    }

    public static parseRequest(): FileItems {
        return new FileItems(__context.get("files"));
    }
}

/**
 * FileItems object
 */
export class FileItems {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public get(index: number): FileItem {
        const native = this.native.get(index);
        return new FileItem(native);
    }

    public size(): number {
        return this.native.size();
    }
}

/**
 * FileItem object
 */
export class FileItem {

    private readonly native: any

    constructor(native: any) {
        this.native = native;
    }

    public getName(): string {
        return this.native.getOriginalFilename();
    }

    public getContentType(): string {
        return this.native.getContentType();
    }

    public isEmpty(): boolean {
        return this.native.isEmpty();
    }

    public getSize(): number {
        return this.native.getSize();
    }

    public getBytes(): any[] {
        const data = this.getBytesNative();
        return Bytes.toJavaScriptBytes(data);
    }

    public getBytesNative(): any[] {
        return this.native.getBytes();
    }

    public getText(): string {
        return String.fromCharCode.apply(null, this.getBytesNative());
    }

    public getInputStream(): InputStream {
        const native = this.native.getInputStream();
        return new InputStream(native);
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Upload;
}
