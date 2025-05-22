/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Bytes } from "sdk/io/bytes";
import { Repository } from "sdk/platform/repository";
const RegistryFacade = Java.type("org.eclipse.dirigible.components.api.platform.RegistryFacade");

export class Registry {

	public static getContent(path: string): any[] {
		return Bytes.toJavaScriptBytes(RegistryFacade.getContent(path));
	}

	public static getContentNative(path: string): any[] {
		return RegistryFacade.getContent(path);
	}

	public static getText(path: string): string {
		return RegistryFacade.getText(path);
	}

	public static find(path: string, pattern: string): string[] {
		return JSON.parse(RegistryFacade.find(path, pattern));
	}
	
	public static getRoot(): Directory {
		return new Directory(Repository.getCollection("/registry/public"));
	}
}

export class Artefact {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	public getParent(): Directory {
		const collectionInstance = this.native.getParent();
		return new Directory(collectionInstance);
	}

	public getInformation(): ArtefactInformation {
		const informationInstance = this.native.getInformation();
		return new ArtefactInformation(informationInstance);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	public getText(): string {
		return Bytes.byteArrayToText(this.getContent());
	}

	public getContent(): any[] {
		const nativeContent = this.native.getContent();
		return Bytes.toJavaScriptBytes(nativeContent);
	}

	public getContentNative(): any[] {
		return this.native.getContent();
	}

	public isBinary(): boolean {
		return this.native.isBinary();
	}

	public getContentType(): string {
		return this.native.getContentType();
	}
}

export class Directory {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	public getParent(): Directory {
		const collectionInstance = this.native.getParent();
		return new Directory(collectionInstance);
	}

	public getInformation(): ArtefactInformation {
		const informationInstance = this.native.getInformation();
		return new ArtefactInformation(informationInstance);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	public getDirectoriesNames(): string[] {
		return this.native.getCollectionsNames();
	}

	public getDirectory(name: string): Directory {
		const collectionInstance = this.native.getCollection(RegistryFacade.toResourcePath(name));
		return new Directory(collectionInstance);
	}

	public getArtefactsNames(): string[] {
		return this.native.getResourcesNames();
	}

	public getArtefact(name: string): Artefact {
		const resourceInstance = this.native.getResource(RegistryFacade.toResourcePath(name));
		return new Artefact(resourceInstance);
	}

}

export class ArtefactInformation {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return RegistryFacade.toRegistryPath(this.native.getPath());
	}

	public getPermissions(): number {
		return this.native.getPermissions();
	}

	public getSize(): number {
		return this.native.getSize();
	}

	public getCreatedBy(): string {
		return this.native.getCreatedBy();
	}

	public getCreatedAt(): Date {
		return this.native.getCreatedAt();
	}

	public getModifiedBy(): string {
		return this.native.getModifiedBy();
	}

	public getModifiedAt(): Date {
		return this.native.getModifiedAt();
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Registry;
}
