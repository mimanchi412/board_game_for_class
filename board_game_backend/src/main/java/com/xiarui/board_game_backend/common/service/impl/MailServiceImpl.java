package com.xiarui.board_game_backend.common.service.impl;

import com.xiarui.board_game_backend.common.entity.MailMessage;
import com.xiarui.board_game_backend.common.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mq.mail.exchange:mail.exchange}")
    private String mailExchange;

    @Value("${mq.mail.routing-key:mail.routing}")
    private String mailRoutingKey;

    @Value("${spring.application.name:BoardGame}")
    private String applicationName;

    @Override
    public void sendPlainTextMail(String to, String subject, String content) {
        MailMessage mailMessage = MailMessage.builder()
                .to(to)
                .subject(subject)
                .content(content)
                .build();
        rabbitTemplate.convertAndSend(mailExchange, mailRoutingKey, mailMessage);
        log.info("Published mail message to queue, to={}", to);
    }

    @Override
    public void sendVerificationCodeMail(String to, String code) {
        String subject = "Board Game 验证码";
        String content = String.format("您正在进行%s账户相关操作，验证码为：%s，该验证码5分钟内有效。", applicationName, code);
        sendPlainTextMail(to, subject, content);
    }
}
