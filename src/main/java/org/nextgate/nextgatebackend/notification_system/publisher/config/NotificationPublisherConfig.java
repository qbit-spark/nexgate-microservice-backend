package org.nextgate.nextgatebackend.notification_system.publisher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification publisher
 * Ensures messages are sent as JSON and can be consumed properly
 */
@Configuration
public class NotificationPublisherConfig {

    @Bean
    public ObjectMapper notificationObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public Jackson2JsonMessageConverter notificationMessageConverter(ObjectMapper notificationObjectMapper) {
        return new Jackson2JsonMessageConverter(notificationObjectMapper);
    }

    @Bean
    public RabbitTemplate notificationRabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter notificationMessageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(notificationMessageConverter);
        return template;
    }
}