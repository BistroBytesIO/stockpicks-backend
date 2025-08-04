package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.StockPick;
import com.stockpicks.backend.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public void sendWelcomeEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to Stock Picks - Your Journey Begins!");

            Context context = new Context();
            context.setVariable("firstName", user.getFirstName());
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("welcome-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    public void sendSubscriptionConfirmationEmail(User user, String planName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Confirmed - " + planName);

            Context context = new Context();
            context.setVariable("firstName", user.getFirstName());
            context.setVariable("planName", planName);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("subscription-confirmation", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send subscription confirmation email", e);
        }
    }

    public void sendNewStockPickNotification(List<User> subscribers, List<StockPick> newPicks) {
        for (User user : subscribers) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(user.getEmail());
                helper.setSubject("New Stock Picks Available!");

                Context context = new Context();
                context.setVariable("firstName", user.getFirstName());
                context.setVariable("stockPicks", newPicks);
                context.setVariable("baseUrl", baseUrl);

                String htmlContent = templateEngine.process("new-stock-picks", context);
                helper.setText(htmlContent, true);

                mailSender.send(message);
            } catch (MessagingException e) {
                System.err.println("Failed to send stock pick notification to " + user.getEmail() + ": " + e.getMessage());
            }
        }
    }

    public void sendSubscriptionExpiryNotification(User user, int daysUntilExpiry) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Subscription Expiring Soon - " + daysUntilExpiry + " days remaining");

            Context context = new Context();
            context.setVariable("firstName", user.getFirstName());
            context.setVariable("daysUntilExpiry", daysUntilExpiry);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("subscription-expiry", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send subscription expiry notification", e);
        }
    }

    public void sendContactFormNotification(String name, String email, String subject, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("New Contact Form Submission: " + subject);

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", email);
            context.setVariable("subject", subject);
            context.setVariable("message", message);

            String htmlContent = templateEngine.process("contact-form-notification", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send contact form notification", e);
        }
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Password Reset Request");

            Context context = new Context();
            context.setVariable("firstName", user.getFirstName());
            context.setVariable("resetUrl", baseUrl + "/reset-password?token=" + resetToken);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("password-reset", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}