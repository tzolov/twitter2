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

package org.springframework.cloud.stream.app.twitter.search.processor;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.OnMissingStreamFunctionDefinitionCondition;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;


/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableBinding(Processor.class)
@EnableConfigurationProperties({ TwitterSearchProcessorProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterSearchProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterSearchProcessorConfiguration.class);

	// Use the spring.cloud.stream.function.definition to override the default function composition.
	@Bean
	@Conditional(OnMissingStreamFunctionDefinitionCondition.class)
	public IntegrationFlow defaultProcessorFlow(Processor processor,
			Function<Message<?>, Query> query, Function<Query, List<Status>> search,
			Function<List<Status>, Message<byte[]>> json) {
		return IntegrationFlows
				.from(processor.input())
				.transform(Message.class, query.andThen(search).andThen(json)::apply)
				.channel(processor.output())
				.get();
	}

	@Bean
	public Function<Query, List<Status>> search(Twitter twitter) {
		return query -> {
			try {
				return twitter.search(query).getTweets();
			}
			catch (TwitterException e) {
				logger.error("Twitter error", e);
			}
			return null;
		};
	}

	@Bean
	public Function<Message<?>, Query> query(TwitterSearchProcessorProperties searchProperties) {
		return message -> {
			Query query = new Query();

			query.setQuery(searchProperties.getQuery().getValue(message, String.class));

			if (searchProperties.getCount() != null) {
				query.count(searchProperties.getCount().getValue(message, int.class));
			}
			if (searchProperties.getMaxId() != null) {
				query.maxId(searchProperties.getMaxId().getValue(message, long.class));
			}
			if (searchProperties.getSinceId() != null) {
				query.sinceId(searchProperties.getSinceId().getValue(message, long.class));
			}
			if (searchProperties.getLang() != null) {
				query.setLang(searchProperties.getLang().getValue(message, String.class));
			}
			if (searchProperties.getSince() != null) {
				query.setSince(searchProperties.getSince().getValue(message, String.class));
			}

			if (searchProperties.getGeocode().getLatitude() != null
					&& searchProperties.getGeocode().getLatitude() != null
					&& searchProperties.getGeocode().getRadius() != null) {

				query.setGeoCode(
						new GeoLocation(
								searchProperties.getGeocode().getLatitude().getValue(message, double.class),
								searchProperties.getGeocode().getLongitude().getValue(message, double.class)),
						searchProperties.getGeocode().getRadius().getValue(message, double.class),
						Query.KILOMETERS);
			}

			if (searchProperties.getResultType() != Query.ResultType.mixed) {
				query.setResultType(searchProperties.getResultType());
			}

			return query;
		};
	}
}
