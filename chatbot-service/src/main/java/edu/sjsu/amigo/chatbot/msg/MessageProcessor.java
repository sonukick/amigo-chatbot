/*
 * Copyright (c) 2017 San Jose State University.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package edu.sjsu.amigo.chatbot.msg;

import edu.sjsu.amigo.chatbot.api.AIClient;
import edu.sjsu.amigo.chatbot.nlp.WitDotAIClient;
import edu.sjsu.amigo.json.util.JsonUtils;
import edu.sjsu.amigo.mp.kafka.MessageProducer;
import edu.sjsu.amigo.mp.model.Message;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rwatsh on 3/26/17.
 */
public class MessageProcessor {
    private static Logger logger = Logger.getLogger(MessageProducer.class.getName());

    public static void processMessage(Message msg) {
        // Get the intent from wit.ai and attach it to the message.
        /*
            Intent will be a sorted list of tokens.
            intent: [aws, ec2, list]
         */
        List<String> intent = null;
        //intent = HttpClient.getIntentFromWitAI(parsedMessage);
        AIClient aiClient = new WitDotAIClient();
        intent = aiClient.getIntent(msg.getContent());
        msg.setIntent(intent);
        // Generate a request ID and attach it to message
        msg.setRequestId(UUID.randomUUID().toString());

        if (msg != null) {
            String msgJson = null;

            try {
                msgJson = JsonUtils.convertObjectToJson(msg);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error in converting message to JSON", e);
            }

            if (msgJson != null) {
                // Send message to topic on kafka MQ
                logger.info("Sending message as JSON: " + msgJson);
                try (MessageProducer producer = new MessageProducer(System.getenv("KAFKA_HOST_NAME"))) {
                    producer.sendUserMessage(msgJson);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error in publishing message to queue", e);
                }
            }
        }
    }
}
