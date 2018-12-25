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

package org.springframework.cloud.stream.app.twitter.search.source;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.twitter.common.TwitterConnectionConfiguration;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * Search pagination with max_id and since_id:
 * https://developer.twitter.com/en/docs/tweets/timelines/guides/working-with-timelines.html
 *
 * @author Christian Tzolov
 */
@EnableBinding(Source.class)
@EnableConfigurationProperties({ TwitterSearchSourceProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterSearchSourceConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterSearchSourceConfiguration.class);

	@Autowired
	private TwitterSearchSourceProperties searchProperties;

	@Autowired
	private Twitter twitter;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private SearchPagination searchPage;

	@Bean
	public SearchPagination searchPage() {
		return new SearchPagination(
				this.searchProperties.getPage(),
				this.searchProperties.isRestartFromMostRecentOnEmptyResponse());
	}

	@InboundChannelAdapter(value = Source.OUTPUT, poller = @Poller(fixedDelay = "10000", maxMessagesPerPoll = "1"))
	public Message<byte[]> myMessageSource() {
		try {
			Query query = toQuery(this.searchProperties, this.searchPage);

			QueryResult result = this.twitter.search(query);

			List<Status> tweets = result.getTweets();

			logger.info(String.format("%s, size: %s", this.searchPage.status(), tweets.size()));

			this.searchPage.update(tweets);

			try {
				String json = objectMapper.writeValueAsString(tweets);

				return MessageBuilder
						.withPayload(json.getBytes())
						.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
						.build();
			}
			catch (JsonProcessingException e) {
				logger.error("Status to JSON conversion error!", e);
			}
		}
		catch (TwitterException e) {
			logger.error("Twitter error", e);
		}

		return null;
	}

	private Query toQuery(TwitterSearchSourceProperties searchProperties, SearchPagination pagination) {

		Query query = new Query();
		if (searchProperties.getCount() > 0) {
			query.count(searchProperties.getCount());
		}
		if (StringUtils.hasText(searchProperties.getQuery())) {
			query.setQuery(searchProperties.getQuery());
		}
		if (StringUtils.hasText(searchProperties.getLang())) {
			query.setLang(searchProperties.getLang());
		}
		if (StringUtils.hasText(searchProperties.getSince())) {
			query.setSince(searchProperties.getSince());
		}
		if (searchProperties.getGeocode().isValid()) {
			query.setGeoCode(
					new GeoLocation(
							this.searchProperties.getGeocode().getLatitude(),
							this.searchProperties.getGeocode().getLongitude()),
					this.searchProperties.getGeocode().getRadius(),
					Query.KILOMETERS);
		}

		if (searchProperties.getResultType() != Query.ResultType.mixed) {
			query.setResultType(this.searchProperties.getResultType());
		}

		if (pagination.getSinceId() > 0) {
			query.setSinceId(pagination.getSinceId());
		}
		if (pagination.getMaxId() > 0) {
			query.setMaxId(pagination.getMaxId());

			Assert.isTrue(pagination.getMaxId() >= (pagination.getSinceId() - 1),
					String.format("For non empty MAX_ID, The MAX_ID (%s) must always be bigger than [SINCE_ID -1](%s)",
							pagination.getMaxId(), (pagination.getSinceId() - 1)));
		}

		return query;
	}
}
