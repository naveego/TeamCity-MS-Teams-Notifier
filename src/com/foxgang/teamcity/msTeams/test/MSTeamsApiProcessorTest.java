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

package com.foxgang.teamcity.msTeams.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.foxgang.teamcity.msTeams.MSTeamsApiProcessor;
import com.foxgang.teamcity.msTeams.MSTeamsApiResponseLinks;
import com.foxgang.teamcity.msTeams.MSTeamsApiResultLinks;
import com.foxgang.teamcity.msTeams.MSTeamsConfiguration;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticon;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticons;
import com.foxgang.teamcity.msTeams.MSTeamsMessageColour;
import com.foxgang.teamcity.msTeams.MSTeamsMessageFormat;
import com.foxgang.teamcity.msTeams.MSTeamsRoom;
import com.foxgang.teamcity.msTeams.MSTeamsRoomNotification;
import com.foxgang.teamcity.msTeams.MSTeamsRooms;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;

public class MSTeamsApiProcessorTest {

	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test(enabled = false)
	public void testGetEmoticons() throws URISyntaxException {
		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "token";
		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		MSTeamsEmoticons emoticons = processor.getEmoticons(0);
		for (MSTeamsEmoticon emoticon : emoticons.items) {
			System.out.println(String.format("%s: %s - %s", emoticon.id, emoticon.shortcut, emoticon.url));
		}
	}
	
	@Test
	public void testGetEmoticonsSuccess() throws Exception {
		// Test parameters
		int expectedNumberOfEmoticons = 1;
		String expectedEmoticonId = "0";
		String expectedEmoticonShortcut = "emo";
		String expectedEmoticonUrl = "http://example.com/";
		int expectedStatusCode = HttpServletResponse.SC_OK;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// JSON
		MSTeamsApiResponseLinks emoticonLinks = new MSTeamsApiResponseLinks("self", "webhooks", "members");
		MSTeamsEmoticon emoticon = new MSTeamsEmoticon(expectedEmoticonId, emoticonLinks, expectedEmoticonShortcut, expectedEmoticonUrl);
		MSTeamsApiResultLinks resultLinks = new MSTeamsApiResultLinks("self", "prev", "next");
		List<MSTeamsEmoticon> emoticonsList = new ArrayList<MSTeamsEmoticon>();
		emoticonsList.add(emoticon);
		MSTeamsEmoticons emoticons = new MSTeamsEmoticons(emoticonsList, 0, expectedNumberOfEmoticons, resultLinks);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(emoticons);
		System.out.println(expectedJson);
				
		// Configuration
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		
		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String response;

			public Handler(int statusCode, String response) {
				this.statusCode = statusCode;
				this.response = response;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        response.getWriter().write(this.response);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, expectedJson));
		server.start();
		
		// Execute
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		MSTeamsEmoticons actualEmoticons = processor.getEmoticons(0);
		
		// Clean up
		server.stop();
		
