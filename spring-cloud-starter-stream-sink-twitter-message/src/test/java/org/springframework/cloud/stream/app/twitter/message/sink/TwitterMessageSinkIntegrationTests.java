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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.StringBody;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.test.twitter.TwitterTestUtils;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionProperties;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockserver.matchers.Times.unlimited;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"twitter.connection.consumerKey=consumerKey666",
				"twitter.connection.consumerSecret=consumerSecret666",
				"twitter.connection.accessToken=accessToken666",
				"twitter.connection.accessTokenSecret=accessTokenSecret666"
		})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TwitterMessageSinkIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@Autowired
	protected Sink sink;

	@BeforeClass
	public static void startMockServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/users/show.json")
								.withQueryStringParameter("screen_name", "user666")
								.withQueryStringParameter("include_entities", "true")
								.withQueryStringParameter("include_ext_alt_text", "true")
								.withQueryStringParameter("tweet_mode", "extended"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/user_666.json"))
								.withDelay(TimeUnit.SECONDS, 1));

		mockClient
				.when(
						request()
								.withMethod("POST")
								.withPath("/direct_messages/events/new.json"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/update_test_1.json"))
								.withDelay(TimeUnit.SECONDS, 1));
	}

	@AfterClass
	public static void stopMockServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.message.update.screenName='user666'"
	})
	public static class TwitterDirectMessageByScreenNameTests extends TwitterMessageSinkIntegrationTests {

		@Test
		public void directMessageScreenName() {

			sink.input().send(new GenericMessage("hello"));

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/users/show.json")
							.withQueryStringParameter("screen_name", "user666")
							.withQueryStringParameter("include_entities", "true")
							.withQueryStringParameter("include_ext_alt_text", "true")
							.withQueryStringParameter("tweet_mode", "extended"),
					once());

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\"," +
									"\"message_create\":{\"target\":{\"recipient_id\":1075751718749659136}," +
									"\"message_data\":{\"text\":\"hello\"}}}}")),
					once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.message.update.userId='1075751718749659136'"
	})
	public static class TwitterDirectMessageByUserIdTests extends TwitterMessageSinkIntegrationTests {

		@Test
		public void directMessageUserId() {

			sink.input().send(new GenericMessage("hello"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\"," +
									"\"message_create\":{\"target\":{\"recipient_id\":1075751718749659136}," +
									"\"message_data\":{\"text\":\"hello\"}}}}")),
					once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.message.update.userId=headers['user']",
			"twitter.message.update.text=payload.concat(\" with suffix \")",
			"twitter.message.update.mediaId='666'"
	})
	public static class TwitterMessageWithExpressionsTests extends TwitterMessageSinkIntegrationTests {

		@Test
		public void directMessageDefaults() {

			Map<String, String> headers = Collections.singletonMap("user", "1075751718749659136");
			sink.input().send(new GenericMessage("hello", headers));
			
			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\",\"message_create\":" +
									"{\"target\":{\"recipient_id\":1075751718749659136},\"message_data\":" +
									"{\"text\":\"hello with suffix \",\"attachment\":{\"type\":\"media\",\"media\":" +
									"{\"id\":666}}}}}}")),
					once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterMessageSinkConfiguration.class)
	public static class TestTwitterMessageSinkApplication {

		@Bean
		@Primary
		public twitter4j.conf.Configuration twitterConfiguration2(TwitterConnectionProperties properties,
				Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder) {

			Function<TwitterConnectionProperties, ConfigurationBuilder> mockedConfiguration =
					toConfigurationBuilder.andThen(
							new TwitterTestUtils().mockTwitterUrls(
									String.format("http://%s:%s", MOCK_SERVER_IP, MOCK_SERVER_PORT)));

			return mockedConfiguration.apply(properties).build();
		}
	}
}
