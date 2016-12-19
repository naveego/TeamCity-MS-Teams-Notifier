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

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

// See: https://www.msTeams.com/docs/apiv2/method/get_all_rooms
public class MSTeamsEmoticons {

	@JsonProperty("items")
	public List<MSTeamsEmoticon> items;

	@JsonProperty("startIndex")
	public int startIndex;

	@JsonProperty("maxResults")
	public int maxResults;

	@JsonProperty("links")
	public MSTeamsApiResultLinks links;

	public MSTeamsEmoticons() {
		// Intentionally left empty
	}

	public MSTeamsEmoticons(List<MSTeamsEmoticon> items, int startIndex, int maxResults, MSTeamsApiResultLinks links) {
		this.items = items;
		this.startIndex = startIndex;
		this.maxResults = maxResults;
		this.links = links;
	}

}
