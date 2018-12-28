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

package org.springframework.cloud.stream.app.twitter.friendships.sink;

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

				//"twitter.connection.consumerKey=UK7rAyFvEayBIvcL52p07x0KT",
				//"twitter.connection.consumerSecret=QNYo29JRWgWhNoq0m6i0Bxt0DzeSi46eedZuFqEROnZdG8nlRs",
				//"twitter.connection.accessToken=1073883577107038208-TG4AUFm2EgpzRFDdK0PeWsBKplvoqT",
				//"twitter.connection.accessTokenSecret=Xnh2yjoLvSTUl4dbDXzRDDpWo5KT7QsK3jYb1Y1RXWakq"
		})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TwitterFriendshipsSinkIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@Autowired
	protected Sink sink;

	@BeforeClass
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		mockClient
				.when(
						request()
								.withMethod("POST")
								.withPath("/friendships/create.json")
								.withQueryStringParameter("user_id", "666")
								.withQueryStringParameter("follow", "true"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/relationship.json"))
								.withDelay(TimeUnit.SECONDS, 1));

		mockClient
				.when(
						request()
								.withMethod("POST")
								.withPath("/friendships/destroy.json")
								.withQueryStringParameter("user_id", "666"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/relationship.json"))
								.withDelay(TimeUnit.SECONDS, 1));

		mockClient
				.when(
						request()
								.withMethod("POST")
								.withPath("/friendships/update.json")
								.withBody(new StringBody("screen_name=christzolov" +
										"&device=true" +
										"&retweets=true" +
										"&include_entities=true" +
										"&include_ext_alt_text=true" +
										"&tweet_mode=extended")),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/relationship.json"))
								.withDelay(TimeUnit.SECONDS, 1));

	}

	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.friendships.sink.type='create'",
			"twitter.friendships.sink.userId='666'"
	})
	public static class TwitterCreateFriendshipsTests extends TwitterFriendshipsSinkIntegrationTests {

		@Test
		public void testOne() {

			sink.input().send(new GenericMessage("hello"));
			// Assert.assertNotNull(.. target resources ..);
			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/friendships/create.json")
							.withQueryStringParameter("user_id", "666")
							.withQueryStringParameter("follow", "true"),
					once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.friendships.sink.type='update'",
			"twitter.friendships.sink.screenName='christzolov'",
			"twitter.friendships.sink.update.device='true'",
			"twitter.friendships.sink.update.retweets='true'",
	})
	public static class TwitterUpdateFriendshipsTests extends TwitterFriendshipsSinkIntegrationTests {

		@Test
		public void testOne() {

			sink.input().send(new GenericMessage("hello"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/friendships/update.json")
							.withBody(new StringBody("screen_name=christzolov" +
									"&device=true" +
									"&retweets=true" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.friendships.sink.type='destroy'",
			"twitter.friendships.sink.userId='666'"
	})
	public static class TwitterDestroyFriendshipsTests extends TwitterFriendshipsSinkIntegrationTests {

		@Test
		public void testOne() {

			sink.input().send(new GenericMessage("hello"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/friendships/destroy.json")
							.withQueryStringParameter("user_id", "666"),
					once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterFriendshipsSinkConfiguration.class)
	public static class TestTwitterFriendshipsSinkApplication {

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
