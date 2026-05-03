package com.ua.estore.cgsWeb.services.mail;

import com.ua.estore.cgsWeb.models.mail.MailMessage;

public interface MailService {
    void send(MailMessage message);
}