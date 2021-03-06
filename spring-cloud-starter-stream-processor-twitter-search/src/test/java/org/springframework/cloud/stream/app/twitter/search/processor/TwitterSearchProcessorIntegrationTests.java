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

package org.springframework.cloud.stream.app.twitter.search.processor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.test.twitter.TwitterTestUtils;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionProperties;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.matchers.Times.exactly;
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
public abstract class TwitterSearchProcessorIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest searchVratsaRequest;
	private static HttpRequest searchAmsterdamRequest;

	@Autowired
	protected Processor channels;

	@Autowired
	protected MessageCollector messageCollector;

	@BeforeClass
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		searchVratsaRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/search/tweets.json")
				.withQueryStringParameter("q", "Vratsa")
				.withQueryStringParameter("count", "100"));

		searchAmsterdamRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/search/tweets.json")
				.withQueryStringParameter("q", "Amsterdam")
				.withQueryStringParameter("count", "100")
				.withQueryStringParameter("result_type", "popular")
				.withQueryStringParameter("geocode", "52.1,4.8,5.0km")
				.withQueryStringParameter("since", "2018-01-01")
				.withQueryStringParameter("lang", "en"));
	}

	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}

	public static HttpRequest setExpectation(HttpRequest request) {
		mockClient
				.when(request, exactly(1))
				.respond(response()
						.withStatusCode(200)
						.withHeaders(
								new Header("Content-Type", "application/json; charset=utf-8"),
								new Header("Cache-Control", "public, max-age=86400"))
						.withBody(TwitterTestUtils.asString("classpath:/response/search_3.json"))
						.withDelay(TimeUnit.SECONDS, 1)
				);
		return request;
	}

	@TestPropertySource(properties = {
			"debug=true",
			"logging.level.*=DEBUG",
	})
	public static class TwitterSearchPayloadTests extends TwitterSearchProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Object payload = "Vratsa";

			channels.input().send(MessageBuilder.withPayload(payload).build());

			mockClient.verify(searchVratsaRequest, once());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			Assert.assertNotNull(received);
			String outPayload = received.getPayload().toString();
			List tweets = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(tweets.size(), is(3));
		}
	}

	@TestPropertySource(properties = {
			"twitter.search.query='Amsterdam'",
			"twitter.search.count='100'",
			"twitter.search.lang='en'",
			"twitter.search.geocode.latitude='52.1'",
			"twitter.search.geocode.longitude='4.8'",
			"twitter.search.geocode.radius='5'",
			"twitter.search.since='2018-01-01'",
			"twitter.search.resultType=popular"
	})
	public static class TwitterSearchAmsterdamTests extends TwitterSearchProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Object payload = "Something";

			channels.input().send(MessageBuilder.withPayload(payload).build());

			mockClient.verify(searchAmsterdamRequest, once());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			Assert.assertNotNull(received);
			String outPayload = received.getPayload().toString();
			List tweets = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(tweets.size(), is(3));
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterSearchProcessorConfiguration.class)
	public static class TestTwitterSearchProcessorApplication {

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
