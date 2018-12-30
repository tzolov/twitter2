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
import twitter4j.Location;

import org.springframework.beans.factory.annotation.Autowired;
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
 * Functional Processor:
 * http://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.1.0.RC3/single/spring-cloud-stream.html#_spring_cloud_function
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableBinding(Processor.class)
@Import({ TwitterConnectionConfiguration.class, TwitterTrendLocationProcessorFunctionConfiguration.class })
public class TwitterTrendLocationProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterTrendLocationProcessorConfiguration.class);

	@Autowired
	private Function<Message<?>, List<Location>> closestOrAvailableTrends;

	@Autowired
	private Function<Object, Message<byte[]>> managedJson;

	// Use the spring.cloud.stream.function.definition to override the default function composition.
	@Bean
	@Conditional(OnMissingStreamFunctionDefinitionCondition.class)
	public IntegrationFlow defaultProcessorFlow(Processor processor) {
		return IntegrationFlows
				.from(processor.input())
				.transform(Message.class, closestOrAvailableTrends.andThen(managedJson)::apply)
				.channel(processor.output())
				.get();
	}
}
