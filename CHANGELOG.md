# 3.0.8 (Mar 27th, 2019)
### New feature

* None

### Improvement

* Java doc refinement: Handle.getBuildStatus, Handle.updateBuildStatus ([541365a](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/commit/541365a0740f1e5b17f2615076249c4da33c34bc))
* Extend POST timeout & avoid re-POST after timeout ([97de437](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/commit/97de437b98bec1cd9d46b78047886809c1e110d2))
* Handle proxy host to avoid fail in subsequent requests ([285d657](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/commit/285d6573107789f3480d5a7fbc726d94a93cb917))

### Bug fixes

* None


# 3.0.7 (Dec 2nd, 2018)
### New feature

* None

### Improvement

* None

### Bug fixes

* [JENKINS-55038](https://issues.jenkins-ci.org/browse/JENKINS-55038)


# 3.0.6 (Sep 18th, 2018)
### New feature

* Disable remote trigger job step instead of removing it

### Improvement

* None

### Bug fixes

* [JENKINS-52810](https://issues.jenkins-ci.org/browse/JENKINS-52810)


# 3.0.5 (Aug 20th, 2018)
### New feature

* None

### Improvement

* None

### Bug fixes

* [PR #46](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/46)
* [JENKINS-53125](https://issues.jenkins-ci.org/browse/JENKINS-53125)


# 3.0.4 (Jul 30th, 2018)
### New feature

* Support to abort remote job

### Improvement

* None

### Bug fixes

* [PR #45](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/45)


# 3.0.3 (Jul 23th, 2018)
### New feature

* None

### Improvement

* Add concurrent connection restriction to prevent remote servers from blocking 
* Add job info. & crumb cache to reduce the dummy inquiries when parallel triggering

### Bug fixes

* [JENKINS-52673](https://issues.jenkins-ci.org/browse/JENKINS-52673)

### Important change

* jdk version must be at least v1.8


# 3.0.2 (Jul 18th, 2018)
### New feature

* None

### Improvement

* HTTP utility reorganized
   * post with form-data

### Bug fixes

* Fix parameters are too long (HTTP status 414)


# 3.0.1 (Jul 9th, 2018)
### New feature
* Support triggering remote jobs via Jenkins proxy 

### Improvement
- code refinement

### Bug fixes
- [JENKINS-47919 ](https://issues.jenkins-ci.org/browse/JENKINS-47919) (clarified & fixed)


# 3.0.0 (May 17th, 2018)
### New feature
* Pipeline support

### Improvement
- [JENKINS-24240](https://issues.jenkins-ci.org/browse/JENKINS-24240)
- [JENKINS-29219](https://issues.jenkins-ci.org/browse/JENKINS-29219)
- [JENKINS-29220](https://issues.jenkins-ci.org/browse/JENKINS-29220)
- [JENKINS-29222](https://issues.jenkins-ci.org/browse/JENKINS-29222)

### Bug fixes
- [JENKINS-29381](https://issues.jenkins-ci.org/browse/JENKINS-29381)
- [JENKINS-30962](https://issues.jenkins-ci.org/browse/JENKINS-30962)
- [JENKINS-32462](https://issues.jenkins-ci.org/browse/JENKINS-32462)
- [JENKINS-32671](https://issues.jenkins-ci.org/browse/JENKINS-32671)
- [JENKINS-33269](https://issues.jenkins-ci.org/browse/JENKINS-33269)
- [JENKINS-47919 ](https://issues.jenkins-ci.org/browse/JENKINS-47919)


# 2.2.2 (Aug 16th, 2015)
### Misc:
- require Jenkins 1.580+

### Bug fixes:
- 2.2.0 didn't make it to the update center


# 2.2.0 (May 12th, 2015)
### New Feature/Enhancement:
- Ability to debug connection errors with (optional) enhanced console output ([pull request](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/9)) 

### Bug fixes:
- fixing [JENKINS-23748](https://issues.jenkins-ci.org/browse/JENKINS-23748) - Better error handleing for console output and logs to display info about the failure ([pull request](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/10))
- Don't fail build on 404 ([pull request](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/8))
- Fixed unhandled NPE ([pull request](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/7))
- Hand-full of other bugs ([pull request](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/5/commits)):
	* fixing [JENKINS-26127](https://issues.jenkins-ci.org/browse/JENKINS-26127)
	* fixing [JENKINS-24872](https://issues.jenkins-ci.org/browse/JENKINS-24872)
	* fixing [JENKINS-25366](https://issues.jenkins-ci.org/browse/JENKINS-25366)


# 2.1.3 (July 6th, 2014)
### Bug fixes:
- merging [pull request #4](https://github.com/jenkinsci/parameterized-remote-trigger-plugin/pull/4)


# 2.1.2 (April 26th, 2014)
### Bug fixes:
- fixing [JENKINS-22325](https://issues.jenkins-ci.org/browse/JENKINS-22325) - local job fails when not sending any parameters to remote job
- fixing [JENKINS-21470](https://issues.jenkins-ci.org/browse/JENKINS-21470) - UI does not display that a build is using a file to get the parameter list
- fixing [JENKINS-22493](https://issues.jenkins-ci.org/browse/JENKINS-22493) - 400 when remote job has default parameters and parameters are not explicitly list them
- fixing [JENKINS-22427](https://issues.jenkins-ci.org/browse/JENKINS-22427) - fails when remote job waits for available executor


# 2.1 (Feb 17th, 2014)
### New Feature/Enhancement:
- ability to specify the list of remote parameters from a file ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-21470))
- optionally block the local build until remote build is complete ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-20828))

### Misc:
- the console output has also been cleansed of displaying any URLs, since this could pose a security risk for public CI environemnts. 
- special thanks to [@tombrown5](https://github.com/timbrown5) for his contributions to the last item mentioned above


#2.0 ( Dec 25th, 2013)

Lots of refactoring and addition of some major new features.

### New Feature/Enhancement:
- integration with the 'Credentials' plugin ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-20826))
- able to override global credentials at a job-level ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-20829))
- support for 'Token Macro' plugin ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-20827))
- support for traditional Jenkins environment variables ([request ticket](https://issues.jenkins-ci.org/browse/JENKINS-21125))

### Misc:
Special thanks to [@elksson](https://github.com/elksson) for his contributions to the last 2 items mentioned above and to [@imod](https://github.com/imod) for his awesome feedback and feature suggestions.


# 1.1 ( Nov. 30th, 2013)

### Bug fixes:
- closing potential security gap for public-read environments
    
    
### New Feature/Enhancement:
- ability to not mark the build as failed if the remote build fails
    
    
### Misc:
- General code clean-up


# 1.0 
Initial release

### Available features:
- Trigger parameterized build on a remote Jenkins server
- Trigger non-parameterized build on a remote Jenkins server
- Authentication via username + API token
- Support for "build token root" plugin
