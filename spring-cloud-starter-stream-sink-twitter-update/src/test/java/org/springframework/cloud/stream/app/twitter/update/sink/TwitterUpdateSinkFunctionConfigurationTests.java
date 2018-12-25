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

import org.junit.Test;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Christian Tzolov
 */
public class TwitterUpdateSinkFunctionConfigurationTests {

	@Test
	public void testStatusUpdateConsumer() throws TwitterException {
		Twitter twitter = mock(Twitter.class);

		Consumer<StatusUpdate> statusUpdateConsumer =
				new TwitterUpdateSinkFunctionConfiguration().updateStatus(twitter);

		StatusUpdate statusUpdateQuery = new StatusUpdate("Hello World");
		statusUpdateConsumer.accept(statusUpdateQuery);
		verify(twitter).updateStatus(eq(statusUpdateQuery));
	}

	@Test
	public void testToStatusUpdateQueryFunction() {
		TwitterUpdateSinkProperties properties = new TwitterUpdateSinkProperties();

		properties.setAttachmentUrl(expression("'attachmentUrl'"));
		properties.setPlaceId(expression("'myPlaceId'"));
		properties.setInReplyToStatusId(expression("'666666'"));
		properties.setDisplayCoordinates(expression("'true'"));
		properties.setMediaIds(expression("'471592142565957632, 471592142565957633'"));
		properties.getLocation().setLat(expression("'37.78217'"));
		properties.getLocation().setLon(expression("'-122.40062'"));

		Function<Message<?>, StatusUpdate> toStatusUpdateQueryFunction =
				new TwitterUpdateSinkFunctionConfiguration().toStatusUpdateQuery(properties);

		StatusUpdate result = toStatusUpdateQueryFunction.apply(new GenericMessage<>("Hello World"));

		assertThat(result, notNullValue());
		assertThat(result.getStatus(), is("Hello World"));
		assertThat(result.getAttachmentUrl(), is("attachmentUrl"));
		assertThat(result.getPlaceId(), is("myPlaceId"));
		assertThat(result.getInReplyToStatusId(), is(666666L));
		assertThat(result.isDisplayCoordinates(), is(true));
		assertThat(result.getLocation().getLatitude(), is(37.78217));
		assertThat(result.getLocation().getLongitude(), is(-122.40062));
	}

	private Expression expression(String expressionString) {
		ExpressionParser parser = new SpelExpressionParser();
		return parser.parseExpression(expressionString);
	}
}
