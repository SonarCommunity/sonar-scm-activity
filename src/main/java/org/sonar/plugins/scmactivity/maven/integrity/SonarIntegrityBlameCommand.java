/*
 * SonarQube SCM Activity Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.scmactivity.maven.integrity;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.integrity.command.blame.IntegrityBlameCommand;
import org.apache.maven.scm.provider.integrity.command.blame.IntegrityBlameConsumer;
import org.apache.maven.scm.provider.integrity.repository.IntegrityScmProviderRepository;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import com.google.common.base.Strings;

/**
 * Overriding the default blame command.<br>
 * <b>Changes:</b> Ensure integrity client connection in current shell environment.
 *
 *
 * @Todo hack - to be submitted as an update in maven-scm-provider-integrity for a future release.
 * @Todo Open issue in maven-scm-provider-integrity.
 * @since 1.6.1
 */
public class SonarIntegrityBlameCommand extends IntegrityBlameCommand {
  
  private static final String SYS_INTEGRITY_ROOT_DIR = "sonar.scm.integrity.root.dir";
  private static final String SYS_JENKINS_WORKSPACE_DIR = "WORKSPACE"; 

  /**
   * {@inheritDoc}
   */
  @Override
  public BlameScmResult executeBlameCommand(ScmProviderRepository repository, ScmFileSet workingDirectory,
    String filename)
    throws ScmException
  {
    getLogger().info("Attempting to display blame results for file: " + filename);
    if (null == filename || filename.length() == 0)
    {
      throw new ScmException("A single filename is required to execute the blame command!");
    }
    BlameScmResult result;
    IntegrityScmProviderRepository iRepo = (IntegrityScmProviderRepository) repository;
    // Since the si annotate command is not completely API ready, we will use the CLI for this command
    // Ensure shell 'si' client is connected.
    doShellConnect(iRepo, workingDirectory);
    result = doShellAnnotate(iRepo, workingDirectory, filename);

    return result;
  }

  /**
   * Execute 'si connect' command in current shell.
   * @param iRepo the Integrity repository instance.
   * @param workingDirectory the SCM working directory.
   * @throws ScmException if connect command failed.
   */
  private void doShellConnect(IntegrityScmProviderRepository iRepo, ScmFileSet workingDirectory)
    throws ScmException
  {
    Commandline shell = new Commandline();
    shell.setWorkingDirectory(workingDirectory.getBasedir());
    shell.setExecutable("si");
    shell.createArg().setValue("connect");
    shell.createArg().setValue("--hostname=" + iRepo.getHost());
    shell.createArg().setValue("--port=" + iRepo.getPort());
    shell.createArg().setValue("--user=" + iRepo.getUser());
    shell.createArg().setValue("--batch");
    shell.createArg().setValue("--password=" + iRepo.getPassword());
    CommandLineUtils.StringStreamConsumer shellConsumer = new CommandLineUtils.StringStreamConsumer();

    try
    {
      getLogger().debug("Executing: " + shadowPasswordArgument(CommandLineUtils.toString(shell.getCommandline())));
      int exitCode = CommandLineUtils.executeCommandLine(shell, shellConsumer, shellConsumer);
      if (exitCode != 0)
      {
				throw new ScmException(String.format("Can't login to integrity. Exite code: '%d'. Message : '%s'"
						, exitCode, shellConsumer.getOutput()));
      }
    } catch (CommandLineException cle)
    {
      getLogger().error("Command Line Connect Exception: " + cle.getMessage());
      throw new ScmException("Can't login to integrity. Message : " + cle.getMessage());
    }

  }

  private String shadowPasswordArgument(String commandline) {
    return commandline.replaceAll("--password=.*($|\\s)", "--password=********* ");
  }

