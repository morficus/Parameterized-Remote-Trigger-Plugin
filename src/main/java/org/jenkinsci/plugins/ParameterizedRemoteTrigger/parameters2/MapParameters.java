package org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2;

import static java.util.stream.Collectors.toMap;

import edu.umd.cs.findbugs.annotations.NonNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.jboss.marshalling.util.IntKeyMap;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;

public class MapParameters extends JobParameters {

	private static final long serialVersionUID = 3614172320192170597L;

	@Extension(ordinal = 2)
	public static final MapParametersDescriptor DESCRIPTOR = new MapParametersDescriptor();

	private final List<MapParameter> parameters = new ArrayList<>();

	@DataBoundConstructor
	public MapParameters() { }

	public MapParameters(@NonNull Map<String, String> parametersMap) {
		setParametersMap(parametersMap);
	}

	@DataBoundSetter
	public void setParameters(final List<MapParameter> parameters) {
		this.parameters.clear();
		if (parameters != null) {
			this.parameters.addAll(parameters);
		}
	}

	public void setParametersMap(final Map<String, String> parametersMap) {
		this.parameters.clear();
		if (parametersMap != null) {
			parametersMap
					.entrySet()
					.stream()
					.map(entry -> new MapParameter(entry.getKey(), entry.getValue()))
					.forEach(parameters::add);
		}
	}

	public List<MapParameter> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		return "(" + getClass().getSimpleName() + ") " + parameters;
	}

	@Override
	public MapParametersDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public Map<String, String> getParametersMap(final BuildContext context) {
		return parameters
				.stream()
				.collect(toMap(MapParameter::getName, MapParameter::getValue));
	}

	@Symbol("MapParameters")
	public static class MapParametersDescriptor extends ParametersDescriptor {
		@Nonnull
		@Override
		public String getDisplayName() {
			return "Map parameters";
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
		final MapParameters that = (MapParameters) o;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameters);
	}
}
