/*
 * Sonar SCM Activity Plugin
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

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.integrity.IntegrityScmProvider;

/**
 * Overriding the default integrity provider in order to use the SonarIntegrityBlameCommand to retrieve the blame data.
 *
 * @Todo hack - to be submitted as an update in maven-scm-provider-integrity for a future release.
 * @Todo Open issue in maven-scm-provider-integrity.
 * @since 1.6.1
 */
public class SonarIntegrityScmProvider extends IntegrityScmProvider {

  /**
   *{@inheritDoc}
   */
  @Override
  protected BlameScmResult blame(ScmProviderRepository repository, ScmFileSet fileSet, CommandParameters params) throws ScmException {
    SonarIntegrityBlameCommand command = new SonarIntegrityBlameCommand();
    command.setLogger(getLogger());
    return (BlameScmResult) command.execute(repository, fileSet, params);
  }

}
