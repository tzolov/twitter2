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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
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
import org.springframework.util.MimeTypeUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
public abstract class TwitterGeoProcessorIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@Autowired
	protected Processor channels;

	@Autowired
	protected MessageCollector messageCollector;

	@BeforeClass
	public static void startMockServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);
	}

	@AfterClass
	public static void stopMockServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.geo.search.ip='127.0.0.1'",
			"twitter.geo.search.query=payload.toUpperCase()",
	})
	public static class TwitterGeoSearchByIPAndQueryTests extends TwitterGeoProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("ip", Arrays.asList("127.0.0.1"));
			queryParameters.put("query", Arrays.asList("Amsterdam"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			channels.input().send(MessageBuilder.withPayload(inPayload).build());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("ip", "127.0.0.1")
							.withQueryStringParameter("query", "AMSTERDAM"),
					once());

			String outPayload = received.getPayload().toString();

			assertNotNull(outPayload);

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places.size(), is(12));
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.location.lat='52.378'",
			"twitter.geo.location.lon='4.9'",
			"twitter.geo.search.query=payload.toUpperCase()",
	})
	public static class TwitterGeoSearchByLocationTests extends TwitterGeoProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Arrays.asList("52.378"));
			queryParameters.put("long", Arrays.asList("4.9"));
			queryParameters.put("query", Arrays.asList("Amsterdam"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			channels.input().send(MessageBuilder.withPayload(inPayload).build());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.378")
							.withQueryStringParameter("long", "4.9")
							.withQueryStringParameter("query", "Amsterdam"),
					once());

			String outPayload = received.getPayload().toString();

			assertNotNull(outPayload);

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places.size(), is(12));
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.type=reverse",
			"twitter.geo.location.lat='52.378'",
			"twitter.geo.location.lon='4.9'"
	})
	public static class TwitterGeoSearchByLocation2Tests extends TwitterGeoProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Arrays.asList("52.378"));
			queryParameters.put("long", Arrays.asList("4.9"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			channels.input().send(MessageBuilder.withPayload(inPayload).build());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.378")
							.withQueryStringParameter("long", "4.9"),
					once());

			String outPayload = received.getPayload().toString();

			assertNotNull(outPayload);

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places.size(), is(12));
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.location.lat=#jsonPath(new String(payload),'$.location.lat')",
			"twitter.geo.location.lon=#jsonPath(new String(payload),'$.location.lon')",
			"twitter.geo.search.query=#jsonPath(new String(payload),'$.country')",
	})
	public static class TwitterGeoSearchJsonPathTests extends TwitterGeoProcessorIntegrationTests {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Arrays.asList("52.0"));
			queryParameters.put("long", Arrays.asList("5.0"));
			queryParameters.put("query", Arrays.asList("Netherlands"));

			recordRequestExpectation(queryParameters);

			String inPayload = "{ \"country\" : \"Netherlands\", \"location\" : { \"lat\" : 52.00 , \"lon\" : 5.0 } }";

			channels.input().send(MessageBuilder
					.withPayload(inPayload)
					.setHeader("contentType", MimeTypeUtils.APPLICATION_JSON_VALUE)
					.build());

			Message<?> received = messageCollector.forChannel(channels.output()).poll();

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.0")
							.withQueryStringParameter("long", "5.0")
							.withQueryStringParameter("query", "Netherlands"),
					once());

			String outPayload = received.getPayload().toString();

			assertNotNull(outPayload);

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places.size(), is(12));
		}
	}

	public static void recordRequestExpectation(Map<String, List<String>> parameters) {

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/geo/search.json")
								.withQueryStringParameters(parameters),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/search_places_amsterdam.json"))
								.withDelay(TimeUnit.SECONDS, 1));

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterGeoProcessorConfiguration.class)
	public static class TestTwitterGeoProcessorApplication {
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
