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

package org.springframework.cloud.stream.app.twitter.friendships.source;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import twitter4j.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@EnableBinding(Source.class)
@Import({ TwitterConnectionConfiguration.class, TwitterFriendshipsSourceFunctionConfiguration.class })
public class TwitterFriendshipsSourceConfiguration {

	@Autowired
	private Supplier<List<User>> userRetriever;

	@Autowired
	private Function<List<User>, List<User>> userDeduplication;

	@Autowired
	private Function<Object, Message<byte[]>> managedJson;

	@InboundChannelAdapter(value = Source.OUTPUT,
			poller = @Poller(fixedDelay = "${twitter.friendships.source.poll-interval:121000}", maxMessagesPerPoll = "1"))
	public Message<byte[]> userRetrieval() {
		return userDeduplication.andThen(managedJson).apply(userRetriever.get());
	}
}
