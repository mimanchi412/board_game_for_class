package com.xiarui.board_game_backend.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件消息体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收件人 */
    private String to;

    /** 邮件主题 */
    private String subject;

    /** 邮件正文 */
    private String content;
}
