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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;

public class MSTeamsServerExtension extends BuildServerAdapter {

	private static Logger logger = Logger.getLogger("com.foxgang.teamcity.msTeams");
	private SBuildServer server;
	private MSTeamsConfiguration configuration;
	private MSTeamsApiProcessor processor;
	private String messageFormat;
	private HashMap<TeamCityEvent, MSTeamsMessageBundle> eventMap;
	private MSTeamsNotificationMessageTemplates templates;

	public MSTeamsServerExtension(@NotNull SBuildServer server, 
			@NotNull MSTeamsConfiguration configuration, 
			@NotNull MSTeamsApiProcessor processor, 
			@NotNull MSTeamsNotificationMessageTemplates templates) {
		this.server = server;
		//this.configDirectory = serverPaths.getConfigDir();
		this.configuration = configuration;
		this.processor = processor;
		this.templates = templates;
		this.messageFormat = MSTeamsMessageFormat.HTML;
		this.eventMap = new HashMap<TeamCityEvent, MSTeamsMessageBundle>();
		this.eventMap.put(TeamCityEvent.BUILD_STARTED, new MSTeamsMessageBundle(MSTeamsEmoticonSet.POSITIVE, MSTeamsMessageColour.INFO));
		this.eventMap.put(TeamCityEvent.BUILD_SUCCESSFUL, new MSTeamsMessageBundle(MSTeamsEmoticonSet.POSITIVE, MSTeamsMessageColour.SUCCESS));
		this.eventMap.put(TeamCityEvent.BUILD_FAILED, new MSTeamsMessageBundle(MSTeamsEmoticonSet.NEGATIVE, MSTeamsMessageColour.ERROR));
		this.eventMap.put(TeamCityEvent.BUILD_INTERRUPTED, new MSTeamsMessageBundle(MSTeamsEmoticonSet.INDIFFERENT, MSTeamsMessageColour.WARNING));
		this.eventMap.put(TeamCityEvent.SERVER_STARTUP, new MSTeamsMessageBundle(null, MSTeamsMessageColour.NEUTRAL));
		this.eventMap.put(TeamCityEvent.SERVER_SHUTDOWN,new MSTeamsMessageBundle(null, MSTeamsMessageColour.NEUTRAL));
		logger.debug("Server extension created");
	}

	public void register() {
		this.server.addListener(this);
		logger.debug("Server extension registered");
		//this.controller.IsInitialised();
	}
	
