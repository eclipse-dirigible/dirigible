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

const WorkspaceFacade = Java.type("org.eclipse.dirigible.components.api.platform.WorkspaceFacade");

/**
 * Workspace object
 */
export class Workspace {

	public static createWorkspace(name: string): Workspace {
		const native = WorkspaceFacade.createWorkspace(name);
		return new Workspace(native);
	}

	public static getWorkspace(name: string): Workspace {
		const native = WorkspaceFacade.getWorkspace(name);
		return new Workspace(native);
	}

	public static getWorkspacesNames(): string[] {
		const workspacesNames = WorkspaceFacade.getWorkspacesNames();
		if (workspacesNames) {
			return JSON.parse(workspacesNames);
		}
		return workspacesNames;
	}

	public static deleteWorkspace(name: string): void {
		WorkspaceFacade.deleteWorkspace(name);
	}

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getProjects(): Projects {
		const native = this.native.getProjects();
		return new Projects(native);
	}

	public createProject(name: string): Project {
		const native = this.native.createProject(name);
		return new Project(native);
	}

	public getProject(name: string): Project {
		const native = this.native.getProject(name);
		return new Project(native);
	}

	public deleteProject(name: string): void {
		this.native.deleteProject(name);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	public copyProject(source: string, target: string): void {
		this.native.copyProject(source, target);
	}

	public moveProject(source: string, target: string): void {
		this.native.moveProject(source, target);
	}

}

/**
 * Projects object
 */
export class Projects {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public size(): number {
		return this.native.size();
	}

	public get(index: number): Project {
		const native = this.native.get(index);
		return new Project(native);
	}

}

/**
 * Project object
 */
export class Project {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	}

	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	public createFolder(path: string): Folder {
		const native = this.native.createFolder(path);
		return new Folder(native);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	public getFolder(path: string): Folder {
		const native = this.native.getFolder(path);
		return new Folder(native);
	}

	public getFolders(path: string): Folders {
		const native = this.native.getFolders(path);
		return new Folders(native);
	}

	public deleteFolder(path: string): void {
		this.native.deleteFolder(path);
	}

	public createFile(path: string, input: any[] = []): File {
		const native = this.native.createFile(path, input);
		return new File(native);
	}

	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	public getFile(path: string): File {
		const native = this.native.getFile(path);
		return new File(native);
	}

	public getFiles(path: string): Files {
		const native = this.native.getFiles(path);
		return new Files(native);
	}

	public deleteFile(path: string): void {
		this.native.deleteFile(path);
	}

}

/**
 * Folders object
 */
export class Folders {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public size(): number {
		return this.native.size();
	}

	public get(index: number): Folder {
		const native = this.native.get(index);
		return new Folder(native);
	}

}

/**
 * Files object
 */
export class Files {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public size(): number {
		return this.native.size();
	}

	public get(index: number): File {
		const native = this.native.get(index);
		return new File(native);
	}

}

/**
 * Folder object
 */
export class Folder {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	};

	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	public createFolder(path: string): Folder {
		const native = this.native.createFolder(path);
		return new Folder(native);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public existsFolder(path: string): boolean {
		return this.native.existsFolder(path);
	}

	public getFolder(path: string): Folder {
		const native = this.native.getFolder(path);
		return new Folder(native);
	}

	public getFolders(path: string): Folders {
		const native = this.native.getFolders(path);
		return new Folders(native);
	}

	public deleteFolder(path: string): void {
		this.native.deleteFolder(path);
	}

	public createFile(path: string, input: any[] = []): File {
		const native = this.native.createFile(path, input);
		return new File(native);
	}

	public existsFile(path: string): boolean {
		return this.native.existsFile(path);
	}

	public getFile(path: string): File {
		const native = this.native.getFile(path);
		return new File(native);
	}

	public getFiles(path: string): Files {
		const native = this.native.getFiles(path);
		return new Files(native);
	}

	public deleteFile(path: string): void {
		this.native.deleteFile(path);
	}

}

/**
 * File object
 */
export class File {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		const collection = this.native.getInternal();
		return collection.getName();
	}

	public getPath(): string {
		const collection = this.native.getInternal();
		return collection.getPath();
	}

	public getContentType(): string {
		return this.native.getContentType();
	}

	public isBinary(): boolean {
		return this.native.isBinary();
	}

	public getContent(): any[] {
		const output = WorkspaceFacade.getContent(this.native);
		if (output) {
			output;
		}
		return output;
	}

	public getText(): string {
		const bytesOutput = this.getContent();
		return Bytes.byteArrayToText(bytesOutput);
	}

	public setContent(input: any[]): void {
		WorkspaceFacade.setContent(this.native, input);
	}

	public setText(input: string): void {
		const bytesInput = Bytes.textToByteArray(input);
		this.setContent(bytesInput);
	}

	public exists(): boolean {
		return this.native.exists();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Workspace;
}