		// Test
		assertEquals(expectedNumberOfEmoticons, actualEmoticons.maxResults);
		assertEquals(expectedNumberOfEmoticons, actualEmoticons.items.size());
		MSTeamsEmoticon actualRoom = actualEmoticons.items.get(0);
		assertEquals(expectedEmoticonId, actualRoom.id);
		assertEquals(expectedEmoticonShortcut, actualRoom.shortcut);
		assertEquals(expectedEmoticonUrl, actualRoom.url);
	}
	
	@Test
	public void testGetEmoticonsException() throws Exception {
		// Test parameters
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		// Execute
		MSTeamsEmoticons actualEmoticons = processor.getEmoticons(0);
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
		assertNull(actualEmoticons);
	}
	
	@Test
	public void testGetEmoticonsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "invalid_token";
		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		MSTeamsEmoticons emoticons = processor.getEmoticons(0);
		assertNull(emoticons);
	}
	
	@Test
	public void testGetRoomsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		
		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "invalid_token";
		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		MSTeamsRooms rooms = processor.getRooms(0);
		assertNotNull(rooms);
		assertNotNull(rooms.items);
		assertEquals(0, rooms.items.size());
	}
	
	@Test(enabled = false)
	public void testGetRooms() throws URISyntaxException {

		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "token";

		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);

		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);

		MSTeamsRooms rooms = processor.getRooms(0);
		for (MSTeamsRoom room : rooms.items) {
			System.out.println(String.format("%s - %s", room.id, room.name));
		}
	}
	
	@Test
	public void testGetRoomsSuccess() throws Exception {
		// Test parameters
		int expectedNumberOfRooms = 1;
		String expectedRoomId = "0";
		String expectedRoomName = "testRoom";
		int expectedStatusCode = HttpServletResponse.SC_OK;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// JSON
		MSTeamsApiResponseLinks roomLinks = new MSTeamsApiResponseLinks("self", "webhooks", "members");
		MSTeamsRoom room = new MSTeamsRoom(expectedRoomId, roomLinks, expectedRoomName);
		MSTeamsApiResultLinks resultLinks = new MSTeamsApiResultLinks("self", "prev", "next");
		List<MSTeamsRoom> roomsList = new ArrayList<MSTeamsRoom>();
		roomsList.add(room);
		MSTeamsRooms rooms = new MSTeamsRooms(roomsList, 0, expectedNumberOfRooms, resultLinks);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(rooms);
		System.out.println(expectedJson);
				
		// Configuration
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		
		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String response;

			public Handler(int statusCode, String response) {
				this.statusCode = statusCode;
				this.response = response;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        response.getWriter().write(this.response);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, expectedJson));
		server.start();
		
		// Execute
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		MSTeamsRooms actualRooms = processor.getRooms(0);
		
		// Clean up
		server.stop();
		
		// Test
		assertEquals(expectedNumberOfRooms, actualRooms.maxResults);
		assertEquals(expectedNumberOfRooms, actualRooms.items.size());
		MSTeamsRoom actualRoom = actualRooms.items.get(0);
		assertEquals(expectedRoomId, actualRoom.id);
		assertEquals(expectedRoomName, actualRoom.name);
	}

	@Test
	public void testGetRoomsException() throws Exception {
		// Test parameters
		int expectedNumberOfRooms = 0;
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		// Execute
		MSTeamsRooms actualRooms = processor.getRooms(0);
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
		assertEquals(expectedNumberOfRooms, actualRooms.startIndex);
		assertEquals(expectedNumberOfRooms, actualRooms.maxResults);
		assertEquals(expectedNumberOfRooms, actualRooms.items.size());
		assertNull(actualRooms.links);
	}
	
	@Test
	public void testSendNotificationSuccess() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_NO_CONTENT;
		String expectedRoomId = "1";
		String expectedToken = "token";
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		ArrayList<String> responses = new ArrayList<String>();		
		MSTeamsRoomNotification notification = new MSTeamsRoomNotification("foo", MSTeamsMessageFormat.TEXT, MSTeamsMessageColour.INFO, true);
		ObjectMapper mapper = new ObjectMapper();
		String expectedJson = mapper.writeValueAsString(notification);

		// Handler
		class Handler extends AbstractHandler {
			
			private String roomId;
			private int statusCode;
			private String authToken;
			private ArrayList<String> responses;

			public Handler(String roomId, int statusCode, String authToken, ArrayList<String> responses) {
				this.roomId = roomId;
				this.statusCode = statusCode;
				this.authToken = authToken;
				this.responses = responses;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals(this.authToken, request.getHeader("Authorization").split(" ")[1]);
				assertEquals(this.roomId, target.split("/")[2]);
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
		        ServletInputStream inputStream = request.getInputStream();
		        String body = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
		        this.responses.add(body);
			}
			
		}
		
		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(expectedToken);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedRoomId, expectedStatusCode, expectedToken, responses));
		server.start();
				
		// Execute
		processor.sendNotification(notification, expectedRoomId);
		
		// Clean up
		server.stop();

		// Test
		assertEquals(1, responses.size());
		assertEquals(expectedJson, responses.get(0));
	}
	
	@Test
	public void testSendNotificationException() throws Exception {
		// Test parameters
		String expectedExceptionText = "UnsupportedSchemeException";
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
		
		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		// Execute
		processor.sendNotification(new MSTeamsRoomNotification(null, null, null, false), "1");
		logger.removeAppender(appender);
		
		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}
	
	@Test
	public void testSendNotificationUndeliverable() throws Exception {
		// Test parameters
		String expectedExceptionText = "Message could not be delivered: 400 Bad Request";
		int expectedStatusCode = HttpServletResponse.SC_BAD_REQUEST;
		String expectedRoomId = "1";
		String expectedToken = "token";
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		MSTeamsRoomNotification notification = new MSTeamsRoomNotification("foo", "text", "black", true);

		// Setup
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;

			public Handler(int statusCode) {
				this.statusCode = statusCode;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(expectedToken);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode));
		server.start();
				
		// Execute
		processor.sendNotification(notification, expectedRoomId);
		
		// Clean up
		logger.removeAppender(appender);
		server.stop();

		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}
	
	@Test
	public void testProxySupport() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_ACCEPTED;
		String host = "localhost";
		int port = 8080;
		URI uri = new URI(String.format("http://%s:%s/", host, port));
		String token = "token";

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String authToken;

			public Handler(int statusCode, String authToken) {
				this.statusCode = statusCode;
				this.authToken = authToken;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals(this.authToken, request.getParameter("auth_token"));
				assertEquals("true", request.getParameter("auth_test"));
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		// TODO: A more correct test would be to actually start up a proxy server here on another port.
		Properties systemProperties = new Properties();
		systemProperties.setProperty("http.proxyHost", host);
		systemProperties.setProperty("http.proxyPort", Integer.toString(port));
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration, systemProperties);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, token));
		server.start();
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();			
		
		// Clean up
		server.stop();

		// Test
		assertTrue(actualAuthResult);
	}
	
	@Test
	public void testTestAuthenticationSuccess() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_ACCEPTED;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;
			private String authToken;

			public Handler(int statusCode, String authToken) {
				this.statusCode = statusCode;
				this.authToken = authToken;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals(this.authToken, request.getParameter("auth_token"));
				assertEquals("true", request.getParameter("auth_test"));
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode, token));
		server.start();
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Clean up
		server.stop();

		// Test
		assertTrue(actualAuthResult);
	}
	
	@Test
	public void testTestAuthenticationBypassSslCertCheckSuccess() throws Exception {
		// Test parameters
		boolean bypassSsl = true;
		int expectedStatusCode = HttpServletResponse.SC_ACCEPTED;
		String host = "localhost";
		int httpsPort = 8443;
		URI uri = new URI(String.format("https://%s:%s/v2/", host, httpsPort));
		String token = "token";

		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setBypassSslCheck(bypassSsl);
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);

		// Mocks
        WireMockConfiguration config = wireMockConfig().httpsPort(httpsPort);
        WireMockServer wireMockServer = new WireMockServer(config);
        wireMockServer.start();
        WireMock.configure();
        stubFor(get(urlMatching("/v2/.*")).willReturn(aResponse().withStatus(expectedStatusCode)));
		try {
			// Execute
			boolean actualAuthResult = processor.testAuthentication();

			// Test
			assertTrue(actualAuthResult);		
		} finally {
			// Clean up
	        wireMockServer.stop();
		}
	}

	@Test
	public void testTestAuthenticationFailure() throws Exception {
		// Test parameters
		int expectedStatusCode = HttpServletResponse.SC_BAD_REQUEST;
		int port = 8080;
		URI uri = new URI(String.format("http://localhost:%s/", port));
		String token = "token";

		// Handler
		class Handler extends AbstractHandler {
			
			private int statusCode;

			public Handler(int statusCode) {
				this.statusCode = statusCode;
			}

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				assertEquals("true", request.getParameter("auth_test"));
				response.setContentType("text/html;charset=utf-8");
		        response.setStatus(this.statusCode);
		        baseRequest.setHandled(true);
			}
			
		}
		
		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri.toString());
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		SimpleServer server = new SimpleServer(port, new Handler(expectedStatusCode));
		server.start();
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Clean up
		server.stop();

		// Test
		assertFalse(actualAuthResult);
	}
	
	@Test
	public void testTestAuthenticationFailureWhenException() throws Exception {
		// Test parameters
		int port = 8080;
		String uri = String.format("nohttp://localhost:%s/", port);
		String token = "token";
	
		// Setup
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(uri);
		configuration.setApiToken(token);
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		
		// Execute
		boolean actualAuthResult = processor.testAuthentication();
		
		// Test
		assertFalse(actualAuthResult);
	}
	
	@Test(enabled = false)
	public void testTestAuthentication() throws URISyntaxException {

		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "token";

		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);

		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);

		assertTrue(processor.testAuthentication());
	}
	
	@Test(enabled = false)
	public void testTestAuthenticationWithSelfSignedCertManual() throws URISyntaxException {

		String apiUrl = "https://localhost/v2/";
		String apiToken = "token";

		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);

		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);

		assertTrue(processor.testAuthentication());
	}
		
}
