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

var msTeamsProject = {
	save : function() {
		BS.ajaxRequest($("msTeamsProjectForm").action, {
			parameters : 
				"project=1" + 
				"&channelUrl="    + $("channelUrl").value +
				"&notify="    + $("notify").checked +
				"&enabled="    + $("enabled").checked +
				"&projectId=" + $("projectId").value,
			onComplete : function(transport) {
				if (transport.responseXML) {
					BS.XMLResponse.processErrors(transport.responseXML, {
						onProfilerProblemError : function(elem) {
							alert(elem.firstChild.nodeValue);
						}
					});
				}
				BS.reload(true);
			}
		});
		return false;
	},
	testConnection : function() {
		jQuery.ajax(
				{
					url: $("msTeamsProjectForm").action, 
					data: {
							test: 1,
							channelUrl: $("channelUrl").value
						  },
					type: "GET"
				}).done(function() {
					alert("Notification successful!");
				}).fail(function() {
					alert("Notification failed!")
				});
		return false;
	},
};

var msTeamsAdmin = {
	save : function() {		
		BS.ajaxRequest($("msTeamsForm").action, {
			method : "POST",
			parameters : 
				"edit=1" + 
				"&defaultChannelUrl="             + $("defaultChannelUrl").value +
				"&serverEventChannelUrl="         + $("serverEventChannelUrl").value +
				"&notify="                        + $("notify").checked + 
				"&branchFilter="                  + $("branchFilter").checked + 
				"&branchFilterRegex="             + $("branchFilterRegex").value + 
				"&buildStarted="                  + $("buildStarted").checked +
				"&buildSuccessful="               + $("buildSuccessful").checked +
				"&buildFailed="                   + $("buildFailed").checked +
				"&buildInterrupted="              + $("buildInterrupted").checked +
				"&serverStartup="                 + $("serverStartup").checked +
				"&serverShutdown="                + $("serverShutdown").checked + 
				"&onlyAfterFirstBuildSuccessful=" + $("onlyAfterFirstBuildSuccessful").checked +
				"&onlyAfterFirstBuildFailed="     + $("onlyAfterFirstBuildFailed").checked + 				
				"&buildStartedTemplate="          + encodeURIComponent(document.getElementById('buildStartedTemplate').value) +
				"&buildSuccessfulTemplate="       + encodeURIComponent(document.getElementById('buildSuccessfulTemplate').value) +
				"&buildFailedTemplate="           + encodeURIComponent(document.getElementById('buildFailedTemplate').value) +
				"&buildInterruptedTemplate="      + encodeURIComponent(document.getElementById('buildInterruptedTemplate').value) +
				"&serverStartupTemplate="         + encodeURIComponent(document.getElementById('serverStartupTemplate').value) +
				"&serverShutdownTemplate="        + encodeURIComponent(document.getElementById('serverShutdownTemplate').value),
			onComplete : function(transport) {
				if (transport.responseXML) {
					BS.XMLResponse.processErrors(transport.responseXML, {
						onProfilerProblemError : function(elem) {
							alert(elem.firstChild.nodeValue);
						}
					});
				}
				BS.reload(true);
			}
		});
		return false;
	},
	
	testServerEventsConnection : function() {
		jQuery.ajax(
				{
					url: $("msTeamsForm").action, 
					data: {
							test: 1, 
							channelUrl: $("serverEventChannelUrl").value
						  },
					type: "GET"
				}).done(function() {
					alert("Notification successful!");
				}).fail(function() {
					alert("Notification failed!")
				});
		return false;
	},
	testDefaultConnection : function() {
		jQuery.ajax(
				{
					url: $("msTeamsForm").action, 
					data: {
							test: 1, 
							channelUrl: $("defaultChannelUrl").value
						  },
					type: "GET"
				}).done(function() {
					alert("Notification successful!");
				}).fail(function() {
					alert("Notification failed!")
				});
		return false;
	}
};