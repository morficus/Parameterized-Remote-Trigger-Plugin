package org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class MapParameter extends AbstractDescribableImpl<MapParameter> implements Cloneable, Serializable {

	@Extension
	public static final MapParameterDescriptor DESCRIPTOR = new MapParameterDescriptor();

	private String name;
	private String value;

	@DataBoundConstructor
	public MapParameter() {
		this("", "");
	}

	public MapParameter(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@DataBoundSetter
	public void setName(String name) {
		this.name = name;
	}

	@DataBoundSetter
	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public MapParameter clone() throws CloneNotSupportedException {
		return (MapParameter) super.clone();
	}

	@Override
	public Descriptor<MapParameter> getDescriptor() {
		return DESCRIPTOR;
	}

	@Symbol("MapParameter")
	public static class MapParameterDescriptor extends Descriptor<MapParameter> {
		@Nonnull
		@Override
		public String getDisplayName() {
			return "Map parameter";
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
		final MapParameter that = (MapParameter) o;
		return Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}
}
