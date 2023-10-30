package org.infrastructurebuilder.maven;

import static java.nio.file.Files.newOutputStream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.infrastructurebuilder.maven.IBVersionsUtils.copyTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.vdurmont.semver4j.Semver;

abstract public class GeneratorComponent extends AbstractLogEnabled {

  private static final String CLASS_FROM_PROJECT_ARTIFACT_ID = "classFromProjectArtifactId";
  private static final String SNAPSHOT = "-snapshot";
  /*
   * The working directory Where templates will be copied from the classpath or overridden locations
   */
  private Path workDirectory;
  /*
   * The target output directory This directory will be added as a source or test source, depending on type
   */
  private Path outputDirectory;
  private MavenSession session;
  private MavenProject project;
  private MavenResourcesFiltering mavenResourcesFiltering;
  private BuildContext buildContext;
  /*
   * You can, but shouldn't, override the generated class name
   */
  private String overriddenGeneratedClassName;
  /*
   * You can, but shouldn't, override the template file
   */
  private Path overriddenTemplateFile;
  private String apiVersionPropertyName;
  private String encoding = "UTF-8";
  private String escapeString = "\\";
  private int copied = 0;

  public void execute() throws IOException {

    if (this.apiVersionPropertyName != null && !StringUtils.isBlank(this.apiVersionPropertyName)) {
      Semver s = new Semver(project.getVersion());
      project.getProperties().setProperty(apiVersionPropertyName, s.getMajor() + "." + s.getMinor());
    }

    final Path workDirectory = getWorkDirectory();

    if ("pom".equals(project.getPackaging())) {
      logInfo("Skipping a POM project type.");
      return;
    }
    logDebug("source=%s target=%s", workDirectory, getOutputDirectory());

    final Path p = getResourcePath();

    if (!Files.exists(p))
      throw new IOException("Path " + p.toAbsolutePath() + " does not exist");

    buildContext.removeMessages(workDirectory.toFile());

    copied = 0;

    project.getProperties().put(CLASS_FROM_PROJECT_ARTIFACT_ID, getClassNameFromArtifactId());

    final Path temporaryDirectory = getTemporaryDirectory(workDirectory);

    logInfo("Copying files with filtering to temporary directory %s.", temporaryDirectory);

    filterSourceToTemporaryDir(workDirectory, temporaryDirectory);

    try {
      copied += copyTree(temporaryDirectory, getOutputDirectory(), getLogger());
    } catch (final IOException e) {
      throw new IOException("Failed to copy directory struct", e);
    }
//    FileUtils.deleteDirectory(temporaryDirectory);
    if (isSomethingBeenUpdated()) {
      buildContext.refresh(getOutputDirectory().toFile());
      logInfo("Copied %d files to output directory: %s", copied, getOutputDirectory());
    } else {
      logInfo("No files needs to be copied to output directory. Up to date: %s", getOutputDirectory());
    }
    project.getProperties().remove(CLASS_FROM_PROJECT_ARTIFACT_ID);
    addSourceFolderToProject(project);
    logInfo("Source directory: %s added.", getOutputDirectory());
  }

  protected Path getResourcePath() throws IOException {
    Path wp = this.workDirectory.toAbsolutePath();
    final Path filePath = Paths //
        .get(wp.toString(), requireNonNull(project.getGroupId()).split("\\.")) // source directory/the/group/id/expanded
        .resolve((isTestGeneration() ? "Test" : "") + getClassNameFromArtifactId() + "." + getType()); // final filename
    final Path templatePath = wp.resolve(filePath).toAbsolutePath();
    logInfo("writing template to " + templatePath.toAbsolutePath());

    try {
      final Path parents = templatePath.getParent();
      if (!Files.exists(parents)) {
        logDebug("Creating " + parents);
        Files.createDirectories(parents);
      }
      if (overriddenTemplateFile != null) {
        logDebug("Copying overriden template file " + overriddenTemplateFile + " to " + templatePath);
        FileUtils.copyFile(overriddenTemplateFile.toFile(), templatePath.toFile());
      } else {
        final String rPath = getTemplatesDir() + "template";
        getLogger().info("Target path for copied resource is " + templatePath);
        try (InputStream res = getClass().getResourceAsStream(rPath); OutputStream os = newOutputStream(templatePath)) {
          FileUtils.copyStreamToFile(new RawInputStreamFacade(res), templatePath.toFile());
        }
      }
    } catch (final IOException e) {
      throw new IOException("Failed to copy files", e);
    }
    return templatePath;
  }

  protected int countCopiedFiles() {
    return copied;
  }

  protected String getClassNameFromArtifactId() {
    if (overriddenGeneratedClassName != null)
      return overriddenGeneratedClassName;

    String ver = project.getVersion();
    if (ver.toLowerCase().endsWith(SNAPSHOT))
      ver = ver.substring(0, ver.length() - SNAPSHOT.length()); // Remove snapshotting
    final String nonJavaMethodName = project.getArtifactId();
    final StringBuilder nameBuilder = new StringBuilder();
    boolean capitalizeNextChar = true;
    boolean first = true;

    for (int i = 0; i < nonJavaMethodName.length(); i++) {
      char c = nonJavaMethodName.charAt(i);
      if (c == '.')
        c = '_';
      if (c != '_' && !Character.isLetterOrDigit(c)) {
        if (!first) {
          capitalizeNextChar = true;
        }
      } else {
        nameBuilder.append(capitalizeNextChar ? Character.toUpperCase(c) : Character.toLowerCase(c));
        capitalizeNextChar = false;
        first = false;
      }
    }

    nameBuilder.append("Versioning");
    return nameBuilder.toString();

  }

