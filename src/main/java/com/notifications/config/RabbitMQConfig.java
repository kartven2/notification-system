package com.notifications.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * RabbitMQ configuration.
 *
 * <p>Declares four durable notification queues (IOS, Android, SMS, Email),
 * a dead-letter exchange for failed messages, and wires Jackson JSON message
 * conversion so payloads are human-readable AMQP messages.</p>
 */
@Configuration
public class RabbitMQConfig {

    // --- Queue names (injected from application.yml) ---

    @Value("${notification.queues.ios}")
    private String iosQueue;

    @Value("${notification.queues.android}")
    private String androidQueue;

    @Value("${notification.queues.sms}")
    private String smsQueue;

    @Value("${notification.queues.email}")
    private String emailQueue;

    // Dead-letter exchange / queue names
    public static final String DLX_EXCHANGE        = "notification.dlx";
    public static final String DLQ_IOS             = "notification.ios.dlq";
    public static final String DLQ_ANDROID         = "notification.android.dlq";
    public static final String DLQ_SMS             = "notification.sms.dlq";
    public static final String DLQ_EMAIL           = "notification.email.dlq";

    // --- Dead-Letter Exchange ---

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // --- Dead-Letter Queues ---

    @Bean public Queue dlqIos()     { return QueueBuilder.durable(DLQ_IOS).build(); }
    @Bean public Queue dlqAndroid() { return QueueBuilder.durable(DLQ_ANDROID).build(); }
    @Bean public Queue dlqSms()     { return QueueBuilder.durable(DLQ_SMS).build(); }
    @Bean public Queue dlqEmail()   { return QueueBuilder.durable(DLQ_EMAIL).build(); }

    @Bean public Binding bindingDlqIos()     { return BindingBuilder.bind(dlqIos()).to(deadLetterExchange()).with(DLQ_IOS); }
    @Bean public Binding bindingDlqAndroid() { return BindingBuilder.bind(dlqAndroid()).to(deadLetterExchange()).with(DLQ_ANDROID); }
    @Bean public Binding bindingDlqSms()     { return BindingBuilder.bind(dlqSms()).to(deadLetterExchange()).with(DLQ_SMS); }
    @Bean public Binding bindingDlqEmail()   { return BindingBuilder.bind(dlqEmail()).to(deadLetterExchange()).with(DLQ_EMAIL); }

    // --- Main Notification Queues ---

    @Bean
    public Queue iosNotificationQueue() {
        return QueueBuilder.durable(iosQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_IOS)
                .build();
    }

    @Bean
    public Queue androidNotificationQueue() {
        return QueueBuilder.durable(androidQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ANDROID)
                .build();
    }

    @Bean
    public Queue smsNotificationQueue() {
        return QueueBuilder.durable(smsQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_SMS)
                .build();
    }

    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable(emailQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_EMAIL)
                .build();
    }

    // --- JSON Message Converter ---

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- RabbitTemplate ---

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // --- Listener Container Factory (with retry) ---

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false); // send to DLQ on rejection
        return factory;
    }

    // --- Dead-letter recoverer (republish to DLX) ---

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX_EXCHANGE);
    }
}
