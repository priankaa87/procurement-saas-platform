package com.procurementsaas.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A message template with {@code {{placeholder}}} variables, editable without a redeploy.
 */
@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    protected NotificationTemplate() {
    }

    public NotificationTemplate(String code, String subject, String body) {
        this.code = code;
        this.subject = subject;
        this.body = body;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