	@Override
	public void changesLoaded(SRunningBuild build) {
		logger.debug(String.format("Build started: %s", build.getBuildType().getName()));
		super.changesLoaded(build);
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getBuildStartedStatus()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_STARTED);
		}
	}
	
	@Override
	public void buildFinished(SRunningBuild build) {
		super.buildFinished(build);
		Branch branch = build.getBranch();
		List<SFinishedBuild> buildHistory = build.getBuildType().getHistory();
		SFinishedBuild previousBuild = null;
				
		if (branch != null) {			
			for (SFinishedBuild tmpBuild : buildHistory) {
				Branch tmpBranch = tmpBuild.getBranch();
				if ((build.getBuildId() != tmpBuild.getBuildId()) && tmpBranch.getName().equals(branch.getName())) {
					previousBuild = tmpBuild;
					break;
				}
			}
		} else {
			if (buildHistory.size() > 1) {
				previousBuild = buildHistory.get(1);
			}
		}
		
		if (build.getBuildStatus().isSuccessful() && this.configuration.getEvents() != null && this.configuration.getEvents().getBuildSuccessfulStatus()) {
			if (!this.configuration.getEvents().getOnlyAfterFirstBuildSuccessfulStatus() || previousBuild == null || previousBuild.getBuildStatus().isFailed()) {
				this.processBuildEvent(build, TeamCityEvent.BUILD_SUCCESSFUL);
			}
		} else if (build.getBuildStatus().isFailed() && this.configuration.getEvents() != null && this.configuration.getEvents().getBuildFailedStatus()) {
			if (!this.configuration.getEvents().getOnlyAfterFirstBuildFailedStatus() || previousBuild == null || previousBuild.getBuildStatus().isSuccessful()) {
				this.processBuildEvent(build, TeamCityEvent.BUILD_FAILED);
			}
		}
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
		super.buildInterrupted(build);
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getBuildInterruptedStatus()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_INTERRUPTED);
		}
	}
	
	@Override
	public void serverStartup() {
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getServerStartupStatus()) {
			this.processServerEvent(TeamCityEvent.SERVER_STARTUP);
		}
	}

	@Override
	public void serverShutdown() {
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getServerShutdownStatus()) {
			this.processServerEvent(TeamCityEvent.SERVER_SHUTDOWN);
		}
	}
	
	private void processServerEvent(TeamCityEvent event) {
		try {
			boolean notify = this.configuration.getDefaultNotifyStatus();
			MSTeamsMessageBundle bundle = this.eventMap.get(event);
			String colour = bundle.getColour();
			String message = renderTemplate(this.templates.readTemplate(event), new HashMap<String, Object>());
			MSTeamsRoomNotification notification = new MSTeamsRoomNotification(message, this.messageFormat, colour, notify);
			String roomId = this.configuration.getDefaultChannelUrl();
			if ((event == TeamCityEvent.SERVER_STARTUP || event == TeamCityEvent.SERVER_SHUTDOWN) && 
					this.configuration.getServerEventChannelUrl() != null) {
				roomId = this.configuration.getServerEventChannelUrl();
			}
			if (roomId != null) {
				this.processor.sendNotification(notification, roomId);
			}
		} catch (Exception e) {
			logger.error(String.format("Error processing server event: %s", event), e);
		}
	}
	
	private void processBuildEvent(SRunningBuild build, TeamCityEvent event) {
		try {
			logger.info(String.format("Received %s build event", event));
			if (!this.configuration.getDisabledStatus() && !build.isPersonal()) {
				
		        Branch branch = build.getBranch();
		        if ((this.configuration.getBranchFilterEnabledStatus()) && (branch != null)) {
		          String branchDisplayName = branch.getDisplayName();
		          if (branchDisplayName.matches(this.configuration.getBranchFilterRegex())) {
		            logger.debug(String.format("Branch %s skipped", new Object[] { branchDisplayName }));
		            return;
		          }
		        }
				
				logger.info("Processing build event");
				String message = createHtmlBuildEventMessage(build, event);
				String colour = getBuildEventMessageColour(event);
				ProjectManager projectManager = this.server.getProjectManager();
				SProject project = projectManager.findProjectById(build.getProjectId());
				
				String channelUrl = getChannelUrl(project);
				MSTeamsProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(project.getProjectId());
				boolean notify = projectConfiguration.getNotifyStatus();
				boolean enabled = projectConfiguration.getEnabled();
				
				MSTeamsRoomNotification notification = new MSTeamsRoomNotification(message, this.messageFormat, colour, notify);
				
				if (enabled) {
					logger.debug(String.format("Channel to be notified: %s", channelUrl));
					logger.debug(String.format("Notification setting: %s", notify));
					this.processor.sendNotification(notification, channelUrl);
				}
			}
		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}
	
	/*
	 * Tries to use current project's config, then walks back up the hierarchy. If those are all empty, 
	 * use the admin-level default. Then fall back on the server notifications channel. Then return null 
	 * if literally nothing is configured.
	 */
	private String getChannelUrl(SProject project) {
		if (project.getParentProject() == null) {
			String defaultChannelUrl = configuration.getDefaultChannelUrl(); 
			if (defaultChannelUrl != null && !defaultChannelUrl.equals("")) {
				return defaultChannelUrl;
			}
			String serverEventChannelUrl = configuration.getServerEventChannelUrl(); 
			if (serverEventChannelUrl != null && !serverEventChannelUrl.equals("")) {
				return serverEventChannelUrl;
			}
			return null;
		}
		MSTeamsProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(project.getProjectId());
		String channelUrl = projectConfiguration.getChannelUrl();
		if (channelUrl != null && !channelUrl.equals("")) {
			return channelUrl;
		}
		return getChannelUrl(project.getParentProject());
	}
	
	private String getBuildEventMessageColour(TeamCityEvent buildEvent) {
		return this.eventMap.get(buildEvent).getColour();
	}
		
	private String createHtmlBuildEventMessage(SRunningBuild build, TeamCityEvent buildEvent) throws TemplateException, IOException {	
		Template template = this.templates.readTemplate(buildEvent);

		// Branch
		Branch branch = build.getBranch();
		boolean hasBranch = branch != null;
		logger.debug(String.format("Has branch: %s", hasBranch));
		String branchDisplayName = "";
		if (hasBranch) {
			branchDisplayName = branch.getDisplayName();
			logger.debug(String.format("Branch: %s", branchDisplayName));
		}
		
		// Contributors (committers)
		String contributors = getContributors(build);
		boolean hasContributors = !contributors.isEmpty();
		logger.debug(String.format("Has contributors: %s", hasContributors));
		
		// Fill the template.
		Map<String, Object> templateMap = new HashMap<String, Object>();		
		
		// Build statistics
		logger.debug("Adding standard build statistics");
		BuildStatistics statistics = build.getFullStatistics();
		logger.debug(String.format("\tNumber of tests: %s", statistics.getAllTestCount()));
		logger.debug(String.format("\tNumber of passed tests: %s", statistics.getPassedTestCount()));
		logger.debug(String.format("\tNumber of failed tests: %s", statistics.getFailedTestCount()));
		logger.debug(String.format("\tNumber of new failed tests: %s", statistics.getNewFailedCount()));
		logger.debug(String.format("\tNumber of ignored tests: %s", statistics.getIgnoredTestCount()));
		logger.debug(String.format("\tTests duration: %s", statistics.getTotalDuration()));
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.NO_OF_TESTS, statistics.getAllTestCount());
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.NO_OF_PASSED_TESTS, statistics.getPassedTestCount());
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.NO_OF_FAILED_TESTS, statistics.getFailedTestCount());
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.NO_OF_NEW_FAILED_TESTS, statistics.getNewFailedCount());
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.NO_OF_IGNORED_TESTS, statistics.getIgnoredTestCount());
		templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.DURATION_OF_TESTS, statistics.getTotalDuration());
		logger.debug("Adding discovered build statistics - use in templates by accessing the data model and with prefix stats");
		Map<String, BigDecimal> allStatistics = build.getStatisticValues();
		for (Map.Entry<String, BigDecimal> statistic : allStatistics.entrySet()) {
			logger.debug(String.format("\t%s: %s", statistic.getKey(), statistic.getValue()));
			templateMap.put(String.format("%s.%s", MSTeamsNotificationMessageTemplates.STATS_PARAMETERS_PREFIX, statistic.getKey()), statistic.getValue());
		}
		
