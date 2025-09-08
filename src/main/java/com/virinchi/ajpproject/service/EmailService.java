package com.virinchi.ajpproject.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String toEmail, String firstName) {
        if (toEmail == null || toEmail.isBlank()) return;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject("Welcome to Monito");
        msg.setText("Hi " + (firstName != null ? firstName : "there") + ",\n\n" +
                "Welcome to Monito! Your account has been created successfully.\n\n" +
                "Happy browsing!\nMonito Team");
        mailSender.send(msg);
    }
}


