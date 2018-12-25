package org.springframework.cloud.stream.app.twitter.stream.source;

/**
 * @author Christian Tzolov
 */
public class Main {

	public static void main(String[] args) {
		TwitterStreamSourceProperties.Filter.BoundingBox bb = new TwitterStreamSourceProperties.Filter.BoundingBox();
		System.out.println(bb.getClass().getName());
	}
}
