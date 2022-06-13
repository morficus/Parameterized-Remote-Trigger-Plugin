package org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;

public class StringParameters extends JobParameters {

	private static final long serialVersionUID = 3614172320192170597L;

	@Extension(ordinal = 1)
	public static final StringParametersDescriptor DESCRIPTOR = new StringParametersDescriptor();

	private String parameters;

	@DataBoundConstructor
	public StringParameters() {
		this.parameters = null;
	}

	public StringParameters(String parameters) {
		this.parameters = parameters;
	}

	@DataBoundSetter
	public void setParameters(final String parameters) {
		this.parameters = parameters;
	}

	public String getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return "(" + getClass().getSimpleName() + ") " + parameters;
	}

	@Override
	public StringParametersDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public Map<String, String> getParametersMap(final BuildContext context) {
		return JobParameters.parseStringParameters(parameters);
	}

	@Symbol("StringParameters")
	public static class StringParametersDescriptor extends ParametersDescriptor {
		@Nonnull
		@Override
		public String getDisplayName() {
			return "String parameters";
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final StringParameters that = (StringParameters) o;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameters);
	}
}