//		// TODO: Add artifact dependencies as a template variable
//		try {
//			SBuildType buildType = build.getBuildType();
//			Collection<SBuildType> childDependencies = buildType.getChildDependencies();
//			for (SBuildType sBuildType : childDependencies) {
//				SFinishedBuild changes = sBuildType.getLastChangesFinished();
//				changes.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
//			} 
//			logger.debug(String.format("Children: %s", childDependencies.isEmpty()));
//			List<Dependency> dependencies = buildType.getDependencies();
//			//dependencies.get(0).
//			logger.debug(String.format("Children: %s", dependencies.isEmpty()));
//			List<SBuildType> dependencyReferences = buildType.getDependencyReferences();
//			logger.debug(String.format("Children: %s", dependencyReferences.isEmpty()));
//		} catch (Exception e) {
//		}
				
		// Add all available project, build configuration, agent, server, etc. parameters to the data model
		// These are accessed as ${.data_model["some.variable"]}
		// See: http://freemarker.org/docs/ref_specvar.html
		logger.debug("Adding build parameters");
		for (Map.Entry<String, String> entry : build.getParametersProvider().getAll().entrySet()) {
			logger.debug(String.format("\t%s: %s", entry.getKey(), entry.getValue()));
			templateMap.put(entry.getKey(), entry.getValue());
		}
		logger.debug("Adding agent parameters");
		for (Map.Entry<String, String> entry : build.getAgent().getAvailableParameters().entrySet()) {
			logger.debug(String.format("\t%s: %s", entry.getKey(), entry.getValue()));
			templateMap.put(entry.getKey(), entry.getValue());
		}
		// Standard plugin parameters
		logger.debug("Adding standard parameters");
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.FULL_NAME, build.getBuildType().getFullName());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.TRIGGERED_BY, build.getTriggeredBy().getAsString());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.HAS_CONTRIBUTORS, hasContributors);
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.CONTRIBUTORS, contributors);
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.HAS_BRANCH, hasBranch);
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.BRANCH, branchDisplayName);
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.SERVER_URL, this.server.getRootUrl());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.PROJECT_ID, build.getProjectExternalId());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.BUILD_ID, new Long(build.getBuildId()).toString());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.BUILD_TYPE_ID, build.getBuildTypeExternalId());
	    templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.BUILD_NUMBER, build.getBuildNumber());
		if (buildEvent == TeamCityEvent.BUILD_INTERRUPTED) {
			long userId = build.getCanceledInfo().getUserId();
			SUser user = this.server.getUserModel().findUserById(userId);
			templateMap.put(MSTeamsNotificationMessageTemplates.Parameters.CANCELLED_BY, user.getDescriptiveName());
		}
		
		return renderTemplate(template, templateMap);
	}

	private static String getContributors(SBuild build) {
		UserSet<SUser> committers = build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (SUser committer : committers.getUsers()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(committer.getDescriptiveName());
		}
		return sb.toString();
	}
	
	private static String renderTemplate(Template template, Map<String, Object> templateMap) throws TemplateException, IOException {
		Writer writer = new StringWriter();
	    template.process(templateMap, writer);
	    writer.flush();
	    String renderedTemplate = writer.toString();
	    writer.close();
	    return renderedTemplate;		
	}
}
