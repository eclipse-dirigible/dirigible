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
 * API Zip
 */
import { Bytes } from "sdk/io/bytes";
import { InputStream, OutputStream } from "sdk/io/streams";

const ZipFacade = Java.type("org.eclipse.dirigible.components.api.io.ZipFacade");

export class Zip {

    public static zip(sourcePath: string, zipTargetPath: string): void {
        ZipFacade.exportZip(sourcePath, zipTargetPath);
    }

    public static unzip(zipPath: string, targetPath: string): void {
        ZipFacade.importZip(zipPath, targetPath);
    }

    public static createZipInputStream(inputStream: InputStream): ZipInputStream {
        const native = ZipFacade.createZipInputStream(inputStream.native);
        return new ZipInputStream(native);
    }

    public static createZipOutputStream(outputStream: OutputStream): ZipOutputStream {
        const native = ZipFacade.createZipOutputStream(outputStream.native);
        return new ZipOutputStream(native);
    }
}

export class ZipInputStream {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public getNextEntry(): ZipEntry {
        const native = this.native.getNextEntry();
        return new ZipEntry(native);
    }

    public read(): any[] {
        const native = ZipFacade.readNative(this.native);
        return Bytes.toJavaScriptBytes(native);
    }

    public readNative(): any[] {
        return ZipFacade.readNative(this.native);
    }

    public readText(): string {
        return ZipFacade.readText(this.native);
    }

    public close(): void {
        this.native.close();
    }

}

export class ZipOutputStream {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public createZipEntry(name: string): ZipEntry {
        const nativeNext = ZipFacade.createZipEntry(name);
        const zipEntry = new ZipEntry(nativeNext);
        this.native.putNextEntry(nativeNext);
        return zipEntry;
    }

    public write(data: any[]): void {
        const native = Bytes.toJavaBytes(data);
        ZipFacade.writeNative(this.native, native);
    }

    public writeNative(data: any[]): void {
        ZipFacade.writeNative(this.native, data);
    }

    public writeText(text: string): void {
        ZipFacade.writeText(this.native, text);
    }

    public closeEntry(): void {
        this.native.closeEntry();
    }

    public close(): void {
        this.native.finish();
        this.native.flush();
        this.native.close();
    }
}

/**
 * ZipEntry object
 */
export class ZipEntry {

    private readonly native: any;

    constructor(native: any) {
        this.native = native;
    }

    public getName(): string {
        return this.native.getName();
    }

    public getSize(): number {
        return this.native.getSize();
    }

    public getCompressedSize(): number {
        return this.native.getCompressedSize();
    }

    public getTime(): number {
        return this.native.getTime();
    }

    public getCrc(): number {
        return this.native.getCrc();
    }

    public getComment(): string {
        return this.native.getComment();
    }

    public isDirectory(): boolean {
        return this.native.isDirectory();
    }

    public isValid(): boolean {
        return this.native !== undefined && this.native !== null;
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Zip;
}
