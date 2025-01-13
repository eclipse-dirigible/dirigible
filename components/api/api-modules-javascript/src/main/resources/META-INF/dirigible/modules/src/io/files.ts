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
 * API Files
 */
import { InputStream, OutputStream } from "sdk/io/streams";
import { Bytes } from "sdk/io/bytes";

const FilesFacade = Java.type("org.eclipse.dirigible.components.api.io.FilesFacade");
const File = Java.type("java.io.File")

export interface FileObject {
    name: string;
    path: string;
}

export interface FolderObject extends FileObject {
    files: FileObject[];
    folders: FolderObject[];
}

export class Files {

	public static readonly separator: string = File.separator;

	public static exists(path: string): boolean {
		return FilesFacade.exists(path);
	}

	public static isExecutable(path: string): boolean {
		return FilesFacade.isExecutable(path);
	}

	public static isReadable(path: string): boolean {
		return FilesFacade.isReadable(path);
	}

	public static isWritable(path: string): boolean {
		return FilesFacade.isWritable(path);
	}

	public static isHidden(path: string): boolean {
		return FilesFacade.isHidden(path);
	}

	public static isDirectory(path: string): boolean {
		return FilesFacade.isDirectory(path);
	}

	public static isFile(path: string): boolean {
		return FilesFacade.isFile(path);
	}

	public static isSameFile(path1: string, path2: string): boolean {
		return FilesFacade.isSameFile(path1, path2);
	}

	public static getCanonicalPath(path: string): string {
		return FilesFacade.getCanonicalPath(path);
	}

	public static getName(path: string): string {
		return FilesFacade.getName(path);
	}

	public static getParentPath(path: string): string {
		return FilesFacade.getParentPath(path);
	}

	public static readBytes(path: string): any[] {
		const native = FilesFacade.readBytes(path);
		return Bytes.toJavaScriptBytes(native);
	}

	public static readBytesNative(path: string): any[] {
		return FilesFacade.readBytes(path);
	}

	public static readText(path: string): string {
		return FilesFacade.readText(path);
	}

	public static writeBytes(path: string, data: any[]): void {
		const native = Bytes.toJavaBytes(data);
		FilesFacade.writeBytesNative(path, native);
	}

	public static writeBytesNative(path: string, data: any[]): void {
		FilesFacade.writeBytesNative(path, data);
	}

	public static writeText(path: string, text: string): void {
		FilesFacade.writeText(path, text);
	}

	public static getLastModified(path: string): Date {
		return new Date(FilesFacade.getLastModified(path));
	}

	public static setLastModified(path: string, time: Date): void {
		FilesFacade.setLastModified(path, time.getTime());
	}

	public static getOwner(path: string): string {
		return FilesFacade.getOwner(path);
	}

	public static setOwner(path: string, owner: string): void {
		FilesFacade.setOwner(path, owner);
	}

	public static getPermissions(path: string): string {
		return FilesFacade.getPermissions(path);
	}

	public static setPermissions(path: string, permissions: string): void {
		FilesFacade.setPermissions(path, permissions);
	}

	public static size(path: string): number {
		return FilesFacade.size(path);
	}

	public static createFile(path: string): void {
		FilesFacade.createFile(path);
	}

	public static createDirectory(path: string): void {
		FilesFacade.createDirectory(path);
	}

	public static copy(source: string, target: string): void {
		FilesFacade.copy(source, target);
	}

	public static move(source: string, target: string): void {
		FilesFacade.move(source, target);
	}

	public static deleteFile(path: string): void {
		FilesFacade.deleteFile(path);
	}

	public static deleteDirectory(path: string, forced?: boolean): void {
		FilesFacade.deleteDirectory(path, forced);
	}

	public static createTempFile(prefix: string, suffix: string): string {
		return FilesFacade.createTempFile(prefix, suffix);
	}

	public static createTempDirectory(prefix: string): string {
		return FilesFacade.createTempDirectory(prefix);
	}

	public static createInputStream(path: string): InputStream {
		const native = FilesFacade.createInputStream(path);
		return new InputStream(native);
	}

	public static createOutputStream(path: string): OutputStream {
		const native = FilesFacade.createOutputStream(path);
		return new OutputStream(native);
	}

	public static traverse(path: string): FolderObject[] {
		return JSON.parse(FilesFacade.traverse(path));
	}

	public static list(path: string): string[] {
		return JSON.parse(FilesFacade.list(path)).map(e => e.path);
	}

	public static find(path: string, pattern: string): string[] {
		return JSON.parse(FilesFacade.find(path, pattern));
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Files;
}
