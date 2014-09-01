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

import org.apache.maven.scm.provider.git.command.GitCommand;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;

/**
 * Overriding the default git exe provider in order to use the SonarGitBlameCommand to retrieve the blame data
 *
 * @Todo: hack - to be submitted as an update in maven-scm-api for a future release
 * <p/>
 * <p/>
 * For more information, see:
 * <a href="http://jira.sonarsource.com/browse/DEVACT-103">DEVACT-103</a>
 * @since 1.5.1
 */
public class SonarGitExeScmProvider extends GitExeScmProvider {

  @Override
  protected GitCommand getBlameCommand() {
    return new SonarGitBlameCommand();
  }
}
