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

package org.springframework.cloud.stream.app.twitter.stream.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.reactive.StreamEmitter;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties({ TwitterStreamSourceProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterStreamSourceConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterStreamSourceConfiguration.class);

	@Autowired
	private TwitterStream twitterStream;

	@Autowired
	private TwitterStreamSourceProperties streamProperties;

	private ObjectMapper objectMapper = new ObjectMapper();

	@StreamEmitter
	@Output(Source.OUTPUT)
	public Flux<Message<byte[]>> emit() {

		return Flux.create(emitter -> {

			//RawStreamListener rawListener = new RawStreamListener() {
			//	@Override
			//	public void onMessage(String json) {
			//		Message<byte[]> message = MessageBuilder
			//				.withPayload(json.getBytes())
			//				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
			//				.build();
			//		emitter.next(message);
			//
			//		//try {
			//		//	Status status = TwitterObjectFactory.createStatus(json);
			//		//	status.getPlace();
			//		//	System.out.println(status);
			//		//}
			//		//catch (TwitterException e) {
			//		//	e.printStackTrace();
			//		//}
			//		////System.out.println(json);
			//		//System.out.println();
			//		//System.out.println();
			//	}
			//
			//	@Override
			//	public void onException(Exception ex) {
			//		logger.error("Status Error: ", ex);
			//		emitter.error(new RuntimeException("Status Error: ", ex));
			//	}
			//};

			StatusListener listener = new StatusListener() {

				@Override
				public void onException(Exception e) {
					logger.error("Status Error: ", e);
					emitter.error(new RuntimeException("Status Error: ", e));
				}

				@Override
				public void onDeletionNotice(StatusDeletionNotice arg) {
					logger.info("StatusDeletionNotice: " + arg);
				}

				@Override
				public void onScrubGeo(long userId, long upToStatusId) {
					logger.info("onScrubGeo: " + userId + ", " + upToStatusId);
				}

				@Override
				public void onStallWarning(StallWarning warning) {
					logger.warn("Stall Warning: " + warning);
					emitter.error(new RuntimeException("Stall Warning: " + warning));
				}

				@Override
				public void onStatus(Status status) {

					try {
						String json = objectMapper.writeValueAsString(status);
						Message<byte[]> message = MessageBuilder
								.withPayload(json.getBytes())
								.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
								.build();
						emitter.next(message);

//						System.out.println(json);
					}
					catch (JsonProcessingException e) {
						logger.error("Status to JSON conversion error!", e);
						emitter.error(new RuntimeException("Status to JSON conversion error!", e));
					}
				}

				@Override
				public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
					logger.warn("Track Limitation Notice: " + numberOfLimitedStatuses);
				}
			};

			this.twitterStream.addListener(listener);

			try {
				startStreaming();
			}
			catch (Exception e) {
				this.logger.error("Filter is not property set");
				emitter.error(e);
			}

			emitter.onDispose(() -> {
				this.logger.info("Emitter cancellation, proactive cancel for twitter stream");
				this.twitterStream.shutdown();
			});
		});
	}

	private TwitterStream startStreaming() {

		switch (this.streamProperties.getType()) {

		case filter: {
			if (!this.streamProperties.getFilter().isValid()) {
				throw new RuntimeException("Filter is not property set");
			}
			return this.twitterStream.filter(this.streamProperties.getFilter().toFilterQuery());
		}
		case sample: {
			return this.twitterStream.sample();
		}
		case firehose: {
			return this.twitterStream.firehose(this.streamProperties.getFilter().getCount());
		}
		case linkn: {
			return this.twitterStream.links(this.streamProperties.getFilter().getCount());
		}

		}
		throw new IllegalArgumentException("Unknown stream type:" + this.streamProperties.getType());
	}
}
