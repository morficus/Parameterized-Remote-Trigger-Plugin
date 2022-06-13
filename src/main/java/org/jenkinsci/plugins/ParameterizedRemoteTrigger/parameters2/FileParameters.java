package org.jenkinsci.plugins.ParameterizedRemoteTrigger.parameters2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BuildContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;

public class FileParameters extends JobParameters {

	private static final long serialVersionUID = 3614172320192170597L;

	@Extension(ordinal = 0)
	public static final FileParametersDescriptor DESCRIPTOR = new FileParametersDescriptor();

	private String filePath;

	@DataBoundConstructor
	public FileParameters() {
		this.filePath = null;
	}

	public FileParameters(String filePath) {
		this.filePath = filePath;
	}

	@DataBoundSetter
	public void setFilePath(final String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return filePath;
	}

	@Override
	public String toString() {
		return "(" + getClass().getSimpleName() + ") " + filePath;
	}

	@Override
	public FileParametersDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public Map<String, String> getParametersMap(final BuildContext context) throws AbortException {
		final String parametersAsString = readParametersFile(context);
		return JobParameters.parseStringParameters(parametersAsString);
	}

	private String readParametersFile(final BuildContext context) throws AbortException {
		if (context.workspace == null) {
			throw new AbortException("Workspace is null but parameter file is used. Looks like this step was started with \"agent: none\"");
		}

		BufferedReader reader = null;
		try {
			final FilePath absoluteFilePath = context.workspace.child(getFilePath());
			context.logger.printf("Loading parameters from file %s%n", absoluteFilePath.getRemote());

			reader = new BufferedReader(new InputStreamReader(absoluteFilePath.read(), UTF_8));
			return reader.lines().collect(joining("\n"));

		} catch (final InterruptedException | IOException e) {
			context.logger.printf("[WARNING] Failed loading parameters: %s%n", e.getMessage());
			return "";

		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@Symbol("FileParameters")
	public static class FileParametersDescriptor extends ParametersDescriptor {
		@Nonnull
		@Override
		public String getDisplayName() {
			return "File parameters";
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
		final FileParameters that = (FileParameters) o;
		return Objects.equals(filePath, that.filePath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(filePath);
	}
}
