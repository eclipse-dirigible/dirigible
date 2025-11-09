import { Request } from "sdk/http/request";
import { Base64 } from "sdk/utils/base64";
import { Streams, InputStream } from "sdk/io/streams";

const MessageFactory = Java.type("javax.xml.soap.MessageFactory");
const MimeHeadersInternal = Java.type("javax.xml.soap.MimeHeaders");
const SOAPConnectionFactory = Java.type("javax.xml.soap.SOAPConnectionFactory");

/**
 * Utility class for creating, parsing, and calling SOAP messages.
 * It wraps the underlying Java javax.xml.soap API.
 */
export class SOAP {

	/**
	 * Call a given SOAP endpoint with a given request message
	 * @param message The SOAP Message wrapper object.
	 * @param url The target SOAP endpoint URL.
	 */
	public static call(message: Message, url: string) {
		const soapConnectionFactory = SOAPConnectionFactory.newInstance();
		const internalConnection = soapConnectionFactory.createConnection();
		// Accessing the internal property native (renamed from 'native' for better encapsulation)
		const internalResponse = internalConnection.call(message.native, url);
		return new Message(internalResponse);
	}

	public static trustAll() {
		// TODO: Implement logic for trusting all certificates if required by the SDK environment
	}

	/**
	 * Creates a new, empty SOAP message.
	 */
	public static createMessage(): Message {
		return new Message(MessageFactory.newInstance().createMessage());
	}

	/**
	 * Parses a SOAP message from an InputStream and MimeHeaders.
	 * @param mimeHeaders The MimeHeaders wrapper object.
	 * @param inputStream The InputStream wrapper object.
	 */
	public static parseMessage(mimeHeaders: MimeHeaders, inputStream: InputStream): Message {
		const internalFactory = MessageFactory.newInstance();
		// Use native for internal Java object access
		if (inputStream.native) {
			try {
				// Use native for internal Java object access
				const internalMessage = internalFactory.createMessage(mimeHeaders.native, inputStream.native);
				const internalPart = internalMessage.getSOAPPart();
				internalPart.getEnvelope();
				return new Message(internalMessage);
			} catch (e: any) {
				console.error(e);
				throw new Error("Input provided is null or in a wrong format. HTTP method used must be POST. " + e.message);
			}
		}
		throw new Error("Input provided is null.");
	}

	/**
	 * Parses a SOAP message from the current HTTP request input stream.
	 */
	public static parseRequest(): Message {
		if (Request.getMethod().toUpperCase() !== "POST") {
			throw new Error("HTTP method used must be POST.");
		}

		const inputStream = Request.getInputStream();
		const mimeHeaders = this.createMimeHeaders();

		return this.parseMessage(mimeHeaders, inputStream);
	}

	/**
	 * Creates a new, empty MimeHeaders object.
	 */
	public static createMimeHeaders(): MimeHeaders {
		const internalMimeHeaders = new MimeHeadersInternal();
		return new MimeHeaders(internalMimeHeaders);
	}
}

/**
 * SOAP Message Wrapper
 */
class Message {

	// Renamed to native to signal internal-use property, improving encapsulation
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
		// Use native for internal Java object access
		this.native.writeTo(outputStream.native);
		return outputStream.getText();
	}
}

/**
 * SOAP Part Wrapper
 */
class Part {

	// Renamed to native
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public getEnvelope(): Envelope {
		return new Envelope(this.native.getEnvelope());
	}
}

/**
 * SOAP Mime Headers Wrapper
 */
class MimeHeaders {

	// Renamed to native
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
 * SOAP Envelope Wrapper
 */
class Envelope {
	// Renamed to native
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
 * SOAP Body Wrapper
 */
class Body {
	// Renamed to native
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
 * SOAP Header Wrapper
 */
class Header {
	// Renamed to native
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}

	public addHeaderElement(element: Element): void {
		this.native.addHeaderElement(element.native);
	}
}

/**
 * SOAP Name Wrapper
 */
class Name {
	private readonly native: any;

	constructor(native: any) {
		this.native = native;
	}
	
	public getNative(): string {
		return this.native;
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
 * SOAP Element Wrapper
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

	public addAttribute(name: Name, value: any): Element {
		// Use name.native for internal Java object access
		return new Element(this.native.addAttribute(name.getNative(), value));
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
			// This catch handles cases where the element might not be a SOAPElement
		}
		return undefined;
	}

	public getValue(): any {
		return this.native.getValue();
	}

	public isSOAPElement(): boolean {
		return this.getElementName() !== undefined;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = SOAP;
}