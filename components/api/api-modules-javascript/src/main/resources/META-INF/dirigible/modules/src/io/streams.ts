/**
 * Provides core functionality for input/output stream management,
 * including stream creation, data transfer, and byte array handling.
 */
import { Bytes } from "./bytes";

const StreamsFacade = Java.type("org.eclipse.dirigible.components.api.io.StreamsFacade");

/**
 * The Streams class provides static utility methods for stream creation,
 * manipulation, and data copying.
 */
export class Streams {

	/**
	 * Copies all bytes from the input stream to the output stream.
	 * This method is generally used for smaller streams.
	 *
	 * @param input The source {@link InputStream}.
	 * @param output The destination {@link OutputStream}.
	 */
	public static copy(input: InputStream, output: OutputStream): void {
		StreamsFacade.copy(input.native, output.native);
	}

	/**
	 * Copies all bytes from the input stream to the output stream using a large buffer,
	 * suitable for large file transfers.
	 *
	 * @param input The source {@link InputStream}.
	 * @param output The destination {@link OutputStream}.
	 */
	public static copyLarge(input: InputStream, output: OutputStream): void {
		StreamsFacade.copyLarge(input.native, output.native);
	}

	/**
	 * Creates a new {@link InputStream} from a resource accessible via the class loader.
	 * This is typically used to read bundled resources within the application runtime.
	 *
	 * @param path The path to the resource.
	 * @returns A new {@link InputStream} instance for the resource.
	 */
	public static getResourceAsByteArrayInputStream(path: string): InputStream {
		const native = StreamsFacade.getResourceAsByteArrayInputStream(path);
		return new InputStream(native);
	}

	/**
	 * Creates a new {@link InputStream} from a JavaScript byte array (`any[]`).
	 *
	 * @param data The JavaScript array of byte values (`number[]`).
	 * @returns A new {@link InputStream} instance initialized with the byte data.
	 */
	public static createByteArrayInputStream(data: any[]): InputStream {
		// Convert JavaScript byte array to native Java byte array
		const array = Bytes.toJavaBytes(data);
		const native = StreamsFacade.createByteArrayInputStream(array);
		return new InputStream(native);
	}

	/**
	 * Creates a new {@link OutputStream} that writes data into an in-memory byte array.
	 * This is typically used as a buffer to capture output before processing it.
	 *
	 * @returns A new {@link OutputStream} instance backed by a byte array.
	 */
	public static createByteArrayOutputStream(): OutputStream {
		const native = StreamsFacade.createByteArrayOutputStream();
		return new OutputStream(native);
	}

	/**
	 * Wraps a native (Java) InputStream object into a new JavaScript {@link InputStream} instance.
	 *
	 * @param native The underlying native InputStream object.
	 * @returns A new {@link InputStream} wrapper.
	 */
	public static createInputStream(native: any): InputStream {
		const inputStream = new InputStream(native);
		return inputStream;
	}

	/**
	 * Wraps a native (Java) OutputStream object into a new JavaScript {@link OutputStream} instance.
	 *
	 * Note: This method is not static in the original definition, but is placed here for completeness
	 * and consistency with other factory methods.
	 *
	 * @param native The underlying native OutputStream object.
	 * @returns A new {@link OutputStream} wrapper.
	 */
	public createOutputStream(native: any): OutputStream {
		const outputStream = new OutputStream(native);
		return outputStream;
	}
}

/**
 * Represents an input stream for reading bytes.
 * This class wraps a native stream object and provides methods for reading data.
 */
export class InputStream {

	/** The underlying native Java stream object. */
	public readonly native: any;

	/**
	 * @param native The native Java InputStream object.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Reads the next byte of data from this input stream.
	 * @returns The next byte of data, or -1 if the end of the stream is reached.
	 */
	public read(): number {
		return StreamsFacade.read(this.native);
	}

