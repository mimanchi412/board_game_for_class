package com.xiarui.board_game_backend.common.service;

/**
 * 邮件服务.
 */
public interface MailService {

    /**
     * 发送通用文本邮件.
     *
     * @param to 收件人
     * @param subject 主题
     * @param content 正文
     */
    void sendPlainTextMail(String to, String subject, String content);

    /**
     * 发送验证码邮件.
     *
     * @param to 收件人
     * @param code 验证码
     */
    void sendVerificationCodeMail(String to, String code);
}
