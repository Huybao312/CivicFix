package com.civicfix.backend.accounts;

import java.util.UUID;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Deliver recovery mail after commit so public response timing does not depend on SMTP. */
@Component
public class RecoveryDelivery implements DisposableBean {
  private static final Logger log = LoggerFactory.getLogger(RecoveryDelivery.class);
  private final LocalMailService mail;
  private final ExecutorService executor =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(100),
          r -> {
            Thread t = new Thread(r, "recovery-mail");
            t.setDaemon(true);
            return t;
          });

  public RecoveryDelivery(LocalMailService mail) {
    this.mail = mail;
  }

  public void afterCommit(UUID user, String email, String text) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              executor.execute(() -> {
                try {
                  mail.send(email, "Đặt lại mật khẩu CivicFix", text);
                } catch (RuntimeException e) {
                  log.warn("Recovery email delivery failed for user {}", user);
                }
              });
            } catch (RejectedExecutionException e) {
              log.warn("Recovery email queue is full");
            }
          }
        });
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }
}
