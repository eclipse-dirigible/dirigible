package org.eclipse.dirigible.components.api.sharepoint;

public class SharePointOperationException extends RuntimeException {

    public SharePointOperationException(String message) {
        super(message);
    }

    public SharePointOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
