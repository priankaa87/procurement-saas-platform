package com.procurementsaas.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default channel: records the message instead of sending it.
 *
 * <p>To plug in real delivery, add an {@link EmailSender} bean marked {@code @Primary}
 * (or profile-scoped) — no calling code changes.
 *
 * <p>Note: {@code @ConditionalOnMissingBean} would be the wrong tool here; it is only
 * dependable on auto-configuration classes, not on scanned components.
 */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String recipient, String subject, String body) {
        log.info("EMAIL -> {} | {} | {}", recipient, subject, body);
    }
}
