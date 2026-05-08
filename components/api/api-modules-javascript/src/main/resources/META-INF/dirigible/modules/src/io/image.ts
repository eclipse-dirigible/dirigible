/**
 * @module io/image
 * @package @aerokit/sdk/io
 * @name Image
 * @overview
 * 
 * The Image class provides a static façade for performing common image manipulation operations, primarily focusing on resizing images. It allows developers to easily resize images by specifying the desired dimensions and output format, while abstracting away the complexities of handling image processing directly.
 * 
 * ### Key Features:
 * - **Image Resizing**: The `resize` method enables resizing an image to specified dimensions and output format, making it suitable for various use cases such as generating thumbnails or optimizing images for web delivery.
 * 
 * ### Use Cases:
 * - **Thumbnail Generation**: Developers can use the Image class to create smaller versions of images for use as thumbnails in galleries or listings.
 * - **Image Optimization**: Resizing images to appropriate dimensions can help optimize them for faster loading times on websites and applications.
 * 
 * ### Example Usage:
 * ```ts
 * import { Image } from "@aerokit/sdk/io";
 * import { InputStream } from "@aerokit/sdk/io/streams";
 * 
 * // Assume we have an InputStream containing the original image data
 * const originalImageStream = new InputStream(...);
 * 
 * // Resize the image to 200x200 pixels in PNG format
 * const resizedImageStream = Image.resize(originalImageStream, "png", 200, 200);
 * 
 * // The resizedImageStream can now be used for further processing or output
 * ```
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
