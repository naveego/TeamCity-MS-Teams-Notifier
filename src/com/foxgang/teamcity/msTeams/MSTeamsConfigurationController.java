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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import com.thoughtworks.xstream.XStream;

public class MSTeamsConfigurationController extends BaseController {

	private static final Object ACTION_ENABLE = "enable";
	private static final String ACTION_PARAMETER = "action";
	private static final String CONTROLLER_PATH = "/configureMSTeams.html";
	public static final String EDIT_PARAMETER = "edit";
	private static final String TEST_PARAMETER = "test";
	private static final String PROJECT_PARAMETER = "project";
	private static final String msTeams_CONFIG_FILE = "msTeams.xml";
	public static final String msTeams_CONFIG_DIRECTORY = "msTeams";
	private static final String SAVED_ID = "configurationSaved";
	private static final String NOT_SAVED_ID = "configurationNotSaved";
	private static final String SAVED_MESSAGE = "Saved";
	private static final String NOT_SAVED_TEMPLATE_VALIDATION_FAILED = "Template validation failed. Check the FreeMarker documentation for syntax.";
	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");
	private String configFilePath;

	private MSTeamsConfiguration configuration;
	private MSTeamsApiProcessor processor;
	private MSTeamsNotificationMessageTemplates templates;
	
	public MSTeamsConfigurationController(@NotNull SBuildServer server, 
			@NotNull ServerPaths serverPaths, 
			@NotNull WebControllerManager manager,
			@NotNull MSTeamsConfiguration configuration, 
			@NotNull MSTeamsApiProcessor processor, 
			@NotNull MSTeamsNotificationMessageTemplates templates) throws IOException {
		manager.registerController(CONTROLLER_PATH, this);
		this.configuration = configuration;
		this.configFilePath = (new File(serverPaths.getConfigDir(), msTeams_CONFIG_FILE)).getCanonicalPath();
		this.processor = processor;
		this.templates = templates;
		logger.debug(String.format("Config file path: %s", this.configFilePath));
		logger.info("Controller created");
	}

