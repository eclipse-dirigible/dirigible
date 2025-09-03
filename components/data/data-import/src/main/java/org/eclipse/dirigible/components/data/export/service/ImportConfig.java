package org.eclipse.dirigible.components.data.export.service;

public class ImportConfig {

    private final boolean header;
    private final boolean useHeaderNames;
    private final boolean distinguishEmptyFromNull;
    private final String delimField;
    private final String delimEnclosing;
    private final String sequence;

    public ImportConfig(boolean header, boolean useHeaderNames, boolean distinguishEmptyFromNull, String delimField, String delimEnclosing,
            String sequence) {
        this.header = header;
        this.useHeaderNames = useHeaderNames;
        this.distinguishEmptyFromNull = distinguishEmptyFromNull;
        this.delimField = delimField;
        this.delimEnclosing = delimEnclosing;
        this.sequence = sequence;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isUseHeaderNames() {
        return useHeaderNames;
    }

    public boolean isDistinguishEmptyFromNull() {
        return distinguishEmptyFromNull;
    }

    public String getDelimField() {
        return delimField;
    }

    public String getDelimEnclosing() {
        return delimEnclosing;
    }

    public String getSequence() {
        return sequence;
    }
}
