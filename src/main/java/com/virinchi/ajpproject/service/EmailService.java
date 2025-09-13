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
        msg.setText("Dear " + (firstName != null ? firstName : "there") + ",\n\n" +
                "🌟 Welcome to the Monito Family! 🐾\n\n" +
                "We're thrilled to have you join our community of pet lovers!\n\n" +
                "🎉 Your Monito account has been successfully created.\n" +
                "🏠 Browse amazing pets and accessories in our marketplace\n" +
                "💌 Connect with fellow pet enthusiasts\n" +
                "🎁 Get exclusive updates on new arrivals and special offers\n\n" +
                "If you have any questions or need assistance, our support team is here to help!\n\n" +
                "Wishing you a wonderful journey with us,\n" +
                "The Monito Team 🐾\n\n" +
                "P.S. Don't forget to check out our latest collection!");
        mailSender.send(msg);
    }
}


