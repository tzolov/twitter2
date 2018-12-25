package org.springframework.cloud.stream.app.twitter.common;

import org.springframework.messaging.Message;

/**
 * @author Christian Tzolov
 */
public class TwitterUtils {

	@Deprecated()
	/**
	 * For Text and JSON content types convert the byte[] payload into String
	 *
	 * @deprecated Use the TwitterConnectionConfiguration().normalizeStringPayload() function
	 * instead
	 */
	public static Message<?> normalizeStringPayload(Message<?> message) {
		return new TwitterConnectionConfiguration().normalizeStringPayload().apply(message);
	}
}
