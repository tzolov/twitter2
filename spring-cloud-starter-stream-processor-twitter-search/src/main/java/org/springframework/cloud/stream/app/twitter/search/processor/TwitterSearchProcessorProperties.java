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

import javax.validation.constraints.NotNull;

import twitter4j.Query;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.search")
@Validated
public class TwitterSearchProcessorProperties {

	private static final Expression DEFAULT_EXPRESSION = new SpelExpressionParser().parseExpression("payload");

	/**
	 * Search tweets by search query string
	 */
	@NotNull
	private Expression query = DEFAULT_EXPRESSION;
	;

	/**
	 *
	 */
	private Expression maxId;

	/**
	 *
	 */
	private Expression sinceId;

	/**
	 * Number of tweets to return per page (e.g. per single request), up to a max of 100.
	 */
	private Expression count = new SpelExpressionParser().parseExpression("'100'");

	/**
	 * Restricts searched tweets to the given language, given by an <a href="http://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1 code</a>
	 */
	private Expression lang = null;

	/**
	 * If specified, returns tweets with since the given date. Date should be formatted as YYYY-MM-DD.
	 */
	private Expression since = null;

	/**
	 * If specified, returns tweets by users located within a given radius (in Km) of the given latitude/longitude,
	 * where the user's location is taken from their Twitter profile.
	 * Should be formatted as
	 */
	private Geocode geocode = new Geocode();

	/**
	 *  Specifies what type of search results you would prefer to receive.
	 *  The current default is "mixed." Valid values include:
	 *   mixed : Include both popular and real time results in the response.
	 *   recent : return only the most recent results in the response
	 *   popular : return only the most popular results in the response
	 */
	@NotNull
	private Query.ResultType resultType = Query.ResultType.mixed;

	public Expression getQuery() {
		return query;
	}

	public void setQuery(Expression query) {
		this.query = query;
	}

	public Expression getLang() {
		return lang;
	}

	public void setLang(Expression lang) {
		this.lang = lang;
	}

	public Expression getMaxId() {
		return maxId;
	}

	public void setMaxId(Expression maxId) {
		this.maxId = maxId;
	}

	public Expression getSinceId() {
		return sinceId;
	}

	public void setSinceId(Expression sinceId) {
		this.sinceId = sinceId;
	}

	public Expression getCount() {
		return count;
	}

	public void setCount(Expression count) {
		this.count = count;
	}

	public Expression getSince() {
		return since;
	}

	public void setSince(Expression since) {
		this.since = since;
	}

	public Geocode getGeocode() {
		return geocode;
	}

	public Query.ResultType getResultType() {
		return resultType;
	}

	public void setResultType(Query.ResultType resultType) {
		this.resultType = resultType;
	}

	public static class Geocode {

		/**
		 * User's latitude
		 */
		private Expression latitude;

		/**
		 * User's longitude
		 */
		private Expression longitude;

		/**
		 * Radius (in kilometers) around the (latitude, longitude) point
		 */
		private Expression radius = new SpelExpressionParser().parseExpression("'10'");;

		public Expression getLatitude() {
			return latitude;
		}

		public void setLatitude(Expression latitude) {
			this.latitude = latitude;
		}

		public Expression getLongitude() {
			return longitude;
		}

		public void setLongitude(Expression longitude) {
			this.longitude = longitude;
		}

		public Expression getRadius() {
			return radius;
		}

		public void setRadius(Expression radius) {
			this.radius = radius;
		}
	}
}
