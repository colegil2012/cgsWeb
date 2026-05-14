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
 * Builds the "confirm your email" message sent at signup.
 *
 * <p>Structurally identical to {@code OrderConfirmationMailer} — same private
 * {@link MarkupTemplateEngine}, same {@code render()} helper, same
 * classpath template loading, same hand-off to {@link MailService}. The only
 * differences are the bindings and the subject line. Keeping the mailers
 * structurally uniform means the mail package has one pattern to learn, not
 * three.</p>
 *
 * <p>The caller (a credential/registration service, wired in round two)
 * obtains a raw token from {@code TokenService.issue(userId, EMAIL_VERIFICATION)}
 * and passes it here. This class doesn't know how tokens are generated or
 * stored — it just embeds the raw token into the verification link.</p>
 *
 * <p>Templates: {@code templates/email/signup-verification.html.tpl} and
 * {@code .txt.tpl}.</p>
 */
@Slf4j
@Component
public class SignupConfirmationMailer {

    private static final String TEMPLATE_BASE = "templates/email/";

    private final MailService mailService;
    private final MailProperties mailProps;
    private final MarkupTemplateEngine engine;

    public SignupConfirmationMailer(MailService mailService, MailProperties mailProps) {
        this.mailService = mailService;
        this.mailProps = mailProps;

        TemplateConfiguration cfg = new TemplateConfiguration();
        cfg.setAutoEscape(true);          // nothing in an email should be raw HTML
        cfg.setAutoIndent(true);
        cfg.setExpandEmptyElements(true);
        this.engine = new MarkupTemplateEngine(getClass().getClassLoader(), cfg);
    }

    /**
     * Send the verification email.
     *
     * @param user     the freshly-registered user (must have a non-blank email)
     * @param rawToken the raw verification token from {@code TokenService.issue(...)}
     */
    public void sendFor(User user, String rawToken) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping signup-verification email: user or recipient missing. userId={}",
                    user != null ? user.getId() : null);
            return;
        }
        if (rawToken == null || rawToken.isBlank()) {
            log.error("Skipping signup-verification email: no token provided. userId={}",
                    user.getId());
            return;
        }

        // The verification endpoint (round two) will accept the token as a
        // query param. URL-encode defensively even though base64url tokens
        // are already URL-safe — costs nothing, and protects against any
        // future change to token encoding.
        String verifyUrl = mailProps.publicBaseUrl()
                + "/account/verify?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        String firstName = (user.getProfile() != null && user.getProfile().getFirstName() != null)
                ? user.getProfile().getFirstName()
                : "there";

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("firstName", firstName);
        bindings.put("verifyUrl", verifyUrl);
        bindings.put("expiryHours", 48); // matches app.tokens.verification-ttl

        String html;
        String text;
        try {
            html = render("signup-verification.html.tpl", bindings);
            text = render("signup-verification.txt.tpl", bindings);
        } catch (Exception ex) {
            log.error("Failed to render signup-verification templates for userId={}",
                    user.getId(), ex);
            return;
        }

        MailMessage msg = MailMessage.builder()
                .to(user.getEmail())
                .subject("Confirm your Celtech General Store account")
                .html(html)
                .text(text)
                .build();

        mailService.send(msg);
    }

    // ====================================================================
    // Template rendering — same helpers as OrderConfirmationMailer
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