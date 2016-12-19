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

import java.io.IOException;

import jetbrains.buildServer.serverSide.ServerPaths;

import org.testng.annotations.Test;

import com.foxgang.teamcity.msTeams.MSTeamsApiResponseLinks;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticonSet;
import com.foxgang.teamcity.msTeams.MSTeamsMessageColour;
import com.foxgang.teamcity.msTeams.MSTeamsMessageFormat;
import com.foxgang.teamcity.msTeams.MSTeamsNotificationMessageTemplates;
import com.foxgang.teamcity.msTeams.TeamCityEvent;

public class DummyTest {

	@Test
	public void forCoverageOnly() throws IOException {
		// EMMA doesn't cover these classes fully, as their constructors are never invoked
		// The problem is that they are effectively static classes, but there's no direct support for static classes in Java
		new MSTeamsMessageColour();
		new MSTeamsMessageFormat();
		new MSTeamsEmoticonSet();
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);
		MSTeamsNotificationMessageTemplates templates = new MSTeamsNotificationMessageTemplates(serverPaths);
		templates.new Parameters();
		
		// And another stupid one: Some hidden byte code and the only way to get full coverage is to call these methods
		TeamCityEvent.values();
		TeamCityEvent.valueOf("BUILD_STARTED");
		
		// We're not interested in testing Jackson
		MSTeamsApiResponseLinks responseLinks = new MSTeamsApiResponseLinks();
		responseLinks.handleUnknown("foo", "bar");
	}

}
