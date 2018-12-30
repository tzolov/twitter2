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

package org.springframework.cloud.stream.app.twitter.trend.processor;

import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ TwitterTrendProcessorProperties.class })
public class TwitterTrendProcessorFunctionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterTrendProcessorFunctionConfiguration.class);

	@Bean
	public Function<Message<?>, Trends> trend(TwitterTrendProcessorProperties properties, Twitter twitter) {
		return message -> {
			try {
				int woeid = properties.getLocationId().getValue(message, int.class);
				return twitter.getPlaceTrends(woeid);
			}
			catch (TwitterException e) {
				logger.error("Twitter API error!", e);
			}
			return null;
		};
	}
}
