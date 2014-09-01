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

import org.apache.maven.scm.*;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.blame.GitBlameCommand;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Overriding the default git blame command in order to use the SonarGitBlameConsumer to process the output
 * Also used to force ignoreWhitespace option as it is badly implemented see
 * http://jira.codehaus.org/browse/SCM-681#comment-323446
 *
 * @Todo: hack - to be submitted as an update in maven-scm-api for a future release
 * <p/>
 * <p/>
 * For more information, see:
 * <a href="http://jira.sonarsource.com/browse/DEVACT-103">DEVACT-103</a>
 * @since 1.5.1
 */
public class SonarGitBlameCommand extends GitBlameCommand {

  @Override
  protected ScmResult executeCommand(ScmProviderRepository repository, ScmFileSet workingDirectory,
                                     CommandParameters parameters)
    throws ScmException {
    String filename = parameters.getString(CommandParameter.FILE);
    Commandline cl = createCommandLine(workingDirectory.getBasedir(), filename,
      true);
    SonarGitBlameConsumer consumer = new SonarGitBlameConsumer(getLogger());
    CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

    int exitCode = GitCommandLineUtils.execute(cl, consumer, stderr, getLogger());
    if (exitCode != 0) {
      return new BlameScmResult(cl.toString(), "The git blame command failed.", stderr.getOutput(), false);
    }
    return new BlameScmResult(cl.toString(), consumer.getLines());
  }
}
