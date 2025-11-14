package com.xiarui.board_game_backend.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${mq.mail.queue:mail.queue}")
    private String mailQueueName;

    @Value("${mq.mail.exchange:mail.exchange}")
    private String mailExchangeName;

    @Value("${mq.mail.routing-key:mail.routing}")
    private String mailRoutingKey;

    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(mailQueueName).build();
    }

    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(mailExchangeName);
    }

    @Bean
    public Binding mailBinding(Queue mailQueue, DirectExchange mailExchange) {
        return BindingBuilder.bind(mailQueue).to(mailExchange).with(mailRoutingKey);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
}
