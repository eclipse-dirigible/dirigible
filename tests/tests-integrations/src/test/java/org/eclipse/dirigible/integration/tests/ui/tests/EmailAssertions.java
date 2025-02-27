package org.eclipse.dirigible.integration.tests.ui.tests;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailAssertions {

    public static void assertEmailReceived(MimeMessage email, String expectedSubject, String expectedContent) throws MessagingException {
        assertThat(email.getSubject()).isEqualTo(expectedSubject);
        assertThat(email.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(email.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("john.doe.employee@example.com");

<<<<<<< HEAD
        String emailBody = GreenMailUtil.getBody(email)
                                        .trim();
=======
        String emailBody = GreenMailUtil.getBody(email).trim();
>>>>>>> b4aa97c2cf (fix: fixing after review)

        String extractedFromDate = extractDate(emailBody, "from \\[");
        String extractedToDate = extractDate(emailBody, "to \\[");

        assertThat(extractedFromDate).isEqualTo("2002-02-02");
        assertThat(extractedToDate).isEqualTo("2002-03-03");

        assertThat(emailBody).contains(expectedContent);
    }

    private static String extractDate(String emailBody, String pattern) {
        return emailBody.split(pattern)[1].split("T")[0];
    }
}
