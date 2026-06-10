package com.waimai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mq")
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "waimai.order.exchange";
    public static final String ORDER_STATUS_QUEUE = "waimai.order.status.queue";
    public static final String ORDER_STATUS_ROUTING_KEY = "order.status.change";

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderStatusQueue() {
        return new Queue(ORDER_STATUS_QUEUE, true);
    }

    @Bean
    public Binding orderStatusBinding() {
        return BindingBuilder.bind(orderStatusQueue())
                .to(orderExchange())
                .with(ORDER_STATUS_ROUTING_KEY);
    }
}
