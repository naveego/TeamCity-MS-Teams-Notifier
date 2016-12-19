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

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(MSTeamsConfiguration.PROJECT_ROOM_KEY)
public class MSTeamsProjectConfiguration {
	
	@XStreamAlias(MSTeamsConfiguration.PROJECT_ID_KEY)
	private String projectId;
	
	@XStreamAlias(MSTeamsConfiguration.ROOM_ID_KEY)
	private String roomId;
	
	@XStreamAlias(MSTeamsConfiguration.NOTIFY_STATUS_KEY)
	private boolean notify;
	
	public MSTeamsProjectConfiguration(String projectId, String roomId, boolean notifyStatus) {
		this.projectId = projectId;
		this.roomId = roomId;
		this.notify = notifyStatus;
	}
	
	public String getProjectId() {
		return this.projectId;
	}

	public String getRoomId() {
		return this.roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
	public boolean getNotifyStatus() {
		return this.notify;
	}
	
	public void setNotifyStatus(boolean status) {
		this.notify = status;
	}
}
