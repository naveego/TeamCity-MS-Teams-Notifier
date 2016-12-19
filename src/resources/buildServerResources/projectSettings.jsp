<!--
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
-->

<%@ include file="/include.jsp"%>

<c:url value="/configureMSTeams.html" var="actionUrl" />

<bs:linkCSS dynamic="${true}">
  ${teamcityPluginResourcesPath}css/msTeamsAdmin.css
</bs:linkCSS>

<bs:linkScript>
    ${teamcityPluginResourcesPath}js/msTeamsAdmin.js
</bs:linkScript>

<form action="${actionUrl}" id="msTeamsProjectForm" method="post"
	onsubmit="return msTeamsProject.save()">
	<div class="editNotificatorSettingsPage">
		<bs:messages key="configurationSaved" />
		<table class="runnerFormTable">
			<tr>
				<th><label for="channelUrl">Webhook URL: </label></th>
				<td>
				  <textarea name="channelUrl" id="channelUrl">${channelUrl}</textarea>
                </td>
			</tr>
			<tr>
				<th><label for="notify">Trigger Notifications: </label></th> <!-- TODO is this a thing in Teams? -->
				<td>
					<forms:checkbox name="notify" checked="${notify}" value="${notify}"/>
					<span class="smallNote">When checked, a notification for all people in the room will be triggered, taking user preferences into account.</span>
				</td>
			</tr>
			<tr>
				<th><label for="enabled">Enabled Notifications: </label></th>
				<td>
					<forms:checkbox name="enabled" checked="${enabled}" value="${enabled}"/>
					<span class="smallNote">Uncheck to disable notifications altogether for this build configuration.</span>
				</td>
			</tr>
		</table>
		<div class="saveButtonsBlock">
			<forms:submit label="Save" />
			<forms:submit id="testConnection" type="button" label="Post Test Message" onclick="return msTeamsProject.testConnection()"/>
			<input type="hidden" id="projectId" name="projectId" value="${projectId}"/>
			<forms:saving />
		</div>
	</div>
</form>