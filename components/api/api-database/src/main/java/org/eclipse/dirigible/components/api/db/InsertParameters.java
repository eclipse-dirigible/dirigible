package org.eclipse.dirigible.components.api.db;

public class InsertParameters {

    private String dateFormat;

    public InsertParameters(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

}
