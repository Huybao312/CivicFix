package com.civicfix.backend.accounts;

import static com.civicfix.backend.accounts.ApiErrors.Problem;

import java.util.Locale;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class LocalMailService {
  private final JavaMailSender sender;

  public LocalMailService(JavaMailSender sender) {
    this.sender = sender;
  }

  public void send(String to, String subject, String text) {
    if (!to.toLowerCase(Locale.ROOT).endsWith("@demo.civicfix.local"))
      throw new Problem(422, "Demo chỉ dùng email @demo.civicfix.local.");
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom("no-reply@demo.civicfix.local");
    mail.setTo(to);
    mail.setSubject(subject);
    mail.setText(text);
    try {
      sender.send(mail);
    } catch (MailException e) {
      throw new Problem(503, "Hộp thư thử nghiệm chưa sẵn sàng. Hãy kiểm tra Mailpit và thử lại.");
    }
  }
}
