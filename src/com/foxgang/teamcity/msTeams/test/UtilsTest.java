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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import jetbrains.buildServer.serverSide.SProject;

import org.apache.log4j.BasicConfigurator;
import org.testng.annotations.Test;

import com.foxgang.teamcity.msTeams.MSTeamsApiProcessor;
import com.foxgang.teamcity.msTeams.MSTeamsApiResultLinks;
import com.foxgang.teamcity.msTeams.MSTeamsConfiguration;
import com.foxgang.teamcity.msTeams.MSTeamsProjectConfiguration;
import com.foxgang.teamcity.msTeams.MSTeamsRoom;
import com.foxgang.teamcity.msTeams.MSTeamsRooms;
import com.foxgang.teamcity.msTeams.Utils;

import org.testng.annotations.BeforeClass;

import static org.mockito.Mockito.*;

public class UtilsTest extends Utils {
	
	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test
	public void testJoin() {
		Collection<String> arrayListItems = new ArrayList<String>();
		arrayListItems.add("foo");
		arrayListItems.add("bar");
		arrayListItems.add("baz");
		assertEquals("foo, bar, baz", Utils.join(arrayListItems));
	}
	
	@Test
	public void testParentsParentHasConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "parent_project_id";
		String expectedParentsParentProjectId = "parents_parent_project_id";
		String expectedParentsParentRoomId = "parents_parent_room_id";
		boolean expectedParentsParentNotifyStatus = false;
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		// The immediate parent has no configuration
		configuration.setProjectConfiguration(new MSTeamsProjectConfiguration(expectedParentsParentProjectId, expectedParentsParentRoomId, expectedParentsParentNotifyStatus));
		
		// Mocks
		SProject parentsParentProject = mock(SProject.class);
		when(parentsParentProject.getProjectId()).thenReturn(expectedParentsParentProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentsParentProject);
		when(parentProject.getParentProjectId()).thenReturn(expectedParentsParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		MSTeamsProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentsParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentsParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentsParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}
	
	@Test
	public void testImmediateParentHasConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "parent_project_id";
		String expectedParentRoomId = "parent_room_id";
		boolean expectedParentNotifyStatus = true;
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setProjectConfiguration(new MSTeamsProjectConfiguration(expectedParentProjectId, expectedParentRoomId, expectedParentNotifyStatus));
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		MSTeamsProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}

	@Test
	public void testImmediateParentIsRootProjectWithConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		String expectedParentRoomId = "parent_room_id";
		boolean expectedParentNotifyStatus = true;
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setProjectConfiguration(new MSTeamsProjectConfiguration(expectedParentProjectId, expectedParentRoomId, expectedParentNotifyStatus));
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		MSTeamsProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}
	
	@Test
	public void testImmediateParentIsRootProjectWithoutConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		MSTeamsProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertNull(actualParentConfiguration);
	}
	
	@Test
	public void testProjectUsesDefaultRoomIdWhenRoomConfigurationAbsent() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setDefaultRoomId(expectedProjectId);
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		MSTeamsProjectConfiguration projectConfiguration = Utils.determineProjectConfiguration(project, configuration);
		assertEquals(expectedProjectId, projectConfiguration.getRoomId());
	}
	
	@Test
	public void testRootHasNoConfigurationWhenProjectInheritsFromParent() {
		// Test parameters
		String rootProjectId = "_Root";
		String parentProjectId = "project2";
		String parentRoomId = "parent";
		boolean notifyStatus = true;
		
		// Top-most project is the root
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(rootProjectId);
		
		// In-between project
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		when(parentProject.getParentProjectId()).thenReturn(rootProjectId);
		
		// Child project
		SProject project = mock(SProject.class);
		when(project.getParentProjectId()).thenReturn(parentProjectId);
		when(project.getParentProject()).thenReturn(parentProject);

		// Configuration
		MSTeamsProjectConfiguration parentProjectConfiguration = new MSTeamsProjectConfiguration(parentProjectId, parentRoomId, notifyStatus);
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setProjectConfiguration(parentProjectConfiguration);
		
		// Execute
		MSTeamsProjectConfiguration actualEffectiveProjectConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		
		// Test
		assertNull(actualEffectiveProjectConfiguration);
	}
	
	@Test
	public void testGetRooms() {
		// Test parameters
		String expectedFirstRoomId = "0";
		String expectedFirstRoomName = "roomOne";
		String expectedSecondRoomId = "1";
		String expectedSecondRoomName = "roomTwo";
		int expectedNumberOfRooms = 2;
		
		// Construct the room sets
		List<MSTeamsRoom> firstSetItems = new ArrayList<MSTeamsRoom>();
		firstSetItems.add(new MSTeamsRoom(expectedFirstRoomId, null, expectedFirstRoomName));
		List<MSTeamsRoom> secondSetItems = new ArrayList<MSTeamsRoom>();
		secondSetItems.add(new MSTeamsRoom(expectedSecondRoomId, null, expectedSecondRoomName));
		MSTeamsApiResultLinks firstSetlinks = new MSTeamsApiResultLinks(null, null, "hasNext");
		MSTeamsApiResultLinks secondSetlinks = new MSTeamsApiResultLinks(null, null, null);
		MSTeamsRooms firstSet = new MSTeamsRooms(firstSetItems, 0, 1, firstSetlinks);
		MSTeamsRooms secondSet = new MSTeamsRooms(secondSetItems, 1, 1, secondSetlinks);
		
		// Mocks
		MSTeamsApiProcessor processor = mock(MSTeamsApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(firstSet);
		when(processor.getRooms(1)).thenReturn(secondSet);
		
		// Execute
		TreeMap<String, String> actualRooms = Utils.getRooms(processor);
		
		// Test
		assertEquals(expectedNumberOfRooms, actualRooms.size());
	}
	
	@Test
	public void testIsRoomIdNullOrNone() {
		assertEquals(false, Utils.IsRoomIdNullOrNone(""));
		assertEquals(false, Utils.IsRoomIdNullOrNone("room1"));
		assertEquals(true, Utils.IsRoomIdNullOrNone("none"));
		assertEquals(true, Utils.IsRoomIdNullOrNone(null));
	}
	
}
