//package com.carol.mqspring;
//
//import org.springframework.jms.annotation.JmsListener;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.stereotype.Component;
//
//import javax.jms.Message;
//import java.util.Map;
//
//@Component
//public class Receiver {
//
//    @JmsListener(destination = "DEV.QUEUE.2"/*, containerFactory = "myFactory" */)
//    public void receiveMessage(Message jmsMessage, org.springframework.messaging.Message springMessage,
//                               @Headers Map<String, Object> headers, org.springframework.messaging.MessageHeaders allHeaders) {
//        System.out.println("Received from @JmsListener. ");
//        System.out.println("JMSMessage: " + jmsMessage);
//        System.out.println("SpringMessage: Payload:" + springMessage.getPayload() + " Headers: " + springMessage.getHeaders());
//        System.out.println("Headers: " + headers);
//        System.out.println("AllHeaders: " + allHeaders);
//
//    }
//
//}