	/**
	 * Reads all remaining bytes from the stream and returns them as a JavaScript array.
	 *
	 * @returns A JavaScript array (`number[]`) of the byte values.
	 */
	public readBytes(): any[] {
		const native = StreamsFacade.readBytes(this.native);
		return Bytes.toJavaScriptBytes(native);
	}

	/**
	 * Reads all remaining bytes from the stream and returns the native Java byte array.
	 *
	 * @returns The native Java byte array object.
	 */
	public readBytesNative(): any[] {
		return StreamsFacade.readBytes(this.native);
	}

	/**
	 * Reads all remaining bytes from the stream and converts them to a string
	 * using the platform's default character encoding.
	 *
	 * @returns The content of the stream as a string.
	 */
	public readText(): string {
		return StreamsFacade.readText(this.native);
	}

	/**
	 * Closes this input stream and releases any system resources associated with it.
	 */
	public close(): void {
		StreamsFacade.close(this.native);
	}

	/**
	 * Checks if the underlying native stream object is defined and non-null.
	 * @returns True if the stream is valid, false otherwise.
	 */
	public isValid(): boolean {
		return this.native !== undefined && this.native !== null;
	}
}

/**
 * Represents an output stream for writing bytes.
 * This class wraps a native stream object and provides methods for writing data.
 */
export class OutputStream {

	/** The underlying native Java stream object. */
	public readonly native: any;

	/**
	 * @param native The native Java OutputStream object.
	 */
	constructor(native: any) {
		this.native = native;
	}

	/**
	 * Writes the specified byte to this output stream.
	 * @param byte The byte (as a number 0-255) to write.
	 */
	public write(byte: number): void {
		StreamsFacade.write(this.native, byte);
	}

	/**
	 * Writes the entire content of a JavaScript byte array to this output stream.
	 *
	 * @param data The JavaScript array (`number[]`) of byte values to write.
	 */
	public writeBytes(data: any[]): void {
		// Convert JavaScript byte array to native Java byte array
		const native = Bytes.toJavaBytes(data);
		StreamsFacade.writeBytes(this.native, native);
	}

	/**
	 * Writes the entire content of a native Java byte array to this output stream.
	 *
	 * @param data The native Java byte array object to write.
	 */
	public writeBytesNative(data: any[]): void {
		StreamsFacade.writeBytes(this.native, data);
	}

	/**
	 * Converts the string to bytes using the platform's default character encoding
	 * and writes them to this output stream.
	 *
	 * @param text The string content to write.
	 */
	public writeText(text: string): void {
		StreamsFacade.writeText(this.native, text);
	}

	/**
	 * Closes this output stream and releases any system resources associated with it.
	 */
	public close(): void {
		StreamsFacade.close(this.native);
	}

	/**
	 * Retrieves the content written to this stream as a JavaScript byte array.
	 * This is typically used with a ByteArrayOutputStream.
	 *
	 * @returns A JavaScript array (`number[]`) of the byte values written to the stream.
	 */
	public getBytes(): any[] {
		const native = StreamsFacade.getBytes(this.native);
		return Bytes.toJavaScriptBytes(native);
	}

	/**
	 * Retrieves the content written to this stream as the native Java byte array.
	 * This is typically used with a ByteArrayOutputStream.
	 *
	 * @returns The native Java byte array object.
	 */
	public getBytesNative(): any[] {
		return StreamsFacade.getBytes(this.native);
	}

	/**
	 * Retrieves the content written to this stream as a string using the platform's
	 * default character encoding. This is typically used with a ByteArrayOutputStream.
	 *
	 * @returns The content of the stream as a string.
	 */
	public getText(): string {
		return StreamsFacade.getText(this.native);
	}

	/**
	 * Checks if the underlying native stream object is defined and non-null.
	 * @returns True if the stream is valid, false otherwise.
	 */
	public isValid(): boolean {
		return this.native !== undefined && this.native !== null;
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Streams;
}
