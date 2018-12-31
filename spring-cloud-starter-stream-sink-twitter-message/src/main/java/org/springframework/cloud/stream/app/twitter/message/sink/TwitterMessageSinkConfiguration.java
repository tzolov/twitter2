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

package org.springframework.cloud.stream.app.twitter.message.sink;

import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.OnMissingStreamFunctionDefinitionCondition;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@EnableBinding(Sink.class)
@Import({ TwitterMessageSinkFunctionConfiguration.class, TwitterConnectionConfiguration.class })
public class TwitterMessageSinkConfiguration {

	@Autowired
	private Consumer<Message<?>> sendDirectMessage;

	@Autowired
	private Function<Message<?>, Message<?>> normalizeStringPayload;

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Conditional(OnMissingStreamFunctionDefinitionCondition.class)
	public void handle(Message<?> message) {
		sendDirectMessage.accept(normalizeStringPayload.apply(message));
	}

}
