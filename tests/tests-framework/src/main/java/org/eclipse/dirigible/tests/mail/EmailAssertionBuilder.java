package org.eclipse.dirigible.tests.mail;

public class EmailAssertionBuilder {

    private String from;
    private String to;
    private String subject;
    private String toContainExactBody;
    private String bodyRegex;

    public EmailAssertion build() {
        return new EmailAssertion(from, to, subject, toContainExactBody, bodyRegex);
    }

    public EmailAssertionBuilder expectedFrom(String from) {
        this.from = from;
        return this;
    }

    public EmailAssertionBuilder expectedTo(String to) {
        this.to = to;
        return this;
    }

    public EmailAssertionBuilder expectedSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public EmailAssertionBuilder expectedToContainBody(String body) {
        this.toContainExactBody = body;
        return this;
    }

    public EmailAssertionBuilder expectedBodyRegex(String bodyRegex) {
        this.bodyRegex = bodyRegex;
        return this;
    }
}
