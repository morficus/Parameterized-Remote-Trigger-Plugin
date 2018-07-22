package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.JenkinsCrumb;

import hudson.Extension;
import hudson.model.PeriodicWork;
import net.sf.json.JSONObject;

@Extension
public class DropCachePeriodicWork extends PeriodicWork {

	private static Map<String, JenkinsCrumb> crumbMap = new HashMap<>();
	private static Map<String, JSONObject> jobInfoMap = new HashMap<>();

	private static Logger logger = Logger.getLogger(DropCachePeriodicWork.class.getName());
	private static Lock jobInfoLock = new ReentrantLock();
	private static Lock crumbLock = new ReentrantLock();

	@Override
	public long getRecurrencePeriod() {
		return TimeUnit.MINUTES.toMillis(10);
	}

	public static JenkinsCrumb safePutCrumb(String key, JenkinsCrumb jenkinsCrumb, boolean isCacheEnable) {
		if (!isCacheEnable)
			return jenkinsCrumb;
		try {
			crumbLock.lock();
			crumbMap.put(key, jenkinsCrumb);
			return jenkinsCrumb;
		} finally {
			crumbLock.unlock();
		}
	}

	public static JenkinsCrumb safeGetCrumb(String key, boolean isCacheEnable) {
		if (!isCacheEnable)
			return null;
		try {
			crumbLock.lock();
			if (crumbMap.containsKey(key)) {
				return crumbMap.get(key);
			} else {
				return null;
			}
		} finally {
			crumbLock.unlock();
		}
	}

	public static JSONObject safePutJobInfo(String key, JSONObject jobInfo, boolean isCacheEnable) {
		if (!isCacheEnable)
			return jobInfo;
		try {
			jobInfoLock.lock();
			jobInfoMap.put(key, jobInfo);
			return jobInfo;
		} finally {
			jobInfoLock.unlock();
		}
	}

	public static JSONObject safeGetJobInfo(String key, boolean isCacheEnable) {
		if (!isCacheEnable)
			return null;
		try {
			jobInfoLock.lock();
			if (jobInfoMap.containsKey(key)) {
				return jobInfoMap.get(key);
			} else {
				return null;
			}
		} finally {
			jobInfoLock.unlock();
		}
	}

	@Override
	protected void doRun() throws Exception {
		logger.log(Level.INFO, "begin schedule clean...");

		try {
			crumbLock.lock();
			crumbMap.clear();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Fail to clear crumb cache", e);
		} finally {
			crumbLock.unlock();
		}

		try {
			jobInfoLock.lock();
			jobInfoMap.clear();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Fail to clear job info cache", e);
		} finally {
			jobInfoLock.unlock();
		}

		logger.log(Level.INFO, "end schedule clean...");
	}

}
