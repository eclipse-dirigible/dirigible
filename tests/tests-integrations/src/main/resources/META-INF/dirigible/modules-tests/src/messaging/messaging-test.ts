import { test, assertEquals } from "sdk/junit"
import { producer } from "sdk/messaging"
import { consumer } from "sdk/messaging"

const MessageProducer = Java.type("org.eclipse.dirigible.integration.tests.api.java.messaging.MessageProducer");

test('send-receive-to-from-queue-test', () => {
	const message = "'This is a test message'";
	const queueName = "messaging-test-queue";
	producer.queue(queueName).send(message);

	const receivedMessage = consumer.queue(queueName).receive(1000);

	assertEquals("Received an unexpected message from queue " + queueName, message, receivedMessage);
});

test('send-receive-to-from-topic-test', () => {
	const topicName = "messaging-test-topic";
	const message = "'This is a test message'";
	MessageProducer.asyncSendMessageToTopic(topicName, message);

	const receivedMessage = consumer.topic(topicName).receive(4000);

	assertEquals("Received an unexpected message from topic " + topicName, message, receivedMessage);
});
