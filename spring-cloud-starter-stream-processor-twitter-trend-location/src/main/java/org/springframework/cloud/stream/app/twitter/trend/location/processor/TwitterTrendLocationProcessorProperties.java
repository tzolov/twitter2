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

package org.springframework.cloud.stream.app.twitter.trend.location.processor;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;


/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.trend.location")
@Validated
public class TwitterTrendLocationProcessorProperties {

	/**
	 *
	 */
	private Closest closest = new Closest();

	public Closest getClosest() {
		return closest;
	}

	public static class Closest {
		/**
		 * If provided with a long parameter the available trend locations will be sorted by distance, nearest
		 * to furthest, to the co-ordinate pair.
		 * The valid ranges for longitude is -180.0 to +180.0 (West is negative, East is positive) inclusive.
		 */
		private Expression lat;

		/**
		 * If provided with a lat parameter the available trend locations will be sorted by distance, nearest to
		 * furthest, to the co-ordinate pair. The valid ranges for longitude is -180.0 to +180.0 (West is negative,
		 * East is positive) inclusive.
		 */
		private Expression lon;

		public Expression getLat() {
			return lat;
		}

		public void setLat(Expression lat) {
			this.lat = lat;
		}

		public Expression getLon() {
			return lon;
		}

		public void setLon(Expression lon) {
			this.lon = lon;
		}
	}
}
