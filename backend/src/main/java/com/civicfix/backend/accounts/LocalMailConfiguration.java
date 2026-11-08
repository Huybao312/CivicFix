package com.civicfix.backend.accounts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class LocalMailConfiguration {
  @Bean
  JavaMailSender localMailSender(@Value("${CIVICFIX_SMTP_PORT:1025}") int port) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    // This portfolio must never relay email to an external SMTP server.
    sender.setHost("127.0.0.1");
    sender.setPort(port);
    sender.setDefaultEncoding("UTF-8");
    sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "3000");
    sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "3000");
    sender.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "3000");
    return sender;
  }
}