  private void filterSourceToTemporaryDir(final Path workingDirectory, final Path temporaryDirectory)
      throws IOException {
    final List<Resource> resources = new ArrayList<Resource>();
    final Resource resource = new Resource();
    resource.setFiltering(true);
    logDebug("Source absolute path: %s", workingDirectory.toAbsolutePath());
    resource.setDirectory(workingDirectory.toAbsolutePath().toString());
    resources.add(resource);

    final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resources,
        temporaryDirectory.toFile(), project, encoding, emptyList(), emptyList(), session);
    mavenResourcesExecution.setInjectProjectBuildFilters(true);
    mavenResourcesExecution.setEscapeString(escapeString);
    mavenResourcesExecution.setOverwrite(true);
    final LinkedHashSet<String> delims = new LinkedHashSet<String>();
    delims.add("@");
    mavenResourcesExecution.setDelimiters(delims);
    try {
      mavenResourcesFiltering.filterResources(mavenResourcesExecution);
    } catch (final MavenFilteringException e) {
      buildContext.addMessage(getWorkDirectory().toFile(), 1, 1, "Filtering Exception", BuildContext.SEVERITY_ERROR, e);
      throw new IOException(e.getMessage(), e);
    }
  }

  private Path getWorkDirectory() throws IOException {
    final Path wd = this.workDirectory.toAbsolutePath();
    if (wd == null)
      throw new IOException("Null work directory");
    if (!Files.exists(wd)) {
      try {
        Files.createDirectories(wd);
      } catch (final IOException e) {
        throw new IOException("Failed to create  " + wd, e);
      }
    }
    return wd;
  }

  private Path getTemporaryDirectory(final Path sourceDirectory) throws IOException {
    final Path basedir = project.getBasedir().toPath();
    Path target = Paths.get(project.getBuild().getDirectory());
    target = target.isAbsolute() ? target : basedir.resolve(target);
    try {
      return Files.createTempDirectory(target, "templates-tmp");
    } catch (IOException e) {
      throw new IOException("Cannot create temp dir", e);
    }
  }

  private boolean isSomethingBeenUpdated() {
    return copied > 0;
  }

  private void logDebug(final String format, final Object... args) {
    if (getLogger().isDebugEnabled()) {
      getLogger().debug(String.format(format, args));
    }
  }

  private void logInfo(final String format, final Object... args) {
    if (getLogger().isInfoEnabled()) {
      getLogger().info(String.format(format, args));
    }
  }

  protected String getType() {
    return "java";
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public void setBuildContext(BuildContext buildContext) {
    this.buildContext = buildContext;
  }

  public void setSession(MavenSession session2) {
    this.session = session2;
  }

  public Path getOutputDirectory() {
    return outputDirectory;
  }

  public void setOverriddenGeneratedClassName(final String overriddenGeneratedClassName) {
    this.overriddenGeneratedClassName = overriddenGeneratedClassName;
  }

  protected boolean isTestGeneration() {
    return false;
  }

  public void setMavenResourcesFiltering(MavenResourcesFiltering mavenResourcesFiltering) {
    this.mavenResourcesFiltering = requireNonNull(mavenResourcesFiltering);
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setApiVersionPropertyName(String apiVersionPropertyName) {
    this.apiVersionPropertyName = apiVersionPropertyName;
  }

  public void setOverriddenTemplateFile(Path overriddenTemplateFile) throws IOException {
    if (overriddenTemplateFile == null) {
      this.overriddenTemplateFile = null;
      return;
    }
    Path ot = overriddenTemplateFile.toAbsolutePath();
    if (!Files.isRegularFile(ot))
      throw new IOException("Overriden template file " + ot + " is not a regular file");
    this.overriddenTemplateFile = ot;

  }

  public void setOutputDirectory(Path outputDirectory) throws IOException {
    logInfo("Setting outputDirectory to " + outputDirectory.toAbsolutePath().toString());
    this.outputDirectory = outputDirectory.toAbsolutePath();
    Files.createDirectories(this.outputDirectory);
  }

  public void setWorkDirectory(Path workDirectory) throws IOException {
    this.workDirectory = requireNonNull(workDirectory, "Work directory cannot be null").toAbsolutePath();
    Files.createDirectories(this.workDirectory);
  }

  @Override
  @Inject
  public void enableLogging(Logger theLogger) {
    super.enableLogging(requireNonNull(theLogger));
  }

  protected abstract void addSourceFolderToProject(MavenProject mavenProject);

  protected String getTemplatesDir() {
    return "/" + getType() + "/" + (isTestGeneration() ? "test-" : "") + "templates/";
  }
}
