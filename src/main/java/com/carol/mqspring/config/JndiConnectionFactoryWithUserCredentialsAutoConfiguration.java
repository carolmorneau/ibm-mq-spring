package com.carol.mqspring.config;

import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

@Configuration
public class JndiConnectionFactoryWithUserCredentialsAutoConfiguration extends JndiConnectionFactoryAutoConfiguration {

    @Bean
    public ConnectionFactory jmsConnectionFactory(JmsProperties properties) throws NamingException {
        JndiLocatorDelegate jndiLocatorDelegate = JndiLocatorDelegate.createDefaultResourceRefLocator();
        if (StringUtils.hasLength(properties.getJndiName())) {
            ConnectionFactory connectionFactory = jndiLocatorDelegate.lookup(properties.getJndiName(), ConnectionFactory.class);
            UserCredentialsConnectionFactoryAdapter cfCredentialAdapter = new UserCredentialsConnectionFactoryAdapter();
            cfCredentialAdapter.setTargetConnectionFactory(connectionFactory);
            cfCredentialAdapter.setUsername("admin");
            cfCredentialAdapter.setPassword("passw0rd");
            return cfCredentialAdapter;
        } else {
            return null;
        }
    }

}
