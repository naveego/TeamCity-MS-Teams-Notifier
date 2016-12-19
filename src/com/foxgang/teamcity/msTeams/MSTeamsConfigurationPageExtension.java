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

import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.PositionConstraint;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class MSTeamsConfigurationPageExtension extends AdminPage {

	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");

	private static final String AFTER_PAGE_ID = "jabber";
	private static final String BEFORE_PAGE_ID = "clouds";
	private static final String PAGE = "adminSettings.jsp";
	private static final String PLUGIN_NAME = "Microsoft Teams";

	private static final String TAB_TITLE = "MSTeams Notifier";
	private MSTeamsConfiguration configuration;
	private MSTeamsNotificationMessageTemplates templates;

	public MSTeamsConfigurationPageExtension(@NotNull PagePlaces pagePlaces, 
			@NotNull PluginDescriptor descriptor, 
			@NotNull MSTeamsConfiguration configuration, 
			@NotNull MSTeamsApiProcessor processor,
			@NotNull MSTeamsNotificationMessageTemplates templates,
			@NotNull MSTeamsServerExtension serverExtension) {
		super(pagePlaces);
		setPluginName(PLUGIN_NAME);
		setIncludeUrl(descriptor.getPluginResourcesPath(PAGE));
		setTabTitle(TAB_TITLE);
		ArrayList<String> after = new ArrayList<String>();
		after.add(AFTER_PAGE_ID);
		ArrayList<String> before = new ArrayList<String>();
		before.add(BEFORE_PAGE_ID);
		setPosition(PositionConstraint.between(after, before));
		this.configuration = configuration;
		this.templates = templates;
		register();
		logger.info("Global configuration page registered");
	}

	@Override
	public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
		super.fillModel(model, request);
		model.put(MSTeamsConfiguration.DEFAULT_CHANNEL_URL_KEY, this.configuration.getDefaultChannelUrl());
		model.put(MSTeamsConfiguration.SERVER_EVENT_CHANNEL_URL_KEY, this.configuration.getServerEventChannelUrl());
		model.put(MSTeamsConfiguration.NOTIFY_STATUS_KEY, this.configuration.getDefaultNotifyStatus());
		model.put(MSTeamsConfiguration.DISABLED_STATUS_KEY, this.configuration.getDisabledStatus());
	    model.put(MSTeamsConfiguration.BRANCH_FILTER_KEY, Boolean.valueOf(this.configuration.getBranchFilterEnabledStatus()));
	    model.put(MSTeamsConfiguration.BRANCH_FILTER_REGEX_KEY, this.configuration.getBranchFilterRegex());

		if (this.configuration.getEvents() != null) {
			model.put(MSTeamsConfiguration.BUILD_STARTED_KEY, this.configuration.getEvents().getBuildStartedStatus());
			model.put(MSTeamsConfiguration.BUILD_SUCCESSFUL_KEY, this.configuration.getEvents().getBuildSuccessfulStatus());
			model.put(MSTeamsConfiguration.ONLY_AFTER_FIRST_BUILD_SUCCESSFUL_KEY, this.configuration.getEvents().getOnlyAfterFirstBuildSuccessfulStatus());
			model.put(MSTeamsConfiguration.BUILD_FAILED_KEY, this.configuration.getEvents().getBuildFailedStatus());
			model.put(MSTeamsConfiguration.ONLY_AFTER_FIRST_BUILD_FAILED_KEY, this.configuration.getEvents().getOnlyAfterFirstBuildFailedStatus());
			model.put(MSTeamsConfiguration.BUILD_INTERRUPTED_KEY, this.configuration.getEvents().getBuildInterruptedStatus());
			model.put(MSTeamsConfiguration.SERVER_STARTUP_KEY, this.configuration.getEvents().getServerStartupStatus());
			model.put(MSTeamsConfiguration.SERVER_SHUTDOWN_KEY, this.configuration.getEvents().getServerShutdownStatus());
		}
		
		try {
			model.put(MSTeamsNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_STARTED).toString());
			model.put(MSTeamsNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.BUILD_STARTED_DEFAULT_TEMPLATE));
			model.put(MSTeamsNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_SUCCESSFUL).toString());
			model.put(MSTeamsNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.BUILD_SUCCESSFUL_DEFAULT_TEMPLATE));
			model.put(MSTeamsNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_FAILED).toString());
			model.put(MSTeamsNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.BUILD_FAILED_DEFAULT_TEMPLATE));
			model.put(MSTeamsNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_INTERRUPTED).toString());
			model.put(MSTeamsNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.BUILD_INTERRUPTED_DEFAULT_TEMPLATE));
			model.put(MSTeamsNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.SERVER_STARTUP).toString());
			model.put(MSTeamsNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.SERVER_STARTUP_DEFAULT_TEMPLATE));
			model.put(MSTeamsNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.SERVER_SHUTDOWN).toString());
			model.put(MSTeamsNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(MSTeamsNotificationMessageTemplates.SERVER_SHUTDOWN_DEFAULT_TEMPLATE));
		} catch (IOException e) {
			logger.error("Exception", e);
		}
				
		logger.debug("Configuration page variables populated");
	}
	
	@Override
	public String getGroup() {
		return SERVER_RELATED_GROUP;
	}

	@Override
	public boolean isAvailable(@NotNull HttpServletRequest request) {
		return super.isAvailable(request) && checkHasGlobalPermission(request, Permission.CHANGE_SERVER_SETTINGS);
	}
	
}
