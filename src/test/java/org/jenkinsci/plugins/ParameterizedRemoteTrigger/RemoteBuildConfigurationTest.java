package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils.StringTools.NL_UNIX;
import static org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils.StringTools.NL_UNIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.RemoteBuildConfiguration.DescriptorImpl;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.NullAuth;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.auth2.TokenAuth;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2.JobParameters;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2.MapParameters;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.pipeline.RemoteBuildPipelineStep;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.remoteJob.RemoteBuildStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.WithoutJenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ListView;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.User;
import hudson.security.AuthorizationStrategy.Unsecured;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import hudson.security.csrf.DefaultCrumbIssuer;
import hudson.util.LogTaskListener;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class RemoteBuildConfigurationTest {

	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();

	private User testUser;
	private String testUserToken;

	private void disableAuth() {
		jenkinsRule.jenkins.setAuthorizationStrategy(Unsecured.UNSECURED);
		jenkinsRule.jenkins.setSecurityRealm(SecurityRealm.NO_AUTHENTICATION);
		jenkinsRule.jenkins.setCrumbIssuer(null);
	}

	private void enableAuth() throws IOException {
		MockAuthorizationStrategy mockAuth = new MockAuthorizationStrategy();
		jenkinsRule.jenkins.setAuthorizationStrategy(mockAuth);

		HudsonPrivateSecurityRealm hudsonPrivateSecurityRealm = new HudsonPrivateSecurityRealm(false, false, null);
		jenkinsRule.jenkins.setSecurityRealm(hudsonPrivateSecurityRealm); //jenkinsRule.createDummySecurityRealm());
		testUser = hudsonPrivateSecurityRealm.createAccount("test", "test");
		testUserToken = testUser.getProperty(jenkins.security.ApiTokenProperty.class).getApiToken();

		mockAuth.grant(Jenkins.ADMINISTER).everywhere().toAuthenticated();
	}

	@Test
	public void testRemoteBuild() throws Exception {
		disableAuth();
		_testRemoteBuild(false);
	}

	@Test
	public void testRemoteBuildWithAuthentication() throws Exception {
		enableAuth();
		_testRemoteBuild(true);
	}

	@Test
	public void testRemoteBuildWithCrumb() throws Exception {
		disableAuth();
		jenkinsRule.jenkins.setCrumbIssuer(new DefaultCrumbIssuer(false));
		_testRemoteBuild(false);
	}

	private void _testRemoteBuild(boolean authenticate, boolean withParam, FreeStyleProject remoteProject) throws Exception {
		Map<String, String> parms = new HashMap<>();
		parms.put("parameterName1", "value1");
		parms.put("parameterName2", "value2");
		this._testRemoteBuild(authenticate, withParam, remoteProject, parms);
	}

	private void _testRemoteBuild(boolean authenticate, boolean withParam, FreeStyleProject remoteProject, Map<String, String> params) throws Exception {

		String remoteUrl = jenkinsRule.getURL().toString();
		RemoteJenkinsServer remoteJenkinsServer = new RemoteJenkinsServer();
		remoteJenkinsServer.setDisplayName("JENKINS");
		remoteJenkinsServer.setAddress(remoteUrl);
		RemoteBuildConfiguration.DescriptorImpl descriptor =
				jenkinsRule.jenkins.getDescriptorByType(RemoteBuildConfiguration.DescriptorImpl.class);
		descriptor.setRemoteSites(remoteJenkinsServer);

		FreeStyleProject project = jenkinsRule.createFreeStyleProject();
		RemoteBuildConfiguration configuration = new RemoteBuildConfiguration();
		configuration.setJob(remoteProject.getFullName());
		configuration.setRemoteJenkinsName(remoteJenkinsServer.getDisplayName());
		configuration.setPreventRemoteBuildQueue(false);
		configuration.setBlockBuildUntilComplete(true);
		configuration.setPollInterval(1);
		configuration.setHttpGetReadTimeout(1000);
		configuration.setHttpPostReadTimeout(1000);
		configuration.setUseCrumbCache(false);
		configuration.setUseJobInfoCache(false);
		configuration.setEnhancedLogging(true);
		configuration.setTrustAllCertificates(true);
		if (withParam) {
			configuration.setParameters2(new MapParameters(params));
		}
		if (authenticate) {
			TokenAuth tokenAuth = new TokenAuth();
			tokenAuth.setUserName(testUser.getId());
			tokenAuth.setApiToken(Secret.fromString(testUserToken));
			configuration.setAuth2(tokenAuth);
		}

		project.getBuildersList().add(configuration);

		//Trigger build
		jenkinsRule.waitUntilNoActivity();
		jenkinsRule.buildAndAssertSuccess(project);

		//Check results
		FreeStyleBuild lastBuild2 = project.getLastBuild();
		assertNotNull(lastBuild2);
		List<String> log = IOUtils.readLines(lastBuild2.getLogInputStream());
		assertTrue(log.toString(), log.toString().contains("Started by user " + (authenticate ? "test" : "anonymous") + ", Building in workspace"));

		FreeStyleBuild lastBuild = remoteProject.getLastBuild();
		assertNotNull("lastBuild null", lastBuild);
		if (withParam) {
			EnvVars remoteEnv = lastBuild.getEnvironment(new LogTaskListener(null, null));
			for (Map.Entry<String, String> p : params.entrySet()) {
				assertEquals(p.getValue(), remoteEnv.get(p.getKey()));
			}
		} else {
			assertNotEquals("lastBuild should be executed no matter the result which depends on the remote job configuration.", null, lastBuild.getNumber());
		}
	}

	private void _testRemoteBuild(boolean authenticate) throws Exception {
		FreeStyleProject remoteProject = jenkinsRule.createFreeStyleProject();
		remoteProject.addProperty(
				new ParametersDefinitionProperty(new StringParameterDefinition("parameterName1", "default1"),
						new StringParameterDefinition("parameterName2", "default2")));
		_testRemoteBuild(authenticate, true, remoteProject);
	}

	@Test @WithoutJenkins
	public void testDefaults() throws IOException {

		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("job");

		assertFalse(config.getBlockBuildUntilComplete()); //False in Job
		assertFalse(config.getEnhancedLogging());
		assertEquals("job", config.getJob());
		assertFalse(config.getOverrideAuth());
		assertTrue(config.getParameters2() instanceof MapParameters);
		assertEquals(10000, config.getHttpGetReadTimeout());
		assertEquals(30000, config.getHttpPostReadTimeout());
		assertEquals(10, config.getPollInterval(RemoteBuildStatus.RUNNING));
		assertFalse(config.getPreventRemoteBuildQueue());
		assertNull(config.getRemoteJenkinsName());
		assertFalse(config.getShouldNotFailBuild());
		assertFalse(config.getOverrideTrustAllCertificates());
		assertFalse(config.getTrustAllCertificates());
		assertEquals("", config.getToken());
	}

	@Test @WithoutJenkins
	public void testDefaultsPipelineStep() throws IOException {

		RemoteBuildPipelineStep config = new RemoteBuildPipelineStep("job");

		assertTrue(config.getBlockBuildUntilComplete()); //True in Pipeline Step
		assertFalse(config.getEnhancedLogging());
		assertEquals("job", config.getJob());
		assertTrue(config.getAuth() instanceof NullAuth);
		assertTrue(config.getParameters() instanceof MapParameters);
		assertEquals(10000, config.getHttpGetReadTimeout());
		assertEquals(30000, config.getHttpPostReadTimeout());
		assertEquals(10, config.getPollInterval());
		assertFalse(config.getPreventRemoteBuildQueue());
		assertNull(config.getRemoteJenkinsName());
		assertFalse(config.getShouldNotFailBuild());
		assertFalse(config.getOverrideTrustAllCertificates());
		assertFalse(config.getTrustAllCertificates());
		assertEquals("", config.getToken());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withoutServer() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		assertEquals("MyJob", config.getJob());
		try {
			config.evaluateEffectiveRemoteHost(null);
			fail("findRemoteHost() should throw an AbortException since server not specified");
		} catch (AbortException e) {
			assertEquals("Configuration of the remote Jenkins host is missing.", e.getMessage());
		}
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withJobNameAndRemoteUrl() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config.setRemoteJenkinsUrl("http://test:8080");
		assertEquals("MyJob", config.getJob());
		assertEquals("http://test:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withJobNameAndRemoteName() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://test:8080");

		config.setRemoteJenkinsName("remoteJenkinsName");
		assertEquals("MyJob", config.getJob());
		assertEquals("http://test:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withMultiFolderJobNameAndRemoteName() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("A/B/C/D/MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://test:8080");

		config.setRemoteJenkinsName("remoteJenkinsName");
		assertEquals("A/B/C/D/MyJob", config.getJob());
		assertEquals("http://test:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withJobUrl() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("http://test:8080/job/folder/job/MyJob");
		assertEquals("http://test:8080/job/folder/job/MyJob", config.getJob()); //The value configured for "job"
		assertEquals("http://test:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withJobUrlAndRemoteUrl() throws IOException {
		//URL specified for "job" shall override specified remote host
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("http://testA:8080/job/folder/job/MyJobA");
		config.setRemoteJenkinsUrl("http://testB:8080");
		assertEquals("http://testA:8080/job/folder/job/MyJobA", config.getJob()); //The value configured for "job"
		assertEquals("http://testA:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testJobUrlHandling_withJobUrlAndRemoteName() throws IOException {
		//URL specified for "job" shall override global setting
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("http://testA:8080/job/folder/job/MyJobA");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://testB:8080");

		config.setRemoteJenkinsName("remoteJenkinsName");
		assertEquals("http://testA:8080/job/folder/job/MyJobA", config.getJob()); //The value configured for "job"
		assertEquals("http://testA:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_withoutJob() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		try {
			RemoteBuildConfiguration config = new RemoteBuildConfiguration();
			config.setJob("xxx");
			Field field = config.getClass().getDeclaredField("job");
			field.setAccessible(true);
			field.set(config, "");
			config.evaluateEffectiveRemoteHost(null);
			fail("findRemoteHost() should throw an AbortException since job not specified");
		} catch (AbortException e) {
			assertEquals("Parameter 'Remote Job Name or URL' ('job' variable in Pipeline) not specified.", e.getMessage());
		}
	}

	@Test @WithoutJenkins
	public void testRemoteUrlOverridesRemoteName() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://globallyConfigured:8080");

		config.setRemoteJenkinsName("remoteJenkinsName");
		assertEquals("http://globallyConfigured:8080", config.evaluateEffectiveRemoteHost(null).getAddress());

		//Now override remote host URL
		config.setRemoteJenkinsUrl("http://locallyOverridden:8080");
		assertEquals("MyJob", config.getJob());
		assertEquals("http://locallyOverridden:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	/**
	 * Testing if it is possible to set the TrustAllCertificates-parameter with OverrideTrustAllCertificates set to true
	 *
	 * @throws IOException
	 */
	@Test @WithoutJenkins
	public void testRemoteOverridesTrustAllCertificates() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config.setTrustAllCertificates(false);
		config.setOverrideTrustAllCertificates(true);

		config = mockGlobalRemoteHost(config,
				"remoteJenkinsName",
				"http://globallyConfigured:8080",
				true);

		config.setRemoteJenkinsName("remoteJenkinsName");
		assertTrue(config.getTrustAllCertificates());
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_jobNameMissing() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		try {
			config.evaluateEffectiveRemoteHost(null);
		} catch (final AbortException e) {
			assertEquals("Parameter 'Remote Job Name or URL' ('job' variable in Pipeline) not specified.", e.getMessage());
		}
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_globalConfigMissing() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://globallyConfigured:8080");

		config.setRemoteJenkinsName("notConfiguredRemoteHost");
		try {
			config.evaluateEffectiveRemoteHost(null);
		} catch (AbortException e) {
			assertEquals("Could get remote host with ID 'notConfiguredRemoteHost' configured in Jenkins global configuration. Please check your global configuration.", e.getMessage());
		}
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_globalConfigMissing_localOverrideHostURL() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://globallyConfigured:8080");

		config.setRemoteJenkinsName("notConfiguredRemoteHost");
		config.setRemoteJenkinsUrl("http://locallyOverridden:8080");
		assertEquals("http://locallyOverridden:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_globalConfigMissing_localOverrideJobURL() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("http://localJobUrl:8080/job/MyJob");
		config = mockGlobalRemoteHost(config, "remoteJenkinsName", "http://globallyConfigured:8080");

		config.setRemoteJenkinsName("notConfiguredRemoteHost");
		assertEquals("http://localJobUrl:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_localOverrideHostURL() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config.setRemoteJenkinsUrl("http://hostname:8080");
		assertEquals("http://hostname:8080", config.evaluateEffectiveRemoteHost(null).getAddress());
	}

	@Test @WithoutJenkins
	public void testEvaluateEffectiveRemoteHost_localOverrideHostURLWrong() throws IOException {
		RemoteBuildConfiguration config = new RemoteBuildConfiguration();
		config.setJob("MyJob");
		config.setRemoteJenkinsUrl("hostname:8080");
		try {
			config.evaluateEffectiveRemoteHost(null);
			fail("Expected AbortException");
		} catch (AbortException e) {
			assertEquals("The 'Override remote host URL' parameter value (remoteJenkinsUrl: 'hostname:8080') is no valid URL", e.getMessage());
		}
	}

	private RemoteBuildConfiguration mockGlobalRemoteHost(RemoteBuildConfiguration config, String remoteName, String remoteUrl) throws MalformedURLException {
		RemoteJenkinsServer jenkinsServer = new RemoteJenkinsServer();
		jenkinsServer.setDisplayName(remoteName);
		jenkinsServer.setAddress(remoteUrl);

		RemoteBuildConfiguration spy = spy(config);
		DescriptorImpl descriptor = DescriptorImpl.newInstanceForTests();
		descriptor.setRemoteSites(jenkinsServer);
		doReturn(descriptor).when(spy).getDescriptor();

		return spy;
	}

	private RemoteBuildConfiguration mockGlobalRemoteHost(
			RemoteBuildConfiguration config, String remoteName, String remoteUrl, boolean trustAllCertificates
	) throws MalformedURLException {
		RemoteJenkinsServer jenkinsServer = new RemoteJenkinsServer();
		jenkinsServer.setDisplayName(remoteName);
		jenkinsServer.setAddress(remoteUrl);

		RemoteBuildConfiguration spy = spy(config);
		DescriptorImpl descriptor = DescriptorImpl.newInstanceForTests();
		descriptor.setRemoteSites(jenkinsServer);
		doReturn(descriptor).when(spy).getDescriptor();
		spy.setTrustAllCertificates(trustAllCertificates);

		return spy;
	}

	@Test @WithoutJenkins
	public void testGenerateEffectiveRemoteBuildURL() throws Exception {
		URL remoteBuildURL = new URL("http://test:8080/job/Abc/3/");

		assertEquals(new URL("https://foobar:8443/job/Abc/3/"), RemoteBuildConfiguration.generateEffectiveRemoteBuildURL(remoteBuildURL, "https://foobar:8443"));
		assertEquals(new URL("http://foobar:8888/job/Abc/3/"), RemoteBuildConfiguration.generateEffectiveRemoteBuildURL(remoteBuildURL, "http://foobar:8888"));
		assertEquals(new URL("https://foobar/job/Abc/3/"), RemoteBuildConfiguration.generateEffectiveRemoteBuildURL(remoteBuildURL, "https://foobar"));
		assertEquals(new URL("http://foobar/job/Abc/3/"), RemoteBuildConfiguration.generateEffectiveRemoteBuildURL(remoteBuildURL, "http://foobar"));
	}

	@Test @WithoutJenkins
	public void testGenerateJobUrl() throws MalformedURLException, AbortException {
		RemoteJenkinsServer remoteServer = new RemoteJenkinsServer();
		remoteServer.setAddress("https://server:8080/jenkins");

		assertEquals("https://server:8080/jenkins/job/JobName", RemoteBuildConfiguration.generateJobUrl(remoteServer, "JobName"));
		assertEquals("https://server:8080/jenkins/job/Folder/job/JobName", RemoteBuildConfiguration.generateJobUrl(remoteServer, "Folder/JobName"));
		assertEquals("https://server:8080/jenkins/job/More/job/than/job/one/job/folder", RemoteBuildConfiguration.generateJobUrl(remoteServer, "More/than/one/folder"));
		try {
			RemoteBuildConfiguration.generateJobUrl(remoteServer, "");
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) { }
		try {
			RemoteBuildConfiguration.generateJobUrl(remoteServer, null);
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) { }
		try {
			RemoteBuildConfiguration.generateJobUrl(null, "JobName");
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException e) { }

		//Test trailing slash
		remoteServer.setAddress("https://server:8080/jenkins/");
		assertEquals("https://server:8080/jenkins/job/JobName", RemoteBuildConfiguration.generateJobUrl(remoteServer, "JobName"));

		try {
			RemoteJenkinsServer missingUrl = new RemoteJenkinsServer();
			RemoteBuildConfiguration.generateJobUrl(missingUrl, "JobName");
			Assert.fail("Expected AbortException");
		} catch (AbortException e) { }

	}

	@Test
	public void testRemoteFolderedBuild() throws Exception {
		disableAuth();

		MockFolder remoteJobFolder = jenkinsRule.createFolder("someJobFolder");
		FreeStyleProject remoteProject = remoteJobFolder.createProject(FreeStyleProject.class, "someJobName");
		remoteProject.addProperty(
				new ParametersDefinitionProperty(new StringParameterDefinition("parameterName1", "default1"),
						new StringParameterDefinition("parameterName2", "default2")));

		this._testRemoteBuild(false, true, remoteProject);
	}

	@Test
	public void testRemoteFolderedBuildWithoutParameters() throws Exception {
		disableAuth();

		MockFolder remoteJobFolder = jenkinsRule.createFolder("someJobFolder1");
		FreeStyleProject remoteProject = remoteJobFolder.createProject(FreeStyleProject.class, "someJobName1");
		this._testRemoteBuild(false, false, remoteProject);
	}

	@Test
	public void testRemoteBuildWith5KByteString() throws Exception {
		enableAuth();
		FreeStyleProject remoteProject = jenkinsRule.createFreeStyleProject();
		remoteProject.addProperty(
				new ParametersDefinitionProperty(new StringParameterDefinition("parameterName1", "default1"),
						new StringParameterDefinition("parameterName2", "default2")));
		Map<String, String> params = new HashMap<>();
		params.put("parameterName1", TestConst.garbled5KString1);
		params.put("parameterName2", TestConst.garbled5KString2);
		this._testRemoteBuild(true, true, remoteProject, params);
	}

	@Test
	public void testRemoteViewBuild() throws Exception {
		disableAuth();

		FreeStyleProject remoteProject = jenkinsRule.createFreeStyleProject("test-job");
		ListView view = new ListView("test-view", jenkinsRule.getInstance());
		view.add(remoteProject);
		jenkinsRule.getInstance().addView(view);
		_testRemoteBuild(false, false, remoteProject);
	}

	@Test @WithoutJenkins
	public void testParseStringParameters() {
		final String parameters = join("\n", asList(
				"# bla bla",       // Comment
				"",                // Empty line
				"   ",             // Empty line (if trimmed)
				"PARAM1=toto",     // Simple parameter
				"PARAM2=dG90bwo=", // Parameter with = sign (see https://issues.jenkins.io/browse/JENKINS-58818)
				"PARAM3=line1",    // Multi-line parameter
				"line2",           // Multi-line parameter
				"line3"            // Multi-line parameter
		));
		final Map<String, String> actualParametersMap = JobParameters.parseStringParameters(parameters);

		final Map<String, String> expectedParametersMap = new HashMap<>();
		expectedParametersMap.put("PARAM1", "toto");
		expectedParametersMap.put("PARAM2", "dG90bwo="); // This one should fail because it's broken (see issue 58818)
		expectedParametersMap.put("PARAM3", "line1"); // We keep the wrong value in the test because it's unfixable with string parameters

		assertEquals(expectedParametersMap, actualParametersMap);
	}

}
