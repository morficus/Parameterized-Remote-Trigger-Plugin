Parameterized-Remote-Trigger-Plugin
===================================

A plugin to Jenkins CI which triggers parameterized builds on a remote Jenkins server.

Similar to the [Parameterized Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Parameterized+Trigger+Plugin), but in place of triggering a parameterized build on the current Jenkins server it will do so on a remote Jenkins server.

This is done by calling the ```/buildWithParameters``` URL on the remote server.

This plugin also has support for build authorization tokens.

Saddly, at the moment, this plugin does not support the new URL structures created when using [Build Token Root Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Build+Token+Root+Plugin) this will not work :-(