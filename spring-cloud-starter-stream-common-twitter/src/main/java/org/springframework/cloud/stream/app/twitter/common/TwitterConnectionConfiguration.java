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

package org.springframework.cloud.stream.app.twitter.common;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ TwitterConnectionProperties.class })
public class TwitterConnectionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterConnectionConfiguration.class);

	@Bean
	public twitter4j.conf.Configuration twitterConfiguration(TwitterConnectionProperties properties,
			Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder) {
		return toConfigurationBuilder.apply(properties).build();
	}

	@Bean
	public Twitter twitter(twitter4j.conf.Configuration configuration) {
		return new TwitterFactory(configuration).getInstance();
	}

	@Bean
	public TwitterStream twitterStream(twitter4j.conf.Configuration configuration) {
		return new TwitterStreamFactory(configuration).getInstance();
	}

	@Bean
	public Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder() {
		return properties -> new ConfigurationBuilder()
				.setJSONStoreEnabled(true)
				.setDebugEnabled(properties.isDebugEnabled())
				.setOAuthConsumerKey(properties.getConsumerKey())
				.setOAuthConsumerSecret(properties.getConsumerSecret())
				.setOAuthAccessToken(properties.getAccessToken())
				.setOAuthAccessTokenSecret(properties.getAccessTokenSecret());
	}

	@Bean
	public Function<Message<?>, Message<?>> normalizeStringPayload() {

		return message -> {
			if (message.getPayload() instanceof byte[]) {
				String contentType = message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)
						? message.getHeaders().get(MessageHeaders.CONTENT_TYPE).toString()
						: BindingProperties.DEFAULT_CONTENT_TYPE.toString();
				if (contentType.contains("text") || contentType.contains("json") || contentType.contains("x-spring-tuple")) {
					message = new MutableMessage<>(new String(((byte[]) message.getPayload())), message.getHeaders());
				}
			}

			return message;
		};
	}
}