	private void handleProjectConfigurationChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing project configuration");
		String channelUrl = request.getParameter(MSTeamsConfiguration.CHANNEL_URL_KEY);
		boolean notify = Boolean.parseBoolean(request.getParameter(MSTeamsConfiguration.NOTIFY_STATUS_KEY));
		boolean enabled = Boolean.parseBoolean(request.getParameter(MSTeamsConfiguration.PROJECT_ENABLED_KEY));
		String projectId = request.getParameter(MSTeamsConfiguration.PROJECT_ID_KEY);
		logger.debug(String.format("Channel URL: %s", channelUrl));
		logger.debug(String.format("Trigger notification: %s", notify));
		logger.debug(String.format("Enable notifications: %s", enabled));
		logger.debug(String.format("Project ID: %s", projectId));
		MSTeamsProjectConfiguration projectConfiguration = new MSTeamsProjectConfiguration(projectId, channelUrl, notify, enabled);
		this.configuration.setProjectConfiguration(projectConfiguration);
		this.getOrCreateMessages(request).addMessage(SAVED_ID, SAVED_MESSAGE);
		this.saveConfiguration();
	}
	
	private static boolean validateTemplates(List<String> templateStrings) {
		for (String templateString : templateStrings) {
			if (!MSTeamsNotificationMessageTemplates.validateTemplate(templateString)) {
				return false;
			}
		}
		
		return true;
	}
	
	private void handleConfigurationChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing configuration");
		logger.debug(String.format("Query string: '%s'", request.getQueryString()));
		
		// Get parameters
		String defaultChannelUrl = request.getParameter(MSTeamsConfiguration.DEFAULT_CHANNEL_URL_KEY);
		String serverEventChannelUrl = request.getParameter(MSTeamsConfiguration.SERVER_EVENT_CHANNEL_URL_KEY);
		String notify = request.getParameter(MSTeamsConfiguration.NOTIFY_STATUS_KEY);
	    String branchFilter = request.getParameter(MSTeamsConfiguration.BRANCH_FILTER_KEY);
	    String branchFilterRegex = request.getParameter(MSTeamsConfiguration.BRANCH_FILTER_REGEX_KEY);
		String buildStarted = request.getParameter(MSTeamsConfiguration.BUILD_STARTED_KEY);
		String buildSuccessful = request.getParameter(MSTeamsConfiguration.BUILD_SUCCESSFUL_KEY);
		String buildFailed = request.getParameter(MSTeamsConfiguration.BUILD_FAILED_KEY);
		String buildInterrupted = request.getParameter(MSTeamsConfiguration.BUILD_INTERRUPTED_KEY);
		String serverStartup = request.getParameter(MSTeamsConfiguration.SERVER_STARTUP_KEY);
		String serverShutdown = request.getParameter(MSTeamsConfiguration.SERVER_SHUTDOWN_KEY);
		String onlyAfterFirstBuildSuccessful = request.getParameter(MSTeamsConfiguration.ONLY_AFTER_FIRST_BUILD_SUCCESSFUL_KEY);
		String onlyAfterFirstBuildFailed = request.getParameter(MSTeamsConfiguration.ONLY_AFTER_FIRST_BUILD_FAILED_KEY);
		String buildStartedTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_KEY);
		String buildSuccessfulTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_KEY);
		String buildFailedTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_KEY);
		String buildInterruptedTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_KEY);
		String serverStartupTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_KEY);
		String serverShutdownTemplate = request.getParameter(MSTeamsNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_KEY);
		
		// Logging
		logger.debug(String.format("Trigger notification: %s", notify));
		logger.debug("Events:");
		logger.debug(String.format("\tDefault Channel URL: %s", defaultChannelUrl));
	    logger.debug(String.format("\tBranch filter enabled: %s", new Object[] { branchFilter }));
	    logger.debug(String.format("\tBranch filter regex: %s", new Object[] { branchFilterRegex }));
		logger.debug(String.format("\tBuild started: %s", buildStarted));		
		logger.debug(String.format("\tBuild successful: %s", onlyAfterFirstBuildSuccessful));
		logger.debug(String.format("\t\tOnly after first: %s", buildSuccessful));
		logger.debug(String.format("\tBuild failed: %s", buildFailed));
		logger.debug(String.format("\t\tOnly after first: %s", onlyAfterFirstBuildFailed));
		logger.debug(String.format("\tBuild interrupted: %s", buildInterrupted));
		logger.debug(String.format("\tServer event URL: %s", serverEventChannelUrl));
		logger.debug(String.format("\tServer startup: %s", serverStartup));
		logger.debug(String.format("\tServer shutdown: %s", serverShutdown));
		logger.debug("Templates:");
		logger.debug(String.format("\tBuild started: %s", buildStartedTemplate));
		logger.debug(String.format("\tBuild successful: %s", buildSuccessfulTemplate));
		logger.debug(String.format("\tBuild failed: %s", buildFailedTemplate));
		logger.debug(String.format("\tBuild interrupted: %s", buildInterruptedTemplate));
		logger.debug(String.format("\tServer startup: %s", serverStartupTemplate));
		logger.debug(String.format("\tServer shutdown: %s", serverShutdownTemplate));
		
		// Validation
		ArrayList<String> templateStrings = new ArrayList<String>();
		templateStrings.add(buildStartedTemplate);
		templateStrings.add(buildSuccessfulTemplate);
		templateStrings.add(buildSuccessfulTemplate);
		templateStrings.add(buildInterruptedTemplate);
		templateStrings.add(serverStartupTemplate);
		templateStrings.add(serverShutdownTemplate);
		if (!validateTemplates(templateStrings)) {
			this.getOrCreateMessages(request).addMessage(NOT_SAVED_ID, NOT_SAVED_TEMPLATE_VALIDATION_FAILED);
			return;
		}

		// Save the configuration
		this.configuration.setNotifyStatus(Boolean.parseBoolean(notify));
		this.configuration.setDefaultChannelUrl(defaultChannelUrl.equals("") ? null : defaultChannelUrl);
		this.configuration.setServerEventChannelUrl(serverEventChannelUrl.equals("") ? null : serverEventChannelUrl);
	    this.configuration.setBranchFilterEnabledStatus(Boolean.parseBoolean(branchFilter));
	    this.configuration.setBranchFilterRegex(branchFilterRegex.equals("") ? null : branchFilterRegex);
		MSTeamsEventConfiguration events = new MSTeamsEventConfiguration();
		events.setBuildStartedStatus(Boolean.parseBoolean(buildStarted));
		events.setBuildSuccessfulStatus(Boolean.parseBoolean(buildSuccessful));
		events.setOnlyAfterFirstBuildSuccessfulStatus(Boolean.parseBoolean(onlyAfterFirstBuildSuccessful));
		events.setBuildFailedStatus(Boolean.parseBoolean(buildFailed));
		events.setOnlyAfterFirstBuildFailedStatus(Boolean.parseBoolean(onlyAfterFirstBuildFailed));
		events.setBuildInterruptedStatus(Boolean.parseBoolean(buildInterrupted));
		events.setServerStartupStatus(Boolean.parseBoolean(serverStartup));
		events.setServerShutdownStatus(Boolean.parseBoolean(serverShutdown));
		this.configuration.setEvents(events);
		this.saveConfiguration();
		
		// Save the templates
		this.templates.writeTemplate(TeamCityEvent.BUILD_STARTED, buildStartedTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_SUCCESSFUL, buildSuccessfulTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_FAILED, buildFailedTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_INTERRUPTED, buildInterruptedTemplate);
		this.templates.writeTemplate(TeamCityEvent.SERVER_STARTUP, serverStartupTemplate);
		this.templates.writeTemplate(TeamCityEvent.SERVER_SHUTDOWN, serverShutdownTemplate);
		
		// Update the page
		this.getOrCreateMessages(request).addMessage(SAVED_ID, SAVED_MESSAGE);
	}
	
	private void handleTestConnection(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("Sending a test message");
		// Figure out which URL we were given to test
		String channelUrl = request.getParameter(MSTeamsConfiguration.CHANNEL_URL_KEY);
		logger.debug(String.format("Channel URL: %s", channelUrl));
		boolean result = this.processor.testCommunication(channelUrl);
		logger.debug(String.format("Authentication status: %s", result));
		if (result) {
			response.setStatus(HttpStatus.SC_OK);
		} else {
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
		}
	}
	
	private void handlePluginStatusChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing status");
		Boolean disabled = !request.getParameter(ACTION_PARAMETER).equals(ACTION_ENABLE);
		logger.debug(String.format("Disabled status: %s", disabled));
		this.configuration.setDisabledStatus(disabled);
		this.saveConfiguration();
	}
	
	@Override
	public ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) {
		try {
			logger.debug("Handling request");
			if (request.getParameter(PROJECT_PARAMETER) != null) {
				logger.debug("Updating project configuration");
				this.handleProjectConfigurationChange(request);
			} else if (request.getParameter(EDIT_PARAMETER) != null) {
				logger.debug("Updating configuration");
				this.handleConfigurationChange(request);
			} else if (request.getParameter(TEST_PARAMETER) != null) {
				logger.debug("Test connection");
				this.handleTestConnection(request, response);
			} else if (request.getParameter(ACTION_PARAMETER) != null) {
				logger.debug("Changing plugin status");
				this.handlePluginStatusChange(request);
			} else {
				logger.debug("No handler for request:");
				@SuppressWarnings("unchecked")
				Map<String, String[]> requestParameters = request.getParameterMap();
				for (String key : requestParameters.keySet()) {
					logger.debug(String.format("%s=%s", key, requestParameters.get(key)[0]));
				}
			}
		} catch (Exception e) {
			logger.error("Could not handle request", e);
		}

		return null;
	}

	public void initialize() {
		try {
			File file = new File(this.configFilePath);
			if (file.exists()) {
				logger.debug("Loading existing configuration");
				this.loadConfiguration();
			} else {
				logger.debug("No configuration file exists; creating new one");
				this.saveConfiguration();
			}
		} catch (Exception e) {
			logger.error("Could not load configuration", e);
		}
		logger.info("Controller initialised");
	}
	
	public void loadConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.setClassLoader(this.configuration.getClass().getClassLoader());
		xstream.setClassLoader(MSTeamsProjectConfiguration.class.getClassLoader());
		xstream.processAnnotations(MSTeamsConfiguration.class);
		FileReader fileReader = new FileReader(this.configFilePath);
		MSTeamsConfiguration configuration = (MSTeamsConfiguration) xstream.fromXML(fileReader);
		fileReader.close();
		
		// Copy the values, because we need it on the original shared (bean),
		// which is a singleton
		this.configuration.setDefaultChannelUrl(configuration.getDefaultChannelUrl());
		this.configuration.setNotifyStatus(configuration.getDefaultNotifyStatus());
		this.configuration.setDisabledStatus(configuration.getDisabledStatus());
		this.configuration.setBranchFilterEnabledStatus(configuration.getBranchFilterEnabledStatus());
		this.configuration.setBranchFilterRegex(configuration.getBranchFilterRegex());
		this.configuration.setServerEventChannelUrl(configuration.getServerEventChannelUrl());
		if (configuration.getEvents() != null) {
			this.configuration.getEvents().setBuildStartedStatus(configuration.getEvents().getBuildStartedStatus());
			this.configuration.getEvents().setBuildSuccessfulStatus(configuration.getEvents().getBuildSuccessfulStatus());
			this.configuration.getEvents().setBuildFailedStatus(configuration.getEvents().getBuildFailedStatus());
			this.configuration.getEvents().setBuildInterruptedStatus(configuration.getEvents().getBuildInterruptedStatus());
			this.configuration.getEvents().setServerStartupStatus(configuration.getEvents().getServerStartupStatus());
			this.configuration.getEvents().setServerShutdownStatus(configuration.getEvents().getServerShutdownStatus());
			this.configuration.getEvents().setOnlyAfterFirstBuildSuccessfulStatus(configuration.getEvents().getOnlyAfterFirstBuildSuccessfulStatus());
			this.configuration.getEvents().setOnlyAfterFirstBuildFailedStatus(configuration.getEvents().getOnlyAfterFirstBuildFailedStatus());
		}
		if (configuration.getProjectRoomMap() != null) {
			for (MSTeamsProjectConfiguration projectConfiguration : configuration.getProjectRoomMap()) {
				this.configuration.setProjectConfiguration(projectConfiguration);
			}
		}
	}

	public void saveConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.processAnnotations(this.configuration.getClass());
		File file = new File(this.configFilePath);
		file.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		xstream.toXML(this.configuration, fileOutputStream);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

}
