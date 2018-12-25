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

package org.springframework.cloud.stream.app.twitter.common;

import java.time.Duration;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.connection")
@Validated
public class TwitterConnectionProperties {

	/**
	 * Your Twitter key
	 */
	@NotEmpty
	private String consumerKey;

	/**
	 * Your Twitter secret
	 */
	@NotEmpty
	private String consumerSecret;

	/**
	 * Your Twitter token
	 */
	@NotEmpty
	private String accessToken;

	/**
	 * Your Twitter token secret
	 */
	@NotEmpty
	private String accessTokenSecret;

	/**
	 * Twitter APIs poll interval. Applicable for the non streaming Twitter APIs
	 */
	private Duration pollInterval = Duration.ofMinutes(1);

	/**
	 * Enables Twitter4J debug mode.
	 */
	private boolean debugEnabled = false;

	public String getConsumerKey() {
		return consumerKey;
	}

	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}

	public String getConsumerSecret() {
		return consumerSecret;
	}

	public void setConsumerSecret(String consumerSecret) {
		this.consumerSecret = consumerSecret;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}

	public Duration getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(Duration pollInterval) {
		this.pollInterval = pollInterval;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}
}
