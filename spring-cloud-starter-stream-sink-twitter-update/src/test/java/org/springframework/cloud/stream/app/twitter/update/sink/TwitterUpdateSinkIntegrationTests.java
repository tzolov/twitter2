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
				"twitter.connection.accessTokenSecret=accessTokenSecret666",
		})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TwitterUpdateSinkIntegrationTests {

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
								.withMethod("POST")
								.withPath("/statuses/update.json"),
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

	public static class TwitterUpdatePayloadTests extends TwitterUpdateSinkIntegrationTests {

		@Test
		public void testUpdateStatus() {

			sink.input().send(new GenericMessage("Test Update 666"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=Test%20Update%20666" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.update.text=payload.toUpperCase().concat(\" With Suffix\")"
	})
	public static class TwitterUpdatePayloadExpressionTests extends TwitterUpdateSinkIntegrationTests {

		@Test
		public void updateWithPayloadExpression() {

			sink.input().send(new GenericMessage("1 Expression Test"));
			sink.input().send(new GenericMessage("2 Expression Test"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=1%20EXPRESSION%20TEST%20With%20Suffix" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=2%20EXPRESSION%20TEST%20With%20Suffix" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());
		}
	}


	@TestPropertySource(properties = {
			"twitter.update.attachmentUrl='http://attachementUrl'",
			"twitter.update.placeId='myPlaceId'",
			"twitter.update.inReplyToStatusId='666666'",
			"twitter.update.displayCoordinates='true'",
			"twitter.update.mediaIds='471592142565957632, 471592142565957633'",
			"twitter.update.location.lat='37.78217'",
			"twitter.update.location.lon='-122.40062'",
	})
	public static class TwitterUpdateAllParamsTests extends TwitterUpdateSinkIntegrationTests {

		@Test
		public void updateWithAllParams() {

			sink.input().send(new GenericMessage("Test Tweet"));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=Test%20Tweet" +
									"&in_reply_to_status_id=666666" +
									"&lat=37.78217&long=-122.40062" +
									"&place_id=myPlaceId" +
									"&media_ids=471592142565957632%2C471592142565957633" +
									"&auto_populate_reply_metadata=true" +
									"&attachment_url=http%3A%2F%2FattachementUrl" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterUpdateSinkConfiguration.class)
	public static class TestTwitterUpdateSinkApplication {

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
