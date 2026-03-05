package com.example.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /* =======================
       Queue Names
       ======================= */

    public static final String MAIL_QUEUE = "mail-queue";
    public static final String MAIL_RETRY_QUEUE = "mail-retry-queue";
    public static final String MAIL_DLQ = "mail-dlq";
    public static final String DOMAIN_VERIFICATION_QUEUE = "domain-verification-queue";
    public static final String DOMAIN_VERIFICATION_DELAY_QUEUE = "domain-verification-delay-queue";
    public static final String EMAIL_VERIFICATION_QUEUE = "email-verification-queue";
    public static final String EMAIL_VERIFICATION_DELAY_QUEUE = "email-verification-delay-queue";

    /* =======================
       Main Queue
       ======================= */

    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(MAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", MAIL_RETRY_QUEUE)
                .build();
    }

    /* =======================
       Retry Queue (Delay)
       ======================= */

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(MAIL_RETRY_QUEUE)
                .withArgument("x-message-ttl", 30000) // 30 seconds delay
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", MAIL_QUEUE)
                .build();
    }

    /* =======================
       Dead Letter Queue
       ======================= */

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MAIL_DLQ).build();
    }

    /* =======================
       JSON Message Converter
       ======================= */

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /* =======================
       RabbitTemplate (Producer)
       ======================= */

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /* =======================
       Listener Container Factory
       ======================= */

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(50);
        // 🔹 Concurrency
        factory.setConcurrentConsumers(10);
        factory.setMaxConcurrentConsumers(50);

        // 🔹 Manual acknowledgment
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 🔹 JSON conversion
        factory.setMessageConverter(jsonMessageConverter());

        return factory;
    }

    @Bean
    Queue domainVerificationQueue() {
        return new Queue(DOMAIN_VERIFICATION_QUEUE);
    }

    @Bean
    Queue emailVerificationQueue() {
        return new Queue(EMAIL_VERIFICATION_QUEUE);
    }

    @Bean
    Queue domainVerificationDelayQueue() {

        Map<String, Object> args = new HashMap<>();

        args.put("x-message-ttl", 120000); // 2 minutes
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", DOMAIN_VERIFICATION_QUEUE);

        return new Queue(DOMAIN_VERIFICATION_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    Queue emailVerificationDelayQueue() {

        Map<String, Object> args = new HashMap<>();

        args.put("x-message-ttl", 120000); // 2 minutes
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", EMAIL_VERIFICATION_QUEUE);

        return new Queue(EMAIL_VERIFICATION_DELAY_QUEUE, true, false, false, args);
    }
}
