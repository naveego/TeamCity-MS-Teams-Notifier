TeamCity-MicrosoftTeams-Notifier
=========================

A fun TeamCity MS Teams Notifier for sending build server notifications to a Teams channel, using colours and emoticons and customisable notification messages.

This plugin used the Hipchat notifier plugin as a starting point: https://github.com/parautenbach/TeamCity-HipChat-Notifier

Screenshots TODO

# Installation

Installation Guide TODO

# Configuration

Configuration TODO

# Developers

* This is an Eclipse project.
* Clone the repository and set the `teamcity.home` property in the `build.xml` to your TeamCity server's home directory (Windows users, use forward slashes in the path, e.g. `C:/TeamCity`).
* To open the project in Eclipse go to _File -> Import -> General -> Existing Projects into Workspace -> Select root directory_. Navigate to the folder with the cloned source code. You can consider to use the Mylyn Github connector for Eclipse.
* Set the `TEAMCITY_HOME` classpath variable under Eclipse preferences to the same location as above.
* Check that Eclipse knows where to find a JDK (under Installed JREs in the Java section of Eclipse preferences).
* On Windows make sure that you have `JAVA_HOME` variable set to where your JDK is installed, e.g. `C:\Program Files\Java\jdk1.7.0_51`.
* To release the project as a TeamCity plugin right click on `build.xml` and select _Run As -> 2 Ant Build_. Check the release target and run. The plugin package will be created under a `build` folder.
* Tests are built on [TestNG](http://testng.org/), coverage determined by [EMMA](http://emma.sourceforge.net/) and static analysis performed using [lint4j](http://www.jutils.com/).

For debugging, add the snippets in teamcity-server-log4j.xml in this project's root to `conf/teamcity-server-log4j.xml` and then monitor `logs/msTeams-notifier.log `.

# Troubleshooting

* Enable logging, as explained directly above, and look for any errors and warnings.
* If you run TeamCity (Tomcat) behind a proxy, e.g. Nginx, you may need to increase your [buffer sizes](http://nginx.org/en/docs/http/ngx_http_proxy_module.html) from the defaults, because of the increased POST payload to save notification templates.

# Future Improvements

Roadmap TODO

# Change log

## Version 0.0.1
* Adapted source from Hipchat plugin (https://github.com/parautenbach/TeamCity-HipChat-Notifier)
* Slight stability improvements to unit tests
* Updating classpath entries for TeamCity 10.0