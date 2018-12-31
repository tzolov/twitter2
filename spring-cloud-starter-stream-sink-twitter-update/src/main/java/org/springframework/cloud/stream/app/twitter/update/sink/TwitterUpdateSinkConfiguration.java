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

package org.springframework.cloud.stream.app.twitter.update.sink;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.StatusUpdate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.OnMissingStreamFunctionDefinitionCondition;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@EnableBinding(Sink.class)
@EnableConfigurationProperties({ TwitterUpdateSinkProperties.class })
@Import({ TwitterConnectionConfiguration.class, TwitterUpdateSinkFunctionConfiguration.class })
public class TwitterUpdateSinkConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterUpdateSinkConfiguration.class);

	// Default function DSL, that is equivalent to: stringifyPayload|toStatusUpdateQuery|updateStatus
	// Set the spring.cloud.stream.function.definition to override the default composition.
	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnMissingStreamFunctionDefinitionCondition.class)
	public IntegrationFlow statusUpdateFlow(Sink sink, Function<Message<?>, Message<?>> stringifyPayload,
			Function<Message<?>, StatusUpdate> statusUpdateQuery, Consumer<StatusUpdate> updateStatus) {

		return IntegrationFlows
				.from(sink.input())
				.transform(Message.class, stringifyPayload.andThen(statusUpdateQuery)::apply)
				.handle(updateStatus)
				.get();
	}

}
