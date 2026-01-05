import { Bytes } from "@aerokit/sdk/io/bytes";
const QRCodeFacade = Java.type("org.eclipse.dirigible.components.api.utils.QRCodeFacade");

/**
 * Utility class for generating QR codes.
 * It uses the underlying native Java QRCodeFacade to convert text into
 * a QR code image represented as a raw byte array.
 */
export class QRCode {

    /**
     * Generates a QR code image byte array from the given text.
     * The returned byte array represents the image data (e.g., PNG or JPEG format,
     * depending on the native implementation's default output).
     *
     * @param text The string content to be encoded in the QR code.
     * @returns A **JavaScript byte array (any[])** containing the raw QR code image data.
     */
    public static generateQRCode(text: string): any[] {
        return Bytes.toJavaScriptBytes(QRCodeFacade.generateQRCode(text));
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = QRCode;
}
