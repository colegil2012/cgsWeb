package com.ua.estore.cgsWeb.services.mail;

import com.ua.estore.cgsWeb.config.props.MailProperties;
import com.ua.estore.cgsWeb.models.mail.MailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class JavaMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailProperties props;

    @Override
    @Async
    public void send(MailMessage message) {
        if (!props.enabled()) {
            log.info("Mail subsystem disabled; would have sent to={}, subject='{}'",
                    message.getTo(), message.getSubject());
            return;
        }
        if (message.getTo() == null || message.getTo().isBlank()) {
            log.warn("Refusing to send mail with empty recipient: subject='{}'",
                    message.getSubject());
            return;
        }

        String from = message.getFromOverride() != null ? message.getFromOverride() : props.from();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            // multipart=true so we can attach plain-text + html alternatives;
            // UTF-8 so non-ASCII characters in names and item titles render.
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());

            helper.setFrom(buildFromAddress(from));
            if (props.replyTo() != null && !props.replyTo().isBlank()) {
                helper.setReplyTo(props.replyTo());
            }
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            // text first, html second — Spring picks the html part as the visible body
            // and the text part as the alternative for plain-text-only clients.
            if (message.getText() != null) {
                helper.setText(message.getText(), message.getHtml());
            } else {
                helper.setText(message.getHtml(), true);
            }

            mailSender.send(mime);
            log.info("Mail sent: to={}, subject='{}'", message.getTo(), message.getSubject());

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Mail send failed: to={}, subject='{}', error={}",
                    message.getTo(), message.getSubject(), ex.getMessage(), ex);
        } catch (Exception ex) {
            // Catch-all so any transport hiccup never propagates to the calling thread.
            log.error("Unexpected mail send error: to={}, subject='{}'",
                    message.getTo(), message.getSubject(), ex);
        }
    }

    /**
     * Build a "from" with the configured friendly name, e.g.
     * {@code "Celtech General Store" <support@celtechgs.com>}. Falls back to
     * the bare address if the name isn't configured.
     */
    private InternetAddress buildFromAddress(String address) throws UnsupportedEncodingException {
        if (props.fromName() == null || props.fromName().isBlank()) {
            try {
                return new InternetAddress(address);
            } catch (jakarta.mail.internet.AddressException e) {
                throw new UnsupportedEncodingException(e.getMessage());
            }
        }
        return new InternetAddress(address, props.fromName(), StandardCharsets.UTF_8.name());
    }
}