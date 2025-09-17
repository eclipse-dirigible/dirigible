package org.eclipse.dirigible.graalium.core.javascript;

public class GuestLanguageException extends RuntimeException {

    public GuestLanguageException(String path, String exMessage, String exClassName) {
        super("Exception in file [" + path + "] - exception class [" + exClassName + "], exception message [" + exMessage + "]");
    }
}
