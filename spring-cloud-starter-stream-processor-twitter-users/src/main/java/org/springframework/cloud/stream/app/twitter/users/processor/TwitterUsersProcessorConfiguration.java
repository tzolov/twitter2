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

package org.springframework.cloud.stream.app.twitter.users.processor;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableBinding(Processor.class)
@EnableConfigurationProperties({ TwitterUsersProcessorProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterUsersProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterUsersProcessorConfiguration.class);

	@Autowired
	private TwitterUsersProcessorProperties properties;

	// Use the spring.cloud.stream.function.definition to override the default function composition.
	@Bean
	@Conditional(OnMissingStreamFunctionDefinitionCondition.class)
	public IntegrationFlow defaultProcessorFlow(Processor processor,
			Function<Message<?>, List<User>> querySearch, Function<Object, Message<byte[]>> json) {

		return IntegrationFlows
				.from(processor.input())
				.transform(Message.class, querySearch.andThen(json)::apply)
				.channel(processor.output())
				.get();
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.users.type", havingValue = "search")
	public Function<Message<?>, List<User>> userSearch(Twitter twitter,
			TwitterUsersProcessorProperties.Search search) {

		return message -> {
			String query = search.getQuery().getValue(message, String.class);
			try {
				ResponseList<User> users = twitter.searchUsers(query, search.getPage());
				return users;
			}
			catch (TwitterException e) {
				e.printStackTrace();
			}
			return null;
		};
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.users.type", havingValue = "lookup")
	public Function<Message<?>, List<User>> userLookup(Twitter twitter,
			TwitterUsersProcessorProperties.Lookup lookup) {

		return message -> {

			try {
				if (lookup.getScreenName() != null) {
					String[] screenNames = lookup.getScreenName().getValue(message, String[].class);
					return twitter.lookupUsers(screenNames);
				}
				else if (lookup.getScreenName() != null) {
					long[] ids = lookup.getUserId().getValue(message, long[].class);
					return twitter.lookupUsers(ids);
				}
			}
			catch (TwitterException e) {
				e.printStackTrace();
			}
			return null;
		};
	}

}
