package org.eclipse.dirigible.components.data.management.helpers;

public class ResultParameters {

    private String dateFormat;

    public ResultParameters(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
