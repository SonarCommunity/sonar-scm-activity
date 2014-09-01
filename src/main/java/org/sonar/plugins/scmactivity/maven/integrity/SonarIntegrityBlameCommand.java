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
      getLogger().debug("Executing: " + CommandLineUtils.toString(shell.getCommandline()));
      int exitCode = CommandLineUtils.executeCommandLine(shell, shellConsumer, shellConsumer);
      if (exitCode != 0)
      {
        throw new ScmException("Can't login to integrity. Message : " + shellConsumer.toString());
      }
    } catch (CommandLineException cle)
    {
      getLogger().error("Command Line Connect Exception: " + cle.getMessage());
      throw new ScmException("Can't login to integrity. Message : " + cle.getMessage());
    }

  }

  /**
   * Execute 'si annotate' command in current shell and process output as {@link BlameScmResult} instance.
   * @param iRepo the Integrity repository instance.
   * @param workingDirectory the SCM working directory.
   * @param filename the file name.
   * @return the {@link BlameScmResult} instance.
   */
  private BlameScmResult doShellAnnotate(IntegrityScmProviderRepository iRepo, ScmFileSet workingDirectory,
    String filename)
  {
    BlameScmResult result;
    Commandline shell = new Commandline();
    shell.setWorkingDirectory(workingDirectory.getBasedir());
    shell.setExecutable("si");
    shell.createArg().setValue("annotate");
    shell.createArg().setValue("--hostname=" + iRepo.getHost());
    shell.createArg().setValue("--port=" + iRepo.getPort());
    shell.createArg().setValue("--user=" + iRepo.getUser());
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
}
