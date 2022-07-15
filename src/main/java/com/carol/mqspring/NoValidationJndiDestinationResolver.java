package com.carol.mqspring;

import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.Destination;

public class NoValidationJndiDestinationResolver extends JndiDestinationResolver {

    @Override
    protected void validateDestination(Destination destination, String destinationName, boolean pubSubDomain) {
        //No validation
    }
}
