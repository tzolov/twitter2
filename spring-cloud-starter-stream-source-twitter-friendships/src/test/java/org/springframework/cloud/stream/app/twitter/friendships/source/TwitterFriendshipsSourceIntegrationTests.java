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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.test.twitter.TwitterTestUtils;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionProperties;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
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
public abstract class TwitterFriendshipsSourceIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@Autowired
	protected Source channels;

	@Autowired
	protected MessageCollector messageCollector;

	@BeforeClass
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/followers/list.json")
								.withQueryStringParameter("screen_name", "christzolov")
								.withQueryStringParameter("cursor", "-1")
								.withQueryStringParameter("count", "200")
								.withQueryStringParameter("skip_status", "false")
								.withQueryStringParameter("include_user_entities", "true")
								.withQueryStringParameter("include_entities", "true")
								.withQueryStringParameter("include_ext_alt_text", "true")
								.withQueryStringParameter("tweet_mode", "extended"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/followers.json"))
								.withDelay(TimeUnit.SECONDS, 1));

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/friends/list.json")
								.withQueryStringParameter("screen_name", "christzolov")
								.withQueryStringParameter("cursor", "-1")
								.withQueryStringParameter("count", "200")
								.withQueryStringParameter("skip_status", "false")
								.withQueryStringParameter("include_user_entities", "true")
								.withQueryStringParameter("include_entities", "true")
								.withQueryStringParameter("include_ext_alt_text", "true")
								.withQueryStringParameter("tweet_mode", "extended"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/friends.json"))
								.withDelay(TimeUnit.SECONDS, 1));

	}

	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}


	@TestPropertySource(properties = {
			"twitter.friendships.source.type=followers",
			"twitter.friendships.source.screenName=christzolov",
			"twitter.friendships.source.poll-interval=1000"
	})
	public static class TwitterFollowersTests extends TwitterFriendshipsSourceIntegrationTests {

		@Test
		public void testOne() throws InterruptedException {

			Message<?> received = messageCollector.forChannel(this.channels.output()).poll(60 * 4, TimeUnit.SECONDS);

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/followers/list.json")
							.withQueryStringParameter("screen_name", "christzolov")
							.withQueryStringParameter("cursor", "-1")
							.withQueryStringParameter("count", "200")
							.withQueryStringParameter("skip_status", "false")
							.withQueryStringParameter("include_user_entities", "true")
							.withQueryStringParameter("include_entities", "true")
							.withQueryStringParameter("include_ext_alt_text", "true")
							.withQueryStringParameter("tweet_mode", "extended"),
					once());

			Assert.assertNotNull(received);
		}
	}

	@TestPropertySource(properties = {
			"twitter.friendships.source.type=friends",
			"twitter.friendships.source.screenName=christzolov"
	})
	public static class TwitterFriendsTests extends TwitterFriendshipsSourceIntegrationTests {

		@Test
		public void testOne() throws InterruptedException {

			Message<?> received = messageCollector.forChannel(this.channels.output()).poll(60 * 4, TimeUnit.SECONDS);

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/friends/list.json")
							.withQueryStringParameter("screen_name", "christzolov")
							.withQueryStringParameter("cursor", "-1")
							.withQueryStringParameter("count", "200")
							.withQueryStringParameter("skip_status", "false")
							.withQueryStringParameter("include_user_entities", "true")
							.withQueryStringParameter("include_entities", "true")
							.withQueryStringParameter("include_ext_alt_text", "true")
							.withQueryStringParameter("tweet_mode", "extended"),
					once());

			Assert.assertNotNull(received);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterFriendshipsSourceConfiguration.class)
	public static class TestTwitterFriendshipsSourceApplication {

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
