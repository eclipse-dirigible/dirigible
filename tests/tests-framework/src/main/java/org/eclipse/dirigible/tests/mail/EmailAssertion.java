package org.eclipse.dirigible.tests.mail;

public record EmailAssertion(String from, String to, String subject, String toContainExactBody, String bodyRegex) {
}
