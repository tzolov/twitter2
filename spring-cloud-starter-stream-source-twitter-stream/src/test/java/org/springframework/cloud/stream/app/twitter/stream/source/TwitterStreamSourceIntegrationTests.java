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

package org.springframework.cloud.stream.app.twitter.stream.source;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

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
public abstract class TwitterStreamSourceIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = 1080;

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest streamFilterRequest;
	private static HttpRequest streamSampleRequest;
	private static HttpRequest streamFirehoseRequest;


	@Autowired
	protected Source channels;

	@Autowired
	protected MessageCollector messageCollector;

	@BeforeClass
	public static void startServer() {

		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);

		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		streamFilterRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/filter.json")
				.withBody(new StringBody("count=0&track=Java%2CPython&stall_warnings=true")));

		streamSampleRequest = mockClientRecordRequest(request()
				.withMethod("GET")
				.withPath("/stream/statuses/sample.json"));

		streamFirehoseRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/firehose.json")
				.withBody(new StringBody("count=0&stall_warnings=true")));
	}

	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=filter",
			"twitter.stream.filter.track=Java,Python"
	})
	public static class TwitterStreamFilterTests extends TwitterStreamSourceIntegrationTests {

		@Test
		public void testOne() throws InterruptedException {
			messageCollector.forChannel(this.channels.output()).poll(1, TimeUnit.SECONDS);
			mockClient.verify(streamFilterRequest, once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=sample"
	})
	public static class TwitterStreamSampleTests extends TwitterStreamSourceIntegrationTests {

		@Test
		public void testOne() throws InterruptedException {
			messageCollector.forChannel(this.channels.output()).poll(1, TimeUnit.SECONDS);
			mockClient.verify(streamSampleRequest, once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=firehose",
			"twitter.stream.filter.track=Java,Python"
	})
	public static class TwitterStreamFirehoseTests extends TwitterStreamSourceIntegrationTests {

		@Test
		public void testOne() throws InterruptedException {
			messageCollector.forChannel(this.channels.output()).poll(1, TimeUnit.SECONDS);
			mockClient.verify(streamFirehoseRequest, once());
		}
	}


	private static HttpRequest mockClientRecordRequest(HttpRequest request) {
		mockClient.when(request,/*unlimited())*/ exactly(1))
				.respond(
						response()
								.withStatusCode(200)
								.withHeaders(
										new Header("Content-Type", "application/json; charset=utf-8"),
										new Header("Cache-Control", "public, max-age=86400"))
								.withBody(TwitterTestUtils.asString("classpath:/response/stream_test_1.json"))
								.withDelay(TimeUnit.SECONDS, 10));
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterStreamSourceConfiguration.class)
	public static class TestTwitterStreamSourceApplication {

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
