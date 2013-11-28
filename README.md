Parameterized-Remote-Trigger-Plugin
===================================

A plugin for Jenkins CI  that lets you trigger new builds parameterized on a **remote** Jenkins server when your build has completed.

Similar to the [Parameterized Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin), but for remote servers.

This is done by calling the ```/buildWithParameters``` URL on the remote server.

This plugin also has support for build authorization tokens (as defined [here](https://wiki.jenkins-ci.org/display/JENKINS/Quick+and+Simple+Security) )

####Current Limitations
1. Does not play well with [Build Token Root Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Build+Token+Root+Plugin) URL formats.
2. No username/password authentication, must use a 'build authorization token'.
3. Follows a "fire & forget" model when triggering the remote build, which means that we don't know the status of the remote build.
