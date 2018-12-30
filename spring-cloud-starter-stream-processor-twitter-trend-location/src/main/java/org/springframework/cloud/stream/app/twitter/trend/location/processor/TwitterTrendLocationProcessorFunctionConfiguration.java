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

package org.springframework.cloud.stream.app.twitter.trend.location.processor;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.GeoLocation;
import twitter4j.Location;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;


/**
 * Functional Processor:
 * http://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.1.0.RC3/single/spring-cloud-stream.html#_spring_cloud_function
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ TwitterTrendLocationProcessorProperties.class })
public class TwitterTrendLocationProcessorFunctionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterTrendLocationProcessorFunctionConfiguration.class);

	@Bean
	public Function<Message<?>, List<Location>> closestOrAvailableTrends(
			TwitterTrendLocationProcessorProperties properties, Twitter twitter) {
		return message -> {
			try {
				if (properties.getClosest().getLat() != null
						&& properties.getClosest().getLon() != null) {
					double lat = properties.getClosest().getLat().getValue(message, double.class);
					double lon = properties.getClosest().getLon().getValue(message, double.class);

					return twitter.getClosestTrends(new GeoLocation(lat, lon));
				}
				else {
					return twitter.getAvailableTrends();
				}
			}
			catch (TwitterException e) {
				logger.error("Twitter API error!", e);
			}
			return null;
		};
	}
}
