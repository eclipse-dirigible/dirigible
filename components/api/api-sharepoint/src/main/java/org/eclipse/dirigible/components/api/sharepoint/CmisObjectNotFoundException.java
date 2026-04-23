package org.eclipse.dirigible.components.api.sharepoint;

public class CmisObjectNotFoundException extends RuntimeException {

    public CmisObjectNotFoundException(String message) {
        super(message);
    }

    public CmisObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
