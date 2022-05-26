package com.carol.mqspring.config;

import com.ibm.mq.spring.boot.MQAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.util.StringUtils;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;

@Configuration
@AutoConfigureBefore({ MQAutoConfiguration.class, JmsAutoConfiguration.class })
@ConditionalOnClass(JmsTemplate.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
@Conditional(JndiConnectionFactoryWithUserCredentialsAutoConfiguration.JndiOrPropertyCondition.class)
@EnableConfigurationProperties(JmsProperties.class)
public class JndiConnectionFactoryWithUserCredentialsAutoConfiguration {

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

    /**
     * Condition for JNDI name or a specific property.
     */
    static class JndiOrPropertyCondition extends AnyNestedCondition {

        JndiOrPropertyCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnJndi({ "java:/JmsXA", "java:/XAConnectionFactory" })
        static class Jndi {

        }

        @ConditionalOnProperty(prefix = "spring.jms", name = "jndi-name")
        static class Property {

        }

    }

}
