package com.xiarui.board_game_backend.common.listener;

import com.xiarui.board_game_backend.common.entity.MailMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮件消息监听器，负责真正发送邮件.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailMessageListener {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Value("${mail.from:}")
    private String mailFrom;

    @RabbitListener(queues = "${mq.mail.queue:mail.queue}")
    public void handleMailMessage(MailMessage mailMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo(mailMessage.getTo());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(mailMessage.getContent(), false);
            helper.setFrom(resolveSender());
            mailSender.send(mimeMessage);
            log.info("Mail sent to {}", mailMessage.getTo());
        } catch (Exception e) {
            log.error("Failed to send mail to {}", mailMessage.getTo(), e);
        }
    }

    private String resolveSender() {
        if (StringUtils.hasText(mailFrom)) {
            return mailFrom;
        }
        return mailProperties.getUsername();
    }
}
