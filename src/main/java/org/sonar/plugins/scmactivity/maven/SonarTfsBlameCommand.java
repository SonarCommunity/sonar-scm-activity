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
package org.sonar.plugins.scmactivity.maven;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.tfs.command.blame.TfsBlameCommand;
import org.apache.maven.scm.provider.tfs.command.blame.TfsBlameConsumer;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public class SonarTfsBlameCommand extends TfsBlameCommand {

  private static final String EXECUTABLE = "SonarTfsAnnotate";

  @Override
  public BlameScmResult executeBlameCommand(ScmProviderRepository repo, ScmFileSet workingDirectory, String filename) throws ScmException {
    Commandline cl = new Commandline();
    cl.setWorkingDirectory(workingDirectory.getBasedir());
    cl.setExecutable(EXECUTABLE);
    cl.createArg().setValue(filename);

    TfsBlameConsumer consumer = new TfsBlameConsumer(getLogger());
    CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

    try {
      int exitCode = CommandLineUtils.executeCommandLine(cl, consumer, stderr);

      if (exitCode != 0) {
        return new BlameScmResult(cl.toString(), "The " + EXECUTABLE + " command failed. Did you install https://github.com/SonarCommunity/sonar-tfs ?", stderr.getOutput(), false);
      }
    } catch (CommandLineException ex) {
      throw new ScmException("Error while executing command.", ex);
    }

    return new BlameScmResult(cl.toString(), consumer.getLines());
  }

}
