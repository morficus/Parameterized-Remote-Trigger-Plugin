package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

import java.io.IOException;
import java.util.logging.Logger;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.ConnectionResponse;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.RemoteBuildConfiguration;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.exceptions.ExceedRetryLimitException;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.pipeline.Handle;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob.RemoteBuildInfo;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob.RemoteBuildStatus;

/*
 * Going to migrate all rest APIs to here
 * */
public class RestUtils {

	private static Logger logger = Logger.getLogger(RestUtils.class.getName());

	public static ConnectionResponse cancelQueueItem(String rootUrl, Handle handle, BuildContext context,
			RemoteBuildConfiguration remoteConfig) throws IOException, InterruptedException {

		String cancelQueueUrl = String.format("%s/queue/cancelItem?id=%s", rootUrl, handle.getQueueId());
		ConnectionResponse resp = null;
		try {
			resp = HttpHelper.tryPost(cancelQueueUrl, context, null, remoteConfig.getHttpPostReadTimeout(),
					remoteConfig.getPollInterval(RemoteBuildStatus.QUEUED) * 2, 0,
					remoteConfig.getAuth2(), remoteConfig.getLock(cancelQueueUrl), remoteConfig.isUseCrumbCache());
		} catch (ExceedRetryLimitException e) {
			// Due to https://issues.jenkins-ci.org/browse/JENKINS-21311, we can't tell
			// whether the action was succeed,
			// Only try once and treat it as success
			logger.warning("Canceled queue item and not sure if it was succeed");
		}
		context.logger.println(String.format("Remote Queued Items:%s was canceled!", handle.getQueueId()));
		return resp;
	}

	public static ConnectionResponse stopRemoteJob(Handle handle, BuildContext context,
			RemoteBuildConfiguration remoteConfig) throws IOException, InterruptedException {

		RemoteBuildInfo buildInfo = handle.getBuildInfo();
		String stopJobUrl = String.format("%sstop", buildInfo.getBuildURL());
		ConnectionResponse resp = HttpHelper.tryPost(stopJobUrl, context, null, remoteConfig.getHttpPostReadTimeout(),
				remoteConfig.getPollInterval(buildInfo.getStatus()), remoteConfig.getConnectionRetryLimit(),
				remoteConfig.getAuth2(), remoteConfig.getLock(stopJobUrl), remoteConfig.isUseCrumbCache());
		context.logger.println(String.format("Remote Job:%s was aborted!", buildInfo.getBuildURL()));
		return resp;
	}

}
