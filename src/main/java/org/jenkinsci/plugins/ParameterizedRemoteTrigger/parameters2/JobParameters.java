package org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class JobParameters extends AbstractDescribableImpl<JobParameters> implements Serializable, Cloneable {

	private static final DescriptorExtensionList<JobParameters, ParametersDescriptor> ALL =
			DescriptorExtensionList.createDescriptorList(Jenkins.getInstance(), JobParameters.class);

	public static DescriptorExtensionList<JobParameters, ParametersDescriptor> all() {
		return ALL;
	}

	public static JobParameters migrateOldParameters(final String parameters, final String parameterFile) {
		if (!isNullOrEmpty(parameterFile)) {
			return new FileParameters(parameterFile);
		}

		if (!isNullOrEmpty(parameters)) {
			return new StringParameters(parameters);
		}

		return new MapParameters();
	}

	public static Map<String, String> parseStringParameters(final String parametersAsString) {
		return Arrays.stream(parametersAsString.split("\\n"))
				.filter(not(JobParameters::isBlankLine))
				.filter(not(JobParameters::isCommentedLine))
				.filter(JobParameters::containsEqualSign)
				.map(JobParameters::splitParameterLine)
				.collect(toMap(Entry::getKey, Entry::getValue));
	}

	private static <T> Predicate<T> not(Predicate<T> t) {
		return t.negate();
	}

	private static boolean isBlankLine(String line) {
		return line.trim().isEmpty();
	}

	private static boolean isCommentedLine(String line) {
		return line.trim().startsWith("#");
	}

	private static boolean containsEqualSign(String line) {
		return line.contains("=");
	}

	private static Entry<String, String> splitParameterLine(String line) {
		final int firstIndexOfEqualSign = line.indexOf("=");
		return new AbstractMap.SimpleEntry<>(
				line.substring(0, firstIndexOfEqualSign),
				line.substring(firstIndexOfEqualSign + 1)
		);
	}

	public static abstract class ParametersDescriptor extends Descriptor<JobParameters> { }

	public abstract Map<String, String> getParametersMap(final BuildContext context) throws AbortException;

	@Override
	public JobParameters clone() throws CloneNotSupportedException {
		return (JobParameters) super.clone();
	}
}
