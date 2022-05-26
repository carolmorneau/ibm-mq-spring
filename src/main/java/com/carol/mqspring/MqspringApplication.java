package com.carol.mqspring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;

@SpringBootApplication
@RestController
public class MqspringApplication {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private JMSContext jmsContext;

	@Autowired(required = false)
	private JndiDestinationResolver jndiDestinationResolver;

	public static void main(String[] args) {
		SpringApplication.run(MqspringApplication.class, args);
	}

	@GetMapping("test")
	String test() {
//		jmsTemplate.setDestinationResolver(); // May specify an instance of JndiDestinationResolver
		jmsTemplate.convertAndSend("DEV.QUEUE.1", "This is a message");
		return "ALl good";
	}

	@GetMapping("send")
	String send() throws JMSException {
		try{
			Queue queue = (Queue) jndiDestinationResolver.resolveDestinationName(null, "DEMO_JMS_QUEUE", false);
//			Queue queue = jmsContext.createQueue("DEV.QUEUE.1");

			TextMessage textMessage = jmsContext.createTextMessage();
			textMessage.setText("My TextMessage");
			textMessage.setStringProperty("MyCustomStringProperty", "MyString");
			textMessage.setLongProperty("MyCustomLongProperty", 100L);

			//Supports Integer, Double, Long, and String
//			textMessage.setObjectProperty("MyObjectProperty", new MyObject("MyString", 122));
//
			jmsContext.createProducer().send(queue, textMessage);

//			context.createProducer().send(queue, "A text message");
//			context.createProducer().send(queue, "A text message".getBytes(StandardCharsets.UTF_8));
//			Map<String, Object> map = new HashMap<>();
//			map.put("Key1", "Value1");
//			map.put("Key2", true);
//			context.createProducer().send(queue, map);

//			context.createProducer().send(queue, new Address("Sherman", 1135));

//			StreamMessage smo = context.createStreamMessage();
//			smo.writeString("256");
//			smo.writeInt(512);
//			smo.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 37);
//			context.createProducer().send(queue, smo);


//			TextMessage tmo = connectionFactory..createTextMessage();
//			tmo.setText("Sent in the character set defined for the destination");
//			producer.send(tmo);
//			jmsTemplate.convertAndSend("DEV.QUEUE.1", "Hola World!");
			return "OK";
		}catch(JmsException ex){
			ex.printStackTrace();
			return "FAIL";
		}
	}

	@GetMapping("recv")
	String recv(){
		try{
			Object obj = jmsTemplate.receiveAndConvert("DEV.QUEUE.2");
			return obj.toString();
		}catch(JmsException ex){
			ex.printStackTrace();
			return "FAIL";
		}

	}

}
