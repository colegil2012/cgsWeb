package com.ua.estore.cgsWeb.services.mail;

import com.ua.estore.cgsWeb.config.props.MailProperties;
import com.ua.estore.cgsWeb.models.mail.MailMessage;
import com.ua.estore.cgsWeb.models.user.User;
import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the "reset your password" message.
 *
 * <p>Same structural pattern as {@code OrderConfirmationMailer} and
 * {@code SignupConfirmationMailer} — private engine, {@code render()} helper,
 * classpath templates, hand-off to {@link MailService}.</p>
 *
 * <p><b>Enumeration-oracle note.</b> The endpoint that triggers this mailer
 * (round two) must respond identically whether or not the email belongs to a
 * real account — otherwise it becomes a way to discover which emails have
 * accounts. That responsibility lives in the <em>controller/service</em>:
 * this mailer is simply never called when there's no matching user. From this
 * class's perspective it always has a real {@link User}.</p>
 *
 * <p>Templates: {@code templates/email/password-reset.html.tpl} and
 * {@code .txt.tpl}.</p>
 */
@Slf4j
@Component
public class PasswordResetMailer {

    private static final String TEMPLATE_BASE = "templates/email/";

    private final MailService mailService;
    private final MailProperties mailProps;
    private final MarkupTemplateEngine engine;

    public PasswordResetMailer(MailService mailService, MailProperties mailProps) {
        this.mailService = mailService;
        this.mailProps = mailProps;

        TemplateConfiguration cfg = new TemplateConfiguration();
        cfg.setAutoEscape(true);
        cfg.setAutoIndent(true);
        cfg.setExpandEmptyElements(true);
        this.engine = new MarkupTemplateEngine(getClass().getClassLoader(), cfg);
    }

    /**
     * Send the password-reset email.
     *
     * @param user     the user who requested the reset (guaranteed real by the caller)
     * @param rawToken the raw reset token from {@code TokenService.issue(...)}
     */
    public void sendFor(User user, String rawToken) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping password-reset email: user or recipient missing. userId={}",
                    user != null ? user.getId() : null);
            return;
        }
        if (rawToken == null || rawToken.isBlank()) {
            log.error("Skipping password-reset email: no token provided. userId={}",
                    user.getId());
            return;
        }

        String resetUrl = mailProps.publicBaseUrl()
                + "/account/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        String firstName = (user.getProfile() != null && user.getProfile().getFirstName() != null)
                ? user.getProfile().getFirstName()
                : "there";

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("firstName", firstName);
        bindings.put("resetUrl", resetUrl);
        bindings.put("expiryMinutes", 30); // matches app.tokens.password-reset-ttl

        String html;
        String text;
        try {
            html = render("password-reset.html.tpl", bindings);
            text = render("password-reset.txt.tpl", bindings);
        } catch (Exception ex) {
            log.error("Failed to render password-reset templates for userId={}",
                    user.getId(), ex);
            return;
        }

        MailMessage msg = MailMessage.builder()
                .to(user.getEmail())
                .subject("Reset your Celtech General Store password")
                .html(html)
                .text(text)
                .build();

        mailService.send(msg);
    }

    // ====================================================================
    // Template rendering — same helpers as the other mailers
    // ====================================================================

    private String render(String templateName, Map<String, Object> bindings) throws IOException {
        Template tpl = createTemplate(templateName);
        StringWriter out = new StringWriter(8192);
        tpl.make(bindings).writeTo(out);
        return out.toString();
    }

    private Template createTemplate(String templateName) throws IOException {
        String resourcePath = TEMPLATE_BASE + templateName;
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Email template not found on classpath: " + resourcePath);
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try {
                return engine.createTemplate(source);
            } catch (ClassNotFoundException ex) {
                throw new IOException("Failed to compile email template: " + resourcePath, ex);
            }
        }
    }
}