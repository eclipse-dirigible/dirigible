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

const RepositoryFacade = Java.type("org.eclipse.dirigible.components.api.platform.RepositoryFacade");

export class Repository {

	public static getResource(path: string): Resource {
		const resourceInstance = RepositoryFacade.getResource(path);
		return new Resource(resourceInstance);
	}

	public static createResource(path: string, content: string, contentType: string): Resource {
		const resourceInstance = RepositoryFacade.createResource(path, content, contentType);
		return new Resource(resourceInstance);
	}

	public static createResourceNative(path: string, content: any[], contentType: string): Resource {
		const resourceInstance = RepositoryFacade.createResourceNative(path, content, contentType);
		return new Resource(resourceInstance);
	}

	public static pdateResource(path: string, content: string): Resource {
		const resourceInstance = RepositoryFacade.updateResource(path, content);
		return new Resource(resourceInstance);
	}

	public static updateResourceNative(path: string, content: any[]): Resource {
		const resourceInstance = RepositoryFacade.updateResourceNative(path, content);
		return new Resource(resourceInstance);
	}

	public static deleteResource(path: string): void {
		RepositoryFacade.deleteResource(path);
	}

	public static getCollection(path: string): Collection {
		const collectionInstance = RepositoryFacade.getCollection(path);
		return new Collection(collectionInstance);
	}

	public static createCollection(path: string): Collection {
		const collectionInstance = RepositoryFacade.createCollection(path);
		return new Collection(collectionInstance);
	}

	public static deleteCollection(path: string): void {
		RepositoryFacade.deleteCollection(path);
	}

	public static find(path: string, pattern: string): string[] {
		return JSON.parse(RepositoryFacade.find(path, pattern));
	}
}

export class Resource {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return this.native.getPath();
	}

	public getParent(): Collection {
		const collectionInstance = this.native.getParent();
		return new Collection(collectionInstance);
	}

	public getInformation(): EntityInformation {
		const informationInstance = this.native.getInformation();
		return new EntityInformation(informationInstance);
	}

	public create(): void {
		this.native.create();
	}

	public delete(): void {
		this.native.delete();
	}

	public renameTo(name: string): void {
		this.native.renameTo(name);
	}

	public moveTo(path: string): void {
		this.native.moveTo(path);
	}

	public copyTo(path: string): void {
		this.native.copyTo(path);
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

	public setText(text: string): void {
		const content = Bytes.textToByteArray(text);
		this.setContent(content);
	}

	public setContent(content: any[]): void {
		const nativeContent = Bytes.toJavaBytes(content);
		this.native.setContent(nativeContent);
	}

	public setContentNative(content: any[]): void {
		this.native.setContent(content);
	}

	public isBinary(): boolean {
		return this.native.isBinary();
	}

	public getContentType(): string {
		return this.native.getContentType();
	}
}

export class Collection {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return this.native.getPath();
	}

	public getParent(): Collection {
		const collectionInstance = this.native.getParent();
		return new Collection(collectionInstance);
	}

	public getInformation(): EntityInformation {
		const informationInstance = this.native.getInformation();
		return new EntityInformation(informationInstance);
	}

	public create(): void {
		this.native.create();
	}

	public delete(): void {
		this.native.delete();
	}

	public renameTo(name: string): void {
		this.native.renameTo(name);
	}

	public moveTo(path: string): void {
		this.native.moveTo(path);
	}

	public copyTo(path: string): void {
		this.native.copyTo(path);
	}

	public exists(): boolean {
		return this.native.exists();
	}

	public isEmpty(): boolean {
		return this.native.isEmpty();
	}

	public getCollectionsNames(): string[] {
		return this.native.getCollectionsNames();
	}

	public createCollection(name: string): Collection {
		const collectionInstance = this.native.createCollection(name);
		return new Collection(collectionInstance);
	}

	public getCollection(name: string): Collection {
		const collectionInstance = this.native.getCollection(name);
		return new Collection(collectionInstance);
	}

	public removeCollection(name: string): void {
		this.native.removeCollection(name);
	}

	public getResourcesNames(): string[] {
		return this.native.getResourcesNames();
	}

	public getResource(name: string): Resource {
		const resourceInstance = this.native.getResource(name);
		return new Resource(resourceInstance);
	}

	public removeResource(name: string): void {
		this.native.removeResource(name);
	}

	public createResource(name: string, content: string): Resource {
		const resourceInstance = this.native.createResource(name, content);
		return new Resource(resourceInstance);
	}
}

export class EntityInformation {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getName(): string {
		return this.native.getName();
	}

	public getPath(): string {
		return this.native.getPath();
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
	module.exports = Repository;
}
