/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.twitter.message.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.api.DirectMessagesResources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionProperties;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.reactive.StreamEmitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties({ TwitterMessageSourceProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterMessageSourceConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterMessageSourceConfiguration.class);

	@Autowired
	private DirectMessagesResources directMessages;

	@Autowired
	private TwitterMessageSourceProperties messageProperties;

	@Autowired
	private TwitterConnectionProperties connectionProperties;

	private ObjectMapper objectMapper = new ObjectMapper();

	private String cursor = null;

	private int count = -1;

	@StreamEmitter
	@Output(Source.OUTPUT)
	public Flux<Message<byte[]>> emit() {
		return Flux.interval(this.connectionProperties.getPollInterval())
				.map(l -> {
					try {
						if (count < 0) {
							count = this.messageProperties.getCount();
						}

						DirectMessageList messages = (this.cursor == null) ?
								this.directMessages.getDirectMessages(count) :
								this.directMessages.getDirectMessages(count, this.cursor);

						//for (DirectMessage message : messages) {
						//	System.out.println("From: " + message.getSenderId() + " id:" + message.getId()
						//			+ " [" + message.getCreatedAt() + "]"
						//			+ " - " + message.getText());
						//
						//	System.out.println("raw[" + message + "]");
						//}
						this.cursor = messages.getNextCursor();

						if (CollectionUtils.isEmpty(messages)) {
							logger.info("No new messages");
							if (this.cursor == null) {
								this.count = 0;
							}
							System.out.println(" EMPTY RESPONSE");
							return null;
						}

						List<String> rawJsonList = new ArrayList<>();
						for (DirectMessage message : messages) {
							rawJsonList.add(TwitterObjectFactory.getRawJSON(message));
						}
						String rawJson = this.objectMapper.writeValueAsString(rawJsonList);

						System.out.println(rawJson);

						return MessageBuilder
								.withPayload(rawJson.getBytes())
								.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
								.build();
					}
					catch (TwitterException te) {
						logger.error(te.getErrorMessage() + ", code: " + te.getErrorCode(), te);
					}
					catch (JsonProcessingException e) {
						logger.error("Json issue", e);
					}

					return null;
				});
	}

	//@InboundChannelAdapter(value = Source.OUTPUT, poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1"))
	//public MessageSource<byte[]> retrieveDirectMessages() {
	//
	//	try {
	//		if (count < 0) {
	//			count = this.messageProperties.getCount();
	//		}
	//
	//		DirectMessageList messages = (this.cursor == null) ?
	//				this.directMessages.getDirectMessages(count) :
	//				this.directMessages.getDirectMessages(count, this.cursor);
	//
	//		this.directMessages.getDirectMessages()
	//		//for (DirectMessage message : messages) {
	//		//	System.out.println("From: " + message.getSenderId() + " id:" + message.getId()
	//		//			+ " [" + message.getCreatedAt() + "]"
	//		//			+ " - " + message.getText());
	//		//
	//		//	System.out.println("raw[" + message + "]");
	//		//}
	//		this.cursor = messages.getNextCursor();
	//
	//		if (CollectionUtils.isEmpty(messages)) {
	//			logger.info("No new messages");
	//			if (this.cursor == null) {
	//				this.count = 0;
	//			}
	//			System.out.println(" EMPTY RESPONSE");
	//			return null;
	//		}
	//
	//		List<String> rawJsonList = new ArrayList<>();
	//		for (DirectMessage message : messages) {
	//			rawJsonList.add(TwitterObjectFactory.getRawJSON(message));
	//		}
	//		String rawJson = this.objectMapper.writeValueAsString(rawJsonList);
	//
	//		System.out.println(rawJson);
	//
	//		Message<byte[]> siMessage = MessageBuilder
	//				.withPayload(rawJson.getBytes())
	//				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
	//				.build();
	//
	//		return () -> siMessage;
	//	}
	//	catch (TwitterException te) {
	//		logger.error(te.getErrorMessage() + ", code: " + te.getErrorCode(), te);
	//	}
	//	catch (JsonProcessingException e) {
	//		logger.error("Json issue", e);
	//	}
	//
	//	return null;
	//}

	@Bean
	public DirectMessagesResources directMessage(Twitter twitter) {
		return twitter.directMessages();
	}
}
