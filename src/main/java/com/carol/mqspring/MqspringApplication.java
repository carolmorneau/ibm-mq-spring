package com.carol.mqspring;

import com.carol.MySerializableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SpringBootApplication
@RestController
public class MqspringApplication {

	private static Logger logger = LoggerFactory.getLogger(MqspringApplication.class);

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private JMSContext jmsContext;

	private JMSProducer producer1;
	private JMSProducer producer2;

	public static void main(String[] args) {
		SpringApplication.run(MqspringApplication.class, args);
	}

	@PostConstruct
	public void setup() {
		producer1 = jmsContext.createProducer();
		producer1.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer1.setAsync(new CompletionListener() {
			@Override
			public void onCompletion(Message message) {
				logger.info("PRODUCER-1: Async Publish SUCCESS");
				logger.info(message.toString());
			}

			@Override
			public void onException(Message message, Exception exception) {
				logger.warn("PRODUCER-1: Async Publish FAILED", exception);
				logger.warn(message.toString());
			}
		});

	}

	@GetMapping("send")
	String send(@RequestParam(value = "dType", defaultValue = "T") String destinationType,
				@RequestParam(value = "dName", defaultValue = "topic") String destinationName,
				@RequestParam(value = "mType", defaultValue = "text") String messageType) {
		try {
			if (!destinationType.equalsIgnoreCase("T") && !destinationType.equalsIgnoreCase("Q")) {
				return "Invalid 'dType' parameter. Valid values are 'T' and 'Q'";
			}
			Destination destination = destinationType.equals("T") ? jmsContext.createTopic(destinationName) : jmsContext.createQueue(destinationName);

			Message msg;
			if (messageType.equalsIgnoreCase("text")) {
				msg = jmsContext.createTextMessage();
				((TextMessage) msg).setText("This is a text message");
			} else if (messageType.equalsIgnoreCase("bytes")) {
				msg = jmsContext.createBytesMessage();
				((BytesMessage) msg).writeBytes("This is a bytes messages".getBytes(StandardCharsets.UTF_8));
			} else if (messageType.equalsIgnoreCase("map")) {
				msg = jmsContext.createMapMessage();
				((MapMessage) msg).setString("MyString", "MyString");
				((MapMessage) msg).setShort("MyShort", (short) 3);
				((MapMessage) msg).setBoolean("MyBoolean", true);
			} else if (messageType.equalsIgnoreCase("object")) {
				msg = jmsContext.createObjectMessage();
				((ObjectMessage) msg).setObject(new MySerializableObject("aValue"));
			} else if (messageType.equalsIgnoreCase("stream")) {
				msg = jmsContext.createStreamMessage();
				((StreamMessage) msg).writeBytes("Those are my bytes".getBytes(StandardCharsets.UTF_8));
				((StreamMessage) msg).writeBoolean(true);
				((StreamMessage) msg).writeString("My String");
			} else if (messageType.equalsIgnoreCase("noPayload")) {
				//=========== NO PAYLOAD MESSAGE =====================
				msg = jmsContext.createMessage();
			} else {
				return "Invalid 'mType' parameter";
			}

			msg.setStringProperty("MyCustomStringProperty", "MyString");
			msg.setLongProperty("MyCustomLongProperty", 100L);

			logger.info("Sending to " + (destination instanceof Topic ? "Topic" : "Queue") + ":" + destinationName + "...");
			producer1.send(destination, msg);

			if (producer1.getAsync() != null) {
				return "Message sent asynchronously. Check logs...";
			} else {
				return msg.toString();
			}
		} catch (Exception e) {
			logger.warn("Failed publish.", e);
			return "Failed publish - see logs";
		}
	}

	@GetMapping("recv")
	String recv(@RequestParam(value = "dName", defaultValue = "DEV.QUEUE.2") String queueName) throws JMSException {
		Message msg = jmsTemplate.receive(queueName);
		String returnString = msg.toString();

		if (msg instanceof MapMessage mapMessage) {
			returnString += mapFieldsToString(mapMessage);
		} else if (msg instanceof StreamMessage streamMessage) {
			returnString += streamObjectsToString(streamMessage);
		}
		return returnString;
	}

	private String streamObjectsToString(StreamMessage streamMessage) throws JMSException {
		String output = "\n\n========Stream Objects========";
		try {
			while (true) {
				Object value = streamMessage.readObject();
				if (value instanceof byte[]) {
					output += "\n" + byteArrayToString((byte[]) value);
				} else {
					output += "\n" + value;
				}
			}
		} catch (MessageEOFException eof) {
			//ignore
		}
		output += "\n========Stream Objects========";
		return output;
	}

	private String mapFieldsToString(MapMessage mapMessage) throws JMSException {
		Enumeration<String> en = mapMessage.getMapNames();

		String output = "\n\n========Map Fields========";
		while(en.hasMoreElements()) {
			String key = en.nextElement();
			Object value = mapMessage.getObject(key);
			if (value instanceof byte[]) {
				output += "\n" + key + " = " + byteArrayToString((byte[]) value);
			} else {
				output += "\n" + key + " = " + value;
			}
		}
		output += "\n========Map Fields========";
		return output;
	}

	private String byteArrayToString(byte[] bytes) {
		String returnValue = "";
		for(int i = 0; i < bytes.length ; i++) {
			returnValue += bytes[i] + " ";
		}
		return returnValue;
	}

}
