package com.foxgang.teamcity.msTeams.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.foxgang.teamcity.msTeams.MSTeamsApiProcessor;
import com.foxgang.teamcity.msTeams.MSTeamsApiResultLinks;
import com.foxgang.teamcity.msTeams.MSTeamsConfiguration;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticon;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticonCache;
import com.foxgang.teamcity.msTeams.MSTeamsEmoticons;

public class MSTeamsEmoticonCacheTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test(enabled = false)
	public void testReload() throws URISyntaxException {
		String apiUrl = "https://api.msTeams.com/v2/";
		String apiToken = "token";
		
		MSTeamsConfiguration configuration = new MSTeamsConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
						
		// Execute
		MSTeamsApiProcessor processor = new MSTeamsApiProcessor(configuration);
		MSTeamsEmoticonCache emoticonCache = new MSTeamsEmoticonCache(processor);
		emoticonCache.reload();
		
		// Test
		AssertJUnit.assertEquals(204, emoticonCache.getSize());
	}
	
	@Test
	public void testSingleBatch() throws IOException {

		// Batch size
		int maxResults = 1;

		// First batch
		String emoticonId = "id1";
		String emoticonShortcut = "emo1";
		String emoticonUrl = "http://example.com/";
		int startIndex = 0;
		
		// First call
		MSTeamsEmoticon emoticon1 = new MSTeamsEmoticon(emoticonId, null, emoticonShortcut, emoticonUrl);
		List<MSTeamsEmoticon> items1 = new ArrayList<MSTeamsEmoticon>();
		items1.add(emoticon1);
		MSTeamsApiResultLinks links1 = new MSTeamsApiResultLinks(null, null, new String());
		MSTeamsEmoticons expectedEmoticons1 = new MSTeamsEmoticons(items1, startIndex, maxResults, links1);		

		// API call mocks
		MSTeamsApiProcessor processor = mock(MSTeamsApiProcessor.class);
		when(processor.getEmoticons(startIndex)).thenReturn(expectedEmoticons1);

		// Execute
		MSTeamsEmoticonCache emoticonCache = new MSTeamsEmoticonCache(processor);
		emoticonCache.reload();

		// Test
		AssertJUnit.assertEquals(1, emoticonCache.getSize());
		
		// Verifications
		verify(processor).getEmoticons(startIndex);
	}
	
	@Test
	public void testMultipleBatches() throws IOException {

		// Batch size
		int maxResults = 1;

		// First batch
		String emoticonId1 = "id1";
		String emoticonShortcut1 = "emo1";
		String emoticonUrl1 = "http://example.com/";
		int startIndex1 = 0;
		
		// Second batch
		String emoticonId2 = "id2";
		String emoticonShortcut2 = "emo2";
		String emoticonUrl2 = "http://example.com/";
		int startIndex2 = startIndex1 + maxResults;

		// First call
		MSTeamsEmoticon emoticon1 = new MSTeamsEmoticon(emoticonId1, null, emoticonShortcut1, emoticonUrl1);
		List<MSTeamsEmoticon> items1 = new ArrayList<MSTeamsEmoticon>();
		items1.add(emoticon1);
		MSTeamsApiResultLinks links1 = new MSTeamsApiResultLinks(null, null, new String());
		MSTeamsEmoticons expectedEmoticons1 = new MSTeamsEmoticons(items1, startIndex1, maxResults, links1);		

		// Second call
		MSTeamsEmoticon emoticon2 = new MSTeamsEmoticon(emoticonId2, null, emoticonShortcut2, emoticonUrl2);
		List<MSTeamsEmoticon> items2 = new ArrayList<MSTeamsEmoticon>();
		items2.add(emoticon2);
		MSTeamsApiResultLinks links2 = new MSTeamsApiResultLinks(null, null, null);
		MSTeamsEmoticons expectedEmoticons2 = new MSTeamsEmoticons(items2, startIndex1, maxResults, links2);		

		// API call mocks
		MSTeamsApiProcessor processor = mock(MSTeamsApiProcessor.class);
		when(processor.getEmoticons(startIndex1)).thenReturn(expectedEmoticons1);
		when(processor.getEmoticons(startIndex2)).thenReturn(expectedEmoticons2);

		// Execute
		MSTeamsEmoticonCache emoticonCache = new MSTeamsEmoticonCache(processor);
		emoticonCache.reload();

		// Test
		AssertJUnit.assertEquals(2, emoticonCache.getSize());
		
		// Verifications
		verify(processor).getEmoticons(startIndex1);
		verify(processor).getEmoticons(startIndex2);
	}
	
}
