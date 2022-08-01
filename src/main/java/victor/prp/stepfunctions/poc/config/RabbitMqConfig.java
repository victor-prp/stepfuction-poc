package victor.prp.stepfunctions.poc.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

public class RabbitMqConfig {
    static public final String EXCHANGE_NAME = "sf-exchange";

    static public final String Q_NAME_PREFIX = "sf-queue-";


    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }


//    @Bean
//    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
//                                             MessageListenerAdapter listenerAdapter) {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(queueName);
//        container.setMessageListener(listenerAdapter);
//        return container;
//    }

//    @Bean
//    MessageListenerAdapter listenerAdapter(Receiver receiver) {
//        return new MessageListenerAdapter(receiver, "receiveMessage");
//    }
}
