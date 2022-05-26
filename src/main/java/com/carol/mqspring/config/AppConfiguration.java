package com.carol.mqspring.config;

import com.ibm.mq.spring.boot.MQAutoConfiguration;
import com.ibm.mq.spring.boot.MQConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;

@Configuration
@EnableConfigurationProperties(MQConfigurationProperties.class)
@AutoConfigureBefore(MQAutoConfiguration.class)
@Import(JndiConnectionFactoryWithUserCredentialsAutoConfiguration.class)
public class AppConfiguration {

    @Bean
    public JMSContext jmsContext(ConnectionFactory connectionFactory, MQConfigurationProperties mqProperties, ApplicationContext appContext) {
        JMSContext context = connectionFactory.createContext(mqProperties.getUser(), mqProperties.getPassword());
        context.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException exception) {
                System.out.println("Received exception in my ExceptionListener. Exception=" + exception);
            }
        });
        return context;
    }

}
