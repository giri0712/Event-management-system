package com.eventmgmt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("mailTaskExecutor")
    public void sendTicketConfirmation(String recipientEmail, String attendeeName, String eventTitle, 
                                       int ticketCount, double totalAmount, String ticketRef) {
        
        String emailContent = String.format("""
                Dear %s,
                
                Thank you for registering for %s! Your ticket purchase has been confirmed.
                
                ====================================================
                TICKET REGISTRATION DETAILS
                ====================================================
                Booking Reference: %s
                Event Name:        %s
                Tickets Purchased: %d
                Total Amount Paid: $%.2f
                Transaction Date:  %s
                ====================================================
                
                Please present this ticket reference at the entrance of the event.
                
                Best regards,
                Event Management Team
                """, 
                attendeeName, eventTitle, ticketRef, eventTitle, ticketCount, totalAmount, LocalDateTime.now());

        logger.info("------------------------------------------------------------------");
        logger.info("ASYNC EMAIL INITIATED (Thread: {})", Thread.currentThread().getName());
        logger.info("Sending ticket receipt email to: {}", recipientEmail);
        logger.info("------------------------------------------------------------------");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Ticket Confirmation - " + eventTitle);
            message.setText(emailContent);
            mailSender.send(message);
            logger.info("Real email successfully sent to: {}", recipientEmail);
        } catch (Exception e) {
            logger.warn("SMTP mail server connection failed (normal for local environments without mail configs).");
            logger.info("Fallback: Logging email body locally to project directory.");
            writeEmailToLocalLog(recipientEmail, eventTitle, emailContent);
        }
        
        logger.info("------------------------------------------------------------------");
    }

    private synchronized void writeEmailToLocalLog(String recipientEmail, String eventTitle, String content) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        File emailLog = new File(logsDir, "sent_emails.txt");
        try (FileWriter writer = new FileWriter(emailLog, true)) {
            writer.write("\n====================================================\n");
            writer.write("TIMESTAMP: " + LocalDateTime.now() + "\n");
            writer.write("RECIPIENT: " + recipientEmail + "\n");
            writer.write("SUBJECT:   Ticket Confirmation - " + eventTitle + "\n");
            writer.write("----------------------------------------------------\n");
            writer.write(content);
            writer.write("====================================================\n");
            logger.info("Email written to local log file: {}", emailLog.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write email receipt to local log file: {}", e.getMessage());
        }
    }
}
