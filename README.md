Parameterized-Remote-Trigger-Plugin
===================================

A plugin for Jenkins CI  that lets you trigger new builds parameterized on a **remote** Jenkins server when your build has completed.

Similar to the [Parameterized Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin), but for remote servers. And similar to [Call Remote Job Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Call+Remote+Job+Plugin) but with (arguably) better configuration options.

This is done by calling the ```/buildWithParameters``` URL on the remote server. (or the ```/build``` URL, if you don't specify any parameters)

This plugin also has support for build authorization tokens (as defined [here](https://wiki.jenkins-ci.org/display/JENKINS/Quick+and+Simple+Security) ).

###Screenshots
System configuration option

![System onfiguration option](https://raw.github.com/morficus/Parameterized-Remote-Trigger-Plugin/master/screenshots/1-system-settings.png)


Job setup options

![select from drop-down](https://raw.github.com/morficus/Parameterized-Remote-Trigger-Plugin/master/screenshots/2-build-configuration-1.png)

![Job setup options](https://raw.github.com/morficus/Parameterized-Remote-Trigger-Plugin/master/screenshots/3-build-configuration-2.png)


####Current Limitations
1. ~~Does not play well with [Build Token Root Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Build+Token+Root+Plugin) URL formats.~~ (added with [this commit](https://github.com/morficus/Parameterized-Remote-Trigger-Plugin/commit/f687dbe75d1c4f39f7e14b68220890384d7c5674)  )
2. ~~No username/password authentication, must use a 'build authorization token'.~~ (added with [this commit](https://github.com/morficus/Parameterized-Remote-Trigger-Plugin/commit/a23ade0add621830e85eb228990a95658e239b80) )
3. Follows a "fire & forget" model when triggering the remote build, which means that we don't know the status of the remote build, only if the request was successful or not.
