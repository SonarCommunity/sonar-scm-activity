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

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.tfs.TfsScmProvider;
import org.apache.maven.scm.provider.tfs.command.blame.TfsBlameCommand;

public class SonarTfsScmProvider extends TfsScmProvider {

  private static final ScmProviderRepository DUMMY_SCM_PROVIDER_REPOSITORY = new ScmProviderRepository() {
  };

  @Override
  public ScmProviderRepository makeProviderScmRepository(String scmUrl, char delimiter) {
    return DUMMY_SCM_PROVIDER_REPOSITORY;
  }

  @Override
  public String getScmSpecificFilename() {
    return "$tf";
  }

  @Override
  protected BlameScmResult blame(ScmProviderRepository repository, ScmFileSet fileSet, CommandParameters parameters) throws ScmException {
    TfsBlameCommand command = new SonarTfsBlameCommand();
    command.setLogger(getLogger());
    return (BlameScmResult) command.execute(repository, fileSet, parameters);
  }

}
