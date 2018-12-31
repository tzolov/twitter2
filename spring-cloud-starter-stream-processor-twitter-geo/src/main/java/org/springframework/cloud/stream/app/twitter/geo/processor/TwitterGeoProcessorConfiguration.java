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

package org.springframework.cloud.stream.app.twitter.geo.processor;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.GeoQuery;
import twitter4j.Place;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;

/**
 * @author Christian Tzolov
 */
@EnableBinding(Processor.class)
@Import({ TwitterConnectionConfiguration.class, TwitterGeoProcessorFunctionConfiguration.class })
public class TwitterGeoProcessorConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterGeoProcessorConfiguration.class);

	@Bean
	public IntegrationFlow geoQueryFlow(Processor processor, Function<Message<?>, GeoQuery> toGeoQuery,
			Function<GeoQuery, List<Place>> places, Function<Object, Message<byte[]>> managedJson) {

		return IntegrationFlows
				.from(processor.input())
				.transform(Message.class, toGeoQuery.andThen(places).andThen(managedJson)::apply)
				.channel(processor.output()).get();
	}
}
