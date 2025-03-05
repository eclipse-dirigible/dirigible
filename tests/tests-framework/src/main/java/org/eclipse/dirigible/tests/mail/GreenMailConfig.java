package org.eclipse.dirigible.tests.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.tests.util.PortUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class GreenMailConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(GreenMailConfig.class);

    private static final String MAIL_USER = "mailUser";
    private static final String MAIL_PASSWORD = "mailPassword";
    private static final int MAIL_PORT = PortUtil.getFreeRandomPort();

    @Bean
    GreenMail provideGreenMailServer() {
        ServerSetup serverSetup = new ServerSetup(MAIL_PORT, "localhost", "smtp");
        GreenMail greenMail = new GreenMail(serverSetup);
        greenMail.start();
        greenMail.setUser(MAIL_USER, MAIL_PASSWORD);

        return greenMail;
    }

    @PreDestroy
    public void shutdownGreenMail() {
        Optional<GreenMail> greenMail = BeanProvider.getOptionalBean(GreenMail.class);
        greenMail.ifPresent((gm) -> {
            LOGGER.info("Shutting down green mail...");
            gm.stop();
        });
    }

    private static void configureDirigibleEmailService() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(MAIL_PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }
}
