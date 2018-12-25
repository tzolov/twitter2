package org.springframework.cloud.stream.app.twitter.common;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeTypeUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Tzolov
 */
public class TwitterConnectionConfigurationTests {

	@Test
	public void testBinaryContentType() {

		Function<Message<?>, Message<?>> function =
				new TwitterConnectionConfiguration().normalizeStringPayload();

		String payload = "Test Binary Content";
		Map<String, Object> headers = new HashedMap<>();
		headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.IMAGE_PNG);
		GenericMessage<byte[]> inMessage = new GenericMessage<>(payload.getBytes(), headers);

		Message<?> outMessage = function.apply(inMessage);

		assertThat(inMessage.getPayload(), is(payload.getBytes()));
		assertThat(outMessage.getPayload(), is(payload.getBytes()));
	}

	@Test
	public void testDefaultContentType() {

		Function<Message<?>, Message<?>> function =
				new TwitterConnectionConfiguration().normalizeStringPayload();

		String payload = "Test Binary Content";
		GenericMessage<byte[]> inMessage = new GenericMessage<>(payload.getBytes());

		Message<?> outMessage = function.apply(inMessage);

		assertThat(inMessage.getPayload(), is(payload.getBytes()));
		assertThat(outMessage.getPayload(), is(payload));

		assertThat(outMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE), nullValue());
	}

	@Test
	public void testJsonContentType() {
		Function<Message<?>, Message<?>> function =
				new TwitterConnectionConfiguration().normalizeStringPayload();

		String payload = "Test Binary Content";
		Map<String, Object> headers = new HashedMap<>();
		headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
		GenericMessage<byte[]> inMessage = new GenericMessage<>(payload.getBytes(), headers);

		Message<?> outMessage = function.apply(inMessage);

		assertThat(inMessage.getPayload(), is(payload.getBytes()));
		assertThat(outMessage.getPayload(), is(payload));

		assertThat(outMessage.getHeaders().get(MessageHeaders.CONTENT_TYPE),
				is(MimeTypeUtils.APPLICATION_JSON_VALUE));
	}

}
