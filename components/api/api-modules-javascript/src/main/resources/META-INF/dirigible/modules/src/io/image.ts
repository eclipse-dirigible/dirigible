/**
 * Provides a static façade for image manipulation operations,
 * primarily focusing on resizing image streams.
 */
import { InputStream } from "@aerokit/sdk/io/streams";

const ImageFacade = Java.type("org.eclipse.dirigible.components.api.io.ImageFacade");

/**
 * The Image class provides static methods for common image processing tasks.
 * All methods operate on and return {@link InputStream} objects, making them
 * suitable for piping image data through the file system or network.
 */
export class Image {

	/**
	 * Resizes an image contained within an InputStream to the specified dimensions.
	 *
	 * @param original The InputStream containing the original image data.
	 * @param type The target format of the resized image (e.g., "png", "jpeg", "gif").
	 * @param width The target width in pixels.
	 * @param height The target height in pixels.
	 * @returns A new InputStream containing the resized image data in the specified format.
	 */
	public static resize(original: InputStream, type: string, width: number, height: number): InputStream {
		// Delegates the resizing operation to the native Java facade, using the native stream object.
		const native = ImageFacade.resize(original.native, type, width, height);
		// Wraps the resulting native stream object back into a JavaScript InputStream instance.
		return new InputStream(native);
	}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Image;
}
