/**
Copyright 2016 Tyler Evert

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.foxgang.teamcity.msTeams;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.MediaType;

public class MSTeamsApiProcessor {
	private Properties systemProperties;
	
	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");
	
	public MSTeamsApiProcessor() throws URISyntaxException {
		this(System.getProperties());
	}	
	
	public MSTeamsApiProcessor(Properties systemProperties) throws URISyntaxException {
		this.systemProperties = systemProperties;
	}
	
	public void sendNotification(MSTeamsRoomNotification notification, String channelUrl) {
		try {
			// Serialize the notification to JSON
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(notification);
			logger.debug(json);

			// Make request
			HttpClient client = createClient();
			HttpPost postRequest = new HttpPost(channelUrl);
			postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			postRequest.setEntity(new StringEntity(json, Charset.forName("UTF-8")));
			HttpResponse postResponse = client.execute(postRequest);
			StatusLine status = postResponse.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_OK) {
				logger.error(String.format("Message could not be delivered: %s %s", status.getStatusCode(), status.getReasonPhrase()));
			}
		} catch (Exception e) {
			logger.error("Could not post room notification", e);
		}
	}
	
	public boolean testCommunication(String url) { // TODO: this code is duplicate with the code in sendNotification
		try {
			// Make request
			HttpClient client = createClient();
			HttpPost postRequest = new HttpPost(url);
			postRequest.setEntity(new ByteArrayEntity("{\"text\": \"Hello World!\"}".getBytes()));
			postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			HttpResponse postResponse = client.execute(postRequest);
			StatusLine status = postResponse.getStatusLine();
			if (status.getStatusCode() == HttpStatus.SC_OK) {
				return true;
			} else {
				logger.error(String.format("Test message failed: %s %s", status.getStatusCode(), status.getReasonPhrase()));
			}
		} catch (Exception e) {
			logger.error("Request failed", e);
		}
		
		return false;		
	}

	private CloseableHttpClient createClient() {
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		String proxyHost = systemProperties.getProperty("http.proxyHost");
		if (proxyHost != null) {
			logger.info("Proxy configuration detected");
			logger.debug(String.format("Host: %s", proxyHost));
			int proxyPort = 80;
			String proxyPortString = systemProperties.getProperty("http.proxyPort");
			if (proxyPortString != null) {
				proxyPort = Integer.parseInt(proxyPortString);
			}
			HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
			httpClientBuilder.setRoutePlanner(routePlanner);
			logger.info(String.format("Proxy configured: %s:%s", proxyHost, proxyPort));
		}
		return httpClientBuilder.build();
	}

}
