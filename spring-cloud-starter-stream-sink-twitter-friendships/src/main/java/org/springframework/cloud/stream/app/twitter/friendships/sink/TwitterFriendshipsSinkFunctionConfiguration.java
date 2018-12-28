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

package org.springframework.cloud.stream.app.twitter.friendships.sink;

import java.util.function.Consumer;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ TwitterFriendshipsSinkProperties.class })
public class TwitterFriendshipsSinkFunctionConfiguration {

	@Bean
	@SuppressWarnings("Duplicates")
	public Consumer<Message<?>> friendship(TwitterFriendshipsSinkProperties properties, Twitter twitter) {

		return message -> {
			try {
				TwitterFriendshipsSinkProperties.OperationType type = properties.getType().getValue(message, TwitterFriendshipsSinkProperties.OperationType.class);
				//TwitterFriendshipsSinkProperties.OperationType type = TwitterFriendshipsSinkProperties.OperationType.create;
				if (properties.getUserId() != null) {
					Long userId = properties.getUserId().getValue(message, long.class);
					switch (type) {
					case create: {
						boolean follow = properties.getCreate().getFollow().getValue(message, boolean.class);
						twitter.createFriendship(userId, follow);
						break;
					}
					case update: {
						boolean enableDeviceNotification = properties.getUpdate().getDevice().getValue(message, boolean.class);
						boolean retweets = properties.getUpdate().getRetweets().getValue(message, boolean.class);
						twitter.updateFriendship(userId, enableDeviceNotification, retweets);
						break;
					}
					case destroy: {
						twitter.destroyFriendship(userId);
						break;
					}
					}
				}
				else if (properties.getScreenName() != null) {
					String screenName = properties.getScreenName().getValue(message, String.class);
					switch (type) {
					case create: {
						boolean follow = properties.getCreate().getFollow().getValue(message, boolean.class);
						twitter.createFriendship(screenName, follow);
						break;
					}
					case update: {
						boolean enableDeviceNotification = properties.getUpdate().getDevice().getValue(message, boolean.class);
						boolean retweets = properties.getUpdate().getRetweets().getValue(message, boolean.class);
						twitter.updateFriendship(screenName, enableDeviceNotification, retweets);
						break;
					}
					case destroy: {
						twitter.destroyFriendship(screenName);
						break;
					}
					}
				}
				else {
					throw new IllegalStateException("Either ScreenName or UserID must be set");
				}
			}
			catch (TwitterException te) {
				throw new IllegalStateException("Twitter API error!", te);
			}
		};
	}
}
