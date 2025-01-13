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
import { Request } from "sdk/http/request";
import { Base64 } from "sdk/utils/base64";
import { Streams, InputStream } from "sdk/io/streams";

const MessageFactory = Java.type("javax.xml.soap.MessageFactory");
const MimeHeadersInternal = Java.type("javax.xml.soap.MimeHeaders");
const SOAPConnectionFactory = Java.type("javax.xml.soap.SOAPConnectionFactory");

export class SOAP {

	/**
	 * Call a given SOAP endpoint with a given request message
	 */
	public static call(message: Message, url: string) {
		const soapConnectionFactory = SOAPConnectionFactory.newInstance();
		const internalConnection = soapConnectionFactory.createConnection();
		const internalResponse = internalConnection.call(message.native, url); // Trying to access private parameter of class Message!
		return new Message(internalResponse);
	}

	public static trustAll() {
		// TODO
	}

	public static createMessage(): Message {
		return new Message(MessageFactory.newInstance().createMessage());
	}

	public static parseMessage(mimeHeaders: MimeHeaders, inputStream: InputStream): Message {
		const internalFactory = MessageFactory.newInstance();
		if (inputStream.native) {
			try {
				const internalMessage = internalFactory.createMessage(mimeHeaders.native, inputStream.native);
				const internalPart = internalMessage.getSOAPPart();
				internalPart.getEnvelope();
				return new Message(internalMessage);
			} catch (e) {
				console.error(e);
				throw new Error("Input provided is null or in a worng format. HTTP method used must be POST. " + e.message);
			}
		}
		throw new Error("Input provided is null.");
	}

	public static parseRequest(): Message {
		if (Request.getMethod().toUpperCase() !== "POST") {
			throw new Error("HTTP method used must be POST.");
		}

		const inputStream = Request.getInputStream();
		const mimeHeaders = this.createMimeHeaders();

		return this.parseMessage(mimeHeaders, inputStream);
	}

	public static createMimeHeaders(): MimeHeaders {
		const internalMimeHeaders = new MimeHeadersInternal();
		return new MimeHeaders(internalMimeHeaders);
	}
}

/**
 * SOAP Message
 */
class Message {

	public readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getPart(): Part {
		return new Part(this.native.getSOAPPart());
	}

	public getMimeHeaders(): MimeHeaders {
		return new MimeHeaders(this.native.getMimeHeaders());
	}

	public save(): void {
		this.native.saveChanges();
	}

	public getText(): string {
		const outputStream = Streams.createByteArrayOutputStream();
		this.native.writeTo(outputStream.native);
		return outputStream.getText();
	}
}

/**
 * SOAP Part
 */
class Part {

	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getEnvelope(): Envelope {
		return new Envelope(this.native.getEnvelope());
	}
}

/**
 * SOAP Mime Headers
 */
class MimeHeaders {

	public readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addHeader(name: string, value: string): void {
		this.native.addHeader(name, value);
	}

	public addBasicAuthenticationHeader(username: string, password: string): void {
		const userAndPassword = `${username}:${password}`;
		const basicAuth = Base64.encode(userAndPassword);
		this.native.addHeader("Authorization", `Basic ${basicAuth}`);
	}
}

/**
 * SOAP Envelope
 */
class Envelope {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addNamespaceDeclaration(prefix: string, uri: string): void {
		this.native.addNamespaceDeclaration(prefix, uri);
	}

	public getBody(): Body {
		return new Body(this.native.getBody());
	}

	public getHeader(): Header {
		return new Header(this.native.getHeader());
	}

	public createName(localName: string, prefix: string, uri: string): Name {
		return new Name(this.native.createName(localName, prefix, uri));
	}
}

/**
 * SOAP Body
 */
class Body {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addChildElement(localName: string, prefix: string): Element {
		return new Element(this.native.addChildElement(localName, prefix));
	}

	public getChildElements(): Element[] {
		const childElements = [];
		const internalElementsIterator = this.native.getChildElements();
		while (internalElementsIterator.hasNext()) {
			childElements.push(new Element(internalElementsIterator.next()));
		}
		return childElements;
	}
}

/**
 * SOAP Header
 */
class Header {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addHeaderElement(element: Element): void {
		this.native.addHeaderElement(element.native);
	}
}

/**
 * SOAP Name
 */
class Name {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getLocalName(): string {
		return this.native.getLocalName();
	}

	public getPrefix(): string {
		return this.native.getPrefix();
	}

	public getQualifiedName(): string {
		return this.native.getQualifiedName();
	}

	public getURI(): string {
		return this.native.getURI();
	}
}

/**
 * SOAP Element
 */
class Element {
	public readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addChildElement(localName: string, prefix: string) {
		return new Element(this.native.addChildElement(localName, prefix));
	}

	public addTextNode(text: string): Element {
		return new Element(this.native.addTextNode(text));
	}

	public addAttribute(name: any, value: any): Element {
		return new Element(this.native.addAttribute(name.native, value));
	}

	public getChildElements(): Element[] {
		const childElements = [];
		const internalElementsIterator = this.native.getChildElements();
		while (internalElementsIterator.hasNext()) {
			childElements.push(new Element(internalElementsIterator.next()));
		}
		return childElements;
	}

	public getElementName(): Name | undefined {
		try {
			const internalName = this.native.getElementName();
			return new Name(internalName);
		} catch (e) {
			//  can we assume that always an exception here means the element is not an SOAPElement
			//	console.log(e);
		}
		return undefined;
	}

	public getValue(): any {
		return this.native.getValue();
	}

	public isSOAPElement(): boolean {
		return this.getElementName() !== null;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = SOAP;
}
