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

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import jetbrains.buildServer.serverSide.SProject;

public class Utils {
	
	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");
	
	public static String join(Collection<String> items) {
		String separator = ", ";
		Iterator<String> iterator = items.iterator();
		StringBuilder stringBuilder = new StringBuilder();
		if (iterator.hasNext()) {
		  stringBuilder.append(iterator.next());
		  while (iterator.hasNext()) {
		    stringBuilder.append(separator).append(iterator.next());
		  }
		}
		
		return stringBuilder.toString();
	}
	
	public static TreeMap<String, String> getRooms(MSTeamsApiProcessor processor) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		int startIndex = 0;
		MSTeamsRooms rooms = null;
		do {
			rooms = processor.getRooms(startIndex);
			for (MSTeamsRoom room : rooms.items) {
				map.put(room.name, room.id);
			}
			startIndex = startIndex + rooms.maxResults;
		} while (rooms.links != null && rooms.links.next != null);
		return map;
	}
	
	public static boolean isRootProject(SProject project) {
		return project.getParentProject().getProjectId().equals(MSTeamsConfiguration.ROOT_PROJECT_ID_VALUE);
	}

	public static MSTeamsProjectConfiguration findFirstSpecificParentConfiguration(SProject project, MSTeamsConfiguration configuration) {
		MSTeamsProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(project.getParentProjectId());
		if ((!isRootProject(project) && projectConfiguration == null) ||
				(projectConfiguration != null && projectConfiguration.getRoomId().equals(MSTeamsConfiguration.ROOM_ID_PARENT_VALUE))) {
			return findFirstSpecificParentConfiguration(project.getParentProject(), configuration);
		} else if (projectConfiguration != null) {
			return projectConfiguration;
		}
		return null;
	}
	
	public static MSTeamsProjectConfiguration determineProjectConfiguration(SProject project, MSTeamsConfiguration configuration) {
		String projectId = project.getProjectId();
		String roomId = configuration.getDefaultRoomId();
		boolean notify = configuration.getDefaultNotifyStatus();
		boolean isRootProject = Utils.isRootProject(project);
		logger.debug(String.format("Default configuration for project ID %s: %s, %s", projectId, roomId, notify));
		logger.debug(String.format("Is root project: %s", isRootProject));
		
		MSTeamsProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(projectId);
		if (projectConfiguration != null) {
			roomId = projectConfiguration.getRoomId();
			notify = projectConfiguration.getNotifyStatus();
			logger.debug(String.format("Found specific configuration for project ID %s: %s, %s", projectId, roomId, notify));
		} else if (!isRootProject) {
			roomId = configuration.getDefaultRoomId();
			MSTeamsProjectConfiguration parentProjectConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
			if (parentProjectConfiguration != null) {
				logger.debug("Found specific configuration in hierarchy");
				roomId = parentProjectConfiguration.getRoomId();
				notify = parentProjectConfiguration.getNotifyStatus();
			}
			logger.debug(String.format("Traversed hierarchy for project ID %s: %s, %s", projectId, roomId, notify));
		}
		
		return new MSTeamsProjectConfiguration(projectId, roomId, notify);
	}
	
	public static boolean IsRoomIdNullOrNone(String roomId) {
		return roomId == null || roomId.equals(MSTeamsConfiguration.ROOM_ID_NONE_VALUE);
	}
		
}
