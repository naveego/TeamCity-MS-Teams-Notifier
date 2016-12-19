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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class MSTeamsEmoticonCache {

	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");
	private MSTeamsApiProcessor processor;
	private HashMap<String, String> emoticonCache;	
	
	public MSTeamsEmoticonCache(@NotNull MSTeamsApiProcessor processor) {
		this.processor = processor;
		this.emoticonCache = new HashMap<String, String>();
	}
	
	public int reload() {
		logger.debug("Caching all available emoticons");
		this.emoticonCache = new HashMap<String, String>();
		int startIndex = 0;
		MSTeamsEmoticons emoticons = null;
		do {
			logger.debug(String.format("Start index: %s", startIndex));
			emoticons = this.processor.getEmoticons(startIndex);
			if (emoticons == null) {
				break;
			}
			for (MSTeamsEmoticon emoticon : emoticons.items) {
				logger.debug(String.format("Adding emoticon: %s - %s", emoticon.shortcut, emoticon.url));
				this.emoticonCache.put(emoticon.shortcut, emoticon.url);
			}
			// Due to a bug in the msTeams REST API, we have to work around using the next resource (#49).
			// The msTeams API returns a next resource on each request, even when there are no more emoticons to retrieve.
			startIndex = startIndex + emoticons.items.size();
		} while (emoticons.items.size() > 0);		
		logger.info(String.format("Emoticon cache reloaded: %s", this.emoticonCache.size()));
		return this.emoticonCache.size();
	}
	
	public int getSize() {
		return this.emoticonCache.size();
	}
	
	public String get(String name) {
		return this.emoticonCache.get(name);
	}
	
}
