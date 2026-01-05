import { Controller, Post } from "@aerokit/sdk/http"
import { client as mailClient } from "@aerokit/sdk/mail";

@Controller
class MailService {

    @Post("/sendTestEmail")
    public sendTestEmail() {

        const from = 'from@example.com';
        const to = 'to@example.com';
        const subject = "A test email";
        const content = `<h2>Test email content</h2>`;
        const subType = "html";
        mailClient.send(from, to, subject, content, subType);

        return "Mail has been sent"
    }

}