  /**
   * Execute 'si annotate' command in current shell and process output as {@link BlameScmResult} instance.
   * @param iRepo the Integrity repository instance.
   * @param workingDirectory the SCM working directory.
   * @param filename the file name.
   * @return the {@link BlameScmResult} instance.
   * @throws ScmException 
   */
  private BlameScmResult doShellAnnotate(IntegrityScmProviderRepository iRepo, ScmFileSet workingDirectory,
    String filename) throws ScmException
  {
    BlameScmResult result;
    Commandline shell = new Commandline();
    shell.setWorkingDirectory(workingDirectory.getBasedir());
    shell.setExecutable("si");
    shell.createArg().setValue("annotate");
    shell.createArg().setValue("--hostname=" + iRepo.getHost());
    shell.createArg().setValue("--port=" + iRepo.getPort());
    shell.createArg().setValue("--user=" + iRepo.getUser());
    shell.createArg().setValue("--batch");
    if(!existProjectFile(workingDirectory.getBasedir())) {
    	getLogger().debug("Project pj file doesn't exists.");
    	final String projectUrl = buildProjectUrl(workingDirectory.getBasedir(), iRepo);
    	getLogger().debug("Computed project url: " + projectUrl);
    	shell.createArg().setValue("--project=" + projectUrl);
    }
    shell.createArg().setValue("--fields=date,revision,author");
    shell.createArg().setValue('"' + filename + '"');
    IntegrityBlameConsumer shellConsumer = new IntegrityBlameConsumer(getLogger());

    try
    {
      getLogger().debug("Executing: " + CommandLineUtils.toString(shell.getCommandline()));
      int exitCode = CommandLineUtils.executeCommandLine(shell, shellConsumer,
        new CommandLineUtils.StringStreamConsumer());
      boolean success = (exitCode == 0 ? true : false);
      ScmResult scmResult =
        new ScmResult(shell.getCommandline().toString(), "", "Exit Code: " + exitCode, success);
      return new BlameScmResult(shellConsumer.getBlameList(), scmResult);
    } catch (CommandLineException cle)
    {
      getLogger().error("Command Line Exception: " + cle.getMessage());
      result = new BlameScmResult(shell.getCommandline().toString(), cle.getMessage(), "", false);
    }

    return result;
  }

	/**
	 * Build project URL.
	 * @param basedir
	 * @param iRepo
	 * @return
	 * @throws ScmException 
	 */
	private String buildProjectUrl(File basedir, IntegrityScmProviderRepository iRepo) throws ScmException {
		String projectConfigPath = Strings.nullToEmpty(iRepo.getConfigruationPath());
		projectConfigPath = StringUtils.removeEnd(projectConfigPath, "/");
		String projectUrl = "";
		try {
			String integrityRootDir = getIntegrityRootDirectory();
			String baseDirStr = basedir.getCanonicalPath();
			getLogger()
					.debug(String
							.format("The project config path '%s', current scm base dir '%s' and Integrity root directory '%s'.",
									projectConfigPath, baseDirStr, integrityRootDir));
			integrityRootDir = integrityRootDir.replaceAll("\\\\", "/");
			baseDirStr = baseDirStr.replaceAll("\\\\", "/");
			if (!StringUtils.startsWithIgnoreCase(baseDirStr, integrityRootDir)) {
				throw new ScmException(String.format("The Integrity root directory '%s' isn't correctly identified.",
						integrityRootDir));
			}
			String relativDir = baseDirStr.substring(integrityRootDir.length());
			relativDir = StringUtils.startsWith(relativDir, "/") ? relativDir : "/" + relativDir;
			getLogger().debug("The project relativ dir: " + relativDir);
			projectUrl = projectConfigPath + relativDir;
		} catch (IOException e) {
			getLogger().error("Cannot get cannoical path for base directory.", e);
			throw new ScmException("Cannot get cannoical path for basedir: " + e.getMessage());
		}

		return projectUrl;
	}

	/**
	 * @return The resolved directory path of system variable '{@value #SYS_INTEGRITY_ROOT_DIR}', system variable value
	 *         '{@value #SYS_JENKINS_WORKSPACE_DIR}' or current working directory.
	 * @throws ScmException
	 */
	private String getIntegrityRootDirectory() throws ScmException {
		try {
			final String integrityRootDir;
			final String sysIntegrityRootDir = System.getenv(SYS_INTEGRITY_ROOT_DIR);
			final String sysJenkinsWorksapceDir = System.getenv(SYS_JENKINS_WORKSPACE_DIR);
			if (sysIntegrityRootDir != null) {
				getLogger().debug(
						String.format("The system integrity root directory property provided value is: '%s'",
								sysIntegrityRootDir));
				integrityRootDir = new File(sysIntegrityRootDir).getCanonicalPath();
			} else if (sysJenkinsWorksapceDir != null) {
				getLogger().debug(
						String.format("The Jenkins worksapce directory property provided value is: '%s'",
								sysJenkinsWorksapceDir));
				integrityRootDir = sysJenkinsWorksapceDir;
			} else {
				getLogger().debug("The system integrity root directory not property provided.");
				integrityRootDir = Strings.nullToEmpty(System.getProperty("user.dir"));
			}
			return integrityRootDir;
		} catch (IOException e) {
			getLogger().error("Cannot get cannoical path for integrity root directory.", e);
			throw new ScmException("Cannot get cannoical path for integrity root directory: " + e.getMessage());
		}
	}
	
	/**
	 * Check if exists "project.pj" file in provide directory.
	 * @param basedir the directory.
	 * @return <code>true</code> if "project.pj" exists, otherwise <code>false</code>.
	 */
	private boolean existProjectFile(File basedir) {
		final File projectPj = new File(basedir, "project.pj");
		return projectPj.exists();
	}
	
}
