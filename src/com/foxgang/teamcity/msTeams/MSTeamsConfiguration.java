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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.*;

@XStreamAlias("msTeams")
public class MSTeamsConfiguration {
	public static final String DEFAULT_CHANNEL_URL_KEY = "defaultChannelUrl";
	public static final String SERVER_EVENT_CHANNEL_URL_KEY = "serverEventChannelUrl";
	public static final String DISABLED_STATUS_KEY = "disabled";
	public static final String NOTIFY_STATUS_KEY = "notify";
	public static final String PROJECT_ENABLED_KEY = "enabled";
	public static final String CHANNEL_URL_KEY = "channelUrl";
	public static final String PROJECT_ID_KEY = "projectId";
	public static final String PROJECT_ROOM_KEY = "projectRoom";
	public static final String ROOT_PROJECT_ID_VALUE = "_Root";
	public static final String EVENTS_KEY = "events";
	public static final String BUILD_STARTED_KEY = "buildStarted";
	public static final String BUILD_SUCCESSFUL_KEY = "buildSuccessful";
	public static final String BUILD_FAILED_KEY = "buildFailed";
	public static final String BUILD_INTERRUPTED_KEY = "buildInterrupted";
	public static final String SERVER_STARTUP_KEY = "serverStartup";
	public static final String SERVER_SHUTDOWN_KEY = "serverShutdown";
	public static final String EMOTICON_CACHE_SIZE_KEY = "emoticonCacheSize";
	public static final String ONLY_AFTER_FIRST_BUILD_SUCCESSFUL_KEY = "onlyAfterFirstBuildSuccessful";
	public static final String ONLY_AFTER_FIRST_BUILD_FAILED_KEY = "onlyAfterFirstBuildFailed";
	public static final String BRANCH_FILTER_KEY = "branchFilter";
	public static final String BRANCH_FILTER_REGEX_KEY = "branchFilterRegex";

	@XStreamAlias(DISABLED_STATUS_KEY)
	private boolean disabled = false;

	@XStreamAlias(NOTIFY_STATUS_KEY)
	private boolean notify = false;

	@XStreamAlias(DEFAULT_CHANNEL_URL_KEY)
	private String defaultChannelUrl;
	
	@XStreamAlias(SERVER_EVENT_CHANNEL_URL_KEY)
	private String serverEventChannelUrl;
	
	// We use a list for correct serialization. It causes us to perform a linear search when getting or setting, but that's ok. 
	@XStreamImplicit
	private List<MSTeamsProjectConfiguration> projectRoomMap = new ArrayList<MSTeamsProjectConfiguration>();
	
	@XStreamAlias(MSTeamsConfiguration.EVENTS_KEY)
	private MSTeamsEventConfiguration events = new MSTeamsEventConfiguration();
	
	@XStreamAlias(BRANCH_FILTER_KEY)
	private boolean branchFilterEnabled;

	@XStreamAlias(BRANCH_FILTER_REGEX_KEY)
	private String branchFilterRegex;
	
	public MSTeamsConfiguration() {
		// Intentionally left empty
	}

	public MSTeamsEventConfiguration getEvents() {
		return this.events;
	}
	
	public void setEvents(MSTeamsEventConfiguration events) {
		this.events = events;
	}
	
	public List<MSTeamsProjectConfiguration> getProjectRoomMap() {
		return this.projectRoomMap;
	}
	
	public void setProjectConfiguration(MSTeamsProjectConfiguration newProjectConfiguration) {
		boolean found = false;
		for (MSTeamsProjectConfiguration projectConfiguration : this.projectRoomMap) {
			if (projectConfiguration.getProjectId().contentEquals(newProjectConfiguration.getProjectId())) {
				projectConfiguration.setChannelUrl(newProjectConfiguration.getChannelUrl());
				projectConfiguration.setNotifyStatus(newProjectConfiguration.getNotifyStatus());
				projectConfiguration.setEnabled(newProjectConfiguration.getEnabled());
				found = true;
			}
		}
		if (!found) {
			this.projectRoomMap.add(newProjectConfiguration);
		}
	}
	
	public MSTeamsProjectConfiguration getProjectConfiguration(String projectId) {
		for (MSTeamsProjectConfiguration projectConfiguration : this.projectRoomMap) {
			if (projectConfiguration.getProjectId().contentEquals(projectId)) {
				return projectConfiguration;
			}
		}
		return null;
	}

	public boolean getDisabledStatus() {
		return this.disabled;
	}

	public boolean getDefaultNotifyStatus() {
		return this.notify;
	}

	public String getDefaultChannelUrl() {
		return this.defaultChannelUrl;
	}

	public String getServerEventChannelUrl() {
		return this.serverEventChannelUrl;
	}

	public boolean getBranchFilterEnabledStatus() {
		return this.branchFilterEnabled;
	}

	public String getBranchFilterRegex() {
		return this.branchFilterRegex;
	}

	public void setDisabledStatus(boolean status) {
		this.disabled = status;
	}

	public void setNotifyStatus(boolean status) {
		this.notify = status;
	}

	public void setDefaultChannelUrl(String defaultChannelUrl) {
		this.defaultChannelUrl = defaultChannelUrl;
	}
	
	public void setServerEventChannelUrl(String serverEventChannelUrl) {
		this.serverEventChannelUrl = serverEventChannelUrl;
	}
	
	public void setBranchFilterEnabledStatus(boolean status) {
		this.branchFilterEnabled = status;
	}

	public void setBranchFilterRegex(String regex) {
		this.branchFilterRegex = regex;
	}	
}
