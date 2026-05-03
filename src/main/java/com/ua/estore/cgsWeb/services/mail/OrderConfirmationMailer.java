package com.ua.estore.cgsWeb.services.mail;

import com.ua.estore.cgsWeb.config.props.MailProperties;
import com.ua.estore.cgsWeb.models.mail.MailMessage;
import com.ua.estore.cgsWeb.models.shop.Order;
import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the "Your order is on its way" email from a persisted {@link Order}.
 *
 * <p>Two responsibilities:</p>
 * <ol>
 *   <li>Render two Groovy MTE templates (HTML + plain text) into strings.</li>
 *   <li>Hand them to {@link MailService} as a single {@link MailMessage}.</li>
 * </ol>
 *
 * <p>Templates live at {@code templates/email/order-confirmation.html.tpl} and
 * {@code .txt.tpl}. The MTE engine here is a private instance — we don't
 * reuse the web-side engine because that one's tuned for HTML output with
 * site-wide bindings, and email rendering wants different defaults.</p>
 */
@Slf4j
@Component
public class OrderConfirmationMailer {

    private static final String TEMPLATE_BASE = "templates/email/";

    private final MailService mailService;
    private final MailProperties mailProps;
    private final MarkupTemplateEngine engine;

    @SuppressWarnings("unused")  // imagesBaseUrl reserved for future image inlining
    public OrderConfirmationMailer(MailService mailService,
                                   MailProperties mailProps,
                                   @Value("${app.images.base-url:}") String imagesBaseUrl) {
        this.mailService = mailService;
        this.mailProps = mailProps;

        TemplateConfiguration cfg = new TemplateConfiguration();
        cfg.setAutoEscape(true);            // safety: nothing in an email should be raw HTML
        cfg.setAutoIndent(true);
        cfg.setExpandEmptyElements(true);
        // Two-arg constructor: classloader + config. Template paths are resolved
        // by us via the classpath, not by the engine, which keeps this code
        // portable across Groovy versions that vary the third constructor arg.
        this.engine = new MarkupTemplateEngine(getClass().getClassLoader(), cfg);
    }

    public void sendFor(Order order) {
        if (order == null || order.getCustomer() == null
                || order.getCustomer().getEmail() == null
                || order.getCustomer().getEmail().isBlank()) {
            log.warn("Skipping order-confirmation email: order or recipient missing. orderId={}",
                    order != null ? order.getId() : null);
            return;
        }

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("order", order);
        bindings.put("orderUrl", mailProps.publicBaseUrl() + "/checkout/confirmation/" + order.getId());
        bindings.put("placedAt", order.getPlacedAt() != null
                ? order.getPlacedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"))
                : "");
        bindings.put("fmt", (java.util.function.Function<BigDecimal, String>)
                v -> v == null ? "0.00" : String.format("%.2f", v));

        String html;
        String text;
        try {
            // Filenames match what's in /resources/templates/email/.
            html = render("order-confirmation.html.tpl", bindings);
            text = render("order-confirmation.txt.tpl", bindings);
        } catch (Exception ex) {
            log.error("Failed to render order-confirmation templates for orderId={}",
                    order.getId(), ex);
            return;
        }

        MailMessage msg = MailMessage.builder()
                .to(order.getCustomer().getEmail())
                .subject("Your Celtech General Store order #" + order.getOrderNumber())
                .html(html)
                .text(text)
                .build();

        mailService.send(msg);
    }

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
            String source = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try {
                return engine.createTemplate(source);
            } catch (ClassNotFoundException ex) {
                throw new IOException("Failed to compile email template: " + resourcePath, ex);
            }
        }
    }
}