package com.carol.mqspring;

import com.carol.MySerializableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Enumeration;

@SpringBootApplication
@RestController
public class MqspringApplication {

	private static Logger logger = LoggerFactory.getLogger(MqspringApplication.class);

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private JMSContext jmsContext;

	private JMSProducer producer1;

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

	@PostMapping("performance")
	String spoolMessages(@RequestParam(value = "dName", defaultValue = "DEV.QUEUE.1") String queueName,
						 @RequestParam(value = "numMsgs", defaultValue = "10000") int numMessages,
						 @RequestParam(value = "numBytes", defaultValue = "1024") int numBytes) throws JMSException {

		logger.info(String.format("Spooling [ %d ] messages of roughly [ %d ] bytes on queue [ %s ]",
				numMessages, numBytes, queueName));

		byte[] bytes = new byte[numBytes];
		for (int i = 0; i < numBytes; i++) {
			bytes[i] = (byte) 'A';
		}

		Connection connection = connectionFactory.createConnection();
		try {
			Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE); //Doesn't matter for producer only sessions
			MessageProducer producer = session.createProducer(session.createQueue(queueName));

			BytesMessage bytesMessage = session.createBytesMessage();
			bytesMessage.writeBytes(bytes);

			long startTime = System.currentTimeMillis();
			for (int i = 0; i < numMessages; i++) {
				producer.send(bytesMessage);
			}
			long endTime = System.currentTimeMillis();

			Duration duration = Duration.ofMillis(endTime - startTime);
			long msgPerSeconds = computeMsgPerSeconds(numMessages, duration);

			logger.info("Spooling COMPLETED");
			return String.format("Sent [ %d ] messages of roughly [ %d ] bytes to queue [ %s ] in [ %d ] seconds\n[ %d msg/sec ]\n",
					numMessages, numBytes, queueName, duration.toSeconds(), msgPerSeconds);
		} finally {
			connection.close();
		}

	}

	@GetMapping("performance")
	String testConsumerPerformance(@RequestParam(value = "dName", defaultValue = "DEV.QUEUE.1") String queueName,
								   @RequestParam(value = "numMsgs", defaultValue = "10000") int numMessages,
								   @RequestParam(value = "transacted", defaultValue = "true") boolean transacted) throws JMSException {


		Connection connection = connectionFactory.createConnection();
		Session session = connection.createSession(transacted ? Session.SESSION_TRANSACTED : Session.CLIENT_ACKNOWLEDGE);
		MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
		connection.start();

		long startTime = System.currentTimeMillis();
		logger.info(String.format("Starting synchronous consuming performance test: numMessages [ %d ], queue [ %s ], transacted [ %s ]",
				numMessages, queueName, transacted));
		try {
			int numReceived = 0;
			while (numReceived < numMessages) {
				Message msg = consumer.receive(); // Blocks until msg is received
				if (msg != null) {
					if (transacted) {
						session.commit();
					} else {
						msg.acknowledge();
					}
				}
				numReceived++;
			}
			long endTime = System.currentTimeMillis();
			Duration duration = Duration.ofMillis(endTime - startTime);
			long msgPerSeconds = computeMsgPerSeconds(numReceived, duration);

			String resultString = String.format("Completed performance test: consumed [ %d ] messages in [ %d ] seconds with [ %s ] session\n[ %d msg/sec ]\n",
					numReceived, duration.toSeconds(), transacted ? "transacted" : "non-transacted", msgPerSeconds);

			logger.info(resultString);
			return resultString;

		} finally {
			connection.close();
		}
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

	private long computeMsgPerSeconds(int numMessages, Duration duration) {
		return Math.floorDiv(numMessages, duration.toSeconds());
	}

}
