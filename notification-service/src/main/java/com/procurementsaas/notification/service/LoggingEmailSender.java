package com.procurementsaas.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Development/default channel: records the message instead of sending it.
 *
 * <p>Marked {@link ConditionalOnMissingBean} so wiring a real SMTP sender later replaces
 * this without touching any calling code.
 */
@Component
@ConditionalOnMissingBean(EmailSender.class)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String recipient, String subject, String body) {
        log.info("EMAIL -> {} | {} | {}", recipient, subject, body);
    }
}
