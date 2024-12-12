package org.eclipse.dirigible.components.ide.git.domain;

/**
 * The Class GitUrlOutput.
 */
public class GitUrlOutput {

    /** The url. */
    private String url;

    /** The status. */
    private String status;

    /**
     * Instantiates a new git url input.
     */
    public GitUrlOutput() {
        super();
    }

    /**
     * Instantiates a new git url input.
     *
     * @param url the url
     * @param status the status
     */
    public GitUrlOutput(String url, String status) {
        super();
        this.url = url;
        this.status = url;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }



}
