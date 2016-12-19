package com.foxgang.teamcity.msTeams.test;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.foxgang.teamcity.msTeams.MSTeamsApiProcessor;
import com.foxgang.teamcity.msTeams.MSTeamsApiResultLinks;
import com.foxgang.teamcity.msTeams.MSTeamsConfiguration;
import com.foxgang.teamcity.msTeams.MSTeamsConfigurationPageExtension;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticonCache;
import com.foxgang.teamcity.msTeams.MSTeamsNotificationMessageTemplates;
import com.foxgang.teamcity.msTeams.MSTeamsRoom;
import com.foxgang.teamcity.msTeams.MSTeamsRooms;
import com.foxgang.teamcity.msTeams.MSTeamsServerExtension;
import com.foxgang.teamcity.msTeams.TeamCityEvent;

import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.controllers.WebFixture;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public class MSTeamsConfigurationPageExtensionTest extends BaseServerTestCase {
	
	private WebFixture webFixture;
	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");

	@BeforeMethod
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		webFixture = new WebFixture(this.myFixture);
		BasicConfigurator.configure();
	}
	
	@AfterMethod
	protected void tearDown() throws Exception {
		super.clearFailure();
		super.tearDown();
	}

	@Test
	public void testIsAvailableFalse() throws IOException {
		// Test parameters
		boolean expectedAvailability = false;
		
		// Mocks and dependencies
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);
        
        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);
		
        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		boolean actualAvailability = myPage.isAvailable(request);
	
		// Test
		AssertJUnit.assertEquals(expectedAvailability, actualAvailability);
	}
	
	@Test
	public void testGetGroup() throws IOException {
		// Test parameters
		String expectedGroup = "Server Administration";
		
		// Mocks and dependencies
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);

        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		String actualGroup = myPage.getGroup();
	
		// Test
		AssertJUnit.assertEquals(expectedGroup, actualGroup);
	}
	
	@Test
	public void testFillModel() throws Exception {
		// Test parameters
		int expectedModelSize = 31;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<MSTeamsRoom> roomItems = new ArrayList<MSTeamsRoom>();
		roomItems.add(new MSTeamsRoom(expectedRoomId, null, expectedRoomName));
		MSTeamsApiResultLinks roomLinks = new MSTeamsApiResultLinks();
		MSTeamsRooms rooms = new MSTeamsRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);

        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
	}
	
	@Test
	public void testFillModelUsingServerEventRoomId() throws Exception {
		// Test parameters
		int expectedModelSize = 31;
		String expectedDefaultRoomId = "room1";
		String expectedServerEventRoomId = "room2";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setServerEventRoomId(expectedServerEventRoomId);
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<MSTeamsRoom> roomItems = new ArrayList<MSTeamsRoom>();
		roomItems.add(new MSTeamsRoom(expectedDefaultRoomId, null, expectedRoomName));
		MSTeamsApiResultLinks roomLinks = new MSTeamsApiResultLinks();
		MSTeamsRooms rooms = new MSTeamsRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);

        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
		AssertJUnit.assertEquals(expectedDefaultRoomId, model.get("defaultRoomId"));
		AssertJUnit.assertEquals(expectedServerEventRoomId, model.get("serverEventRoomId"));
	}
	
	@Test
	public void testFillModelNoEventsConfiguration() throws Exception {
		// Test parameters
		int expectedModelSize = 23;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setEvents(null);
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<MSTeamsRoom> roomItems = new ArrayList<MSTeamsRoom>();
		roomItems.add(new MSTeamsRoom(expectedRoomId, null, expectedRoomName));
		MSTeamsApiResultLinks roomLinks = new MSTeamsApiResultLinks();
		MSTeamsRooms rooms = new MSTeamsRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);

        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
	}
	
	@Test
	public void testFillModelGetTemplateRaisesException() throws IOException {
		// Test parameters
		int expectedModelSize = 19;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		String expectedExceptionText = "This is a test!";
		
		// Logger
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		
		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<MSTeamsRoom> roomItems = new ArrayList<MSTeamsRoom>();
		roomItems.add(new MSTeamsRoom(expectedRoomId, null, expectedRoomName));
		MSTeamsApiResultLinks roomLinks = new MSTeamsApiResultLinks();
		MSTeamsRooms rooms = new MSTeamsRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		MSTeamsApiProcessor processor = org.mockito.Mockito.mock(MSTeamsApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();	
		MSTeamsNotificationMessageTemplates templates = org.mockito.Mockito.mock(MSTeamsNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.BUILD_STARTED)).thenThrow(new IOException(expectedExceptionText));
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        MSTeamsServerExtension serverExtension = org.mockito.Mockito.mock(MSTeamsServerExtension.class);
		MSTeamsEmoticonCache emoticonCache = org.mockito.Mockito.mock(MSTeamsEmoticonCache.class);

        // The test page
        MSTeamsConfigurationPageExtension myPage = new MSTeamsConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		AssertJUnit.assertTrue(exceptionFound);
		super.clearFailure();
	}
}