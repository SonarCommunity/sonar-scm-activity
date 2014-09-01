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
package org.sonar.plugins.scmactivity;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.command.blame.BlameScmRequest;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.svn.util.SvnUtil;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmFacadeTest {
  ScmFacade scmFacade;

  ScmConfiguration conf = mock(ScmConfiguration.class);
  SonarScmManager manager = mock(SonarScmManager.class);
  ScmRepository repository = mock(ScmRepository.class);
  ScmProviderRepository provider = mock(ScmProviderRepository.class);
  BlameScmResult blameScmResult = mock(BlameScmResult.class);
  StatusScmResult statusScmResult = mock(StatusScmResult.class);

  @Before
  public void setUp() {
    scmFacade = new ScmFacade(manager, conf);
  }

  @Test
  public void should_set_credentials() throws ScmException {
    when(conf.getUrl()).thenReturn("/url");
    when(conf.getUser()).thenReturn("godin");
    when(conf.getPassword()).thenReturn("pass");
    when(manager.makeScmRepository("/url")).thenReturn(repository);
    when(repository.getProviderRepository()).thenReturn(provider);

    ScmRepository scmRepository = scmFacade.getScmRepository();

    assertThat(scmRepository).isSameAs(scmFacade.getScmRepository());
    verify(provider).setUser("godin");
    verify(provider).setPassword("pass");
  }

  @Test
  public void should_not_set_credentials_for_blank_user() throws ScmException {
    when(conf.getUser()).thenReturn("");
    when(conf.getPassword()).thenReturn("");
    when(conf.getUrl()).thenReturn("/url");
    when(manager.makeScmRepository("/url")).thenReturn(repository);

    ScmRepository scmRepository = scmFacade.getScmRepository();

    assertThat(scmRepository).isSameAs(scmFacade.getScmRepository());
    verify(provider, never()).setUser(anyString());
    verify(provider, never()).setPassword(anyString());
  }

  @Test
  public void should_set_credentials_for_blank_password() throws ScmException {
    when(conf.getUrl()).thenReturn("/url");
    when(conf.getUser()).thenReturn("login");
    when(conf.getPassword()).thenReturn("");
    when(manager.makeScmRepository("/url")).thenReturn(repository);
    when(repository.getProviderRepository()).thenReturn(provider);

    ScmRepository scmRepository = scmFacade.getScmRepository();

    assertThat(scmRepository).isSameAs(scmFacade.getScmRepository());
    verify(provider).setUser("login");
    verify(provider).setPassword("");
  }

  @Test
  public void shouldInitSvn() throws ScmException {
    when(conf.getUrl()).thenReturn("/url");
    when(conf.getScmProvider()).thenReturn("svn");
    when(manager.makeScmRepository("/url")).thenReturn(repository);
    when(repository.getProviderRepository()).thenReturn(provider);

    scmFacade.getScmRepository();

    assertThat(SvnUtil.getSettings().isTrustServerCert()).isTrue();
  }

  @Test(expected = SonarException.class)
  public void should_report_repository_failure() throws ScmException {
    when(manager.makeScmRepository(anyString())).thenThrow(new ScmRepositoryException("BUG"));

    scmFacade.getScmRepository();
  }

  @Test(expected = SonarException.class)
  public void should_report_failure() throws ScmException {
    when(manager.makeScmRepository(anyString())).thenThrow(new NoSuchScmProviderException("BUG"));

    scmFacade.getScmRepository();
  }

  @Test
  public void should_blame_file() throws ScmException {
    when(manager.blame(any(BlameScmRequest.class))).thenReturn(blameScmResult);

    BlameScmResult result = scmFacade.blame(new File("src/source.java"));

    assertThat(result).isSameAs(blameScmResult);
  }

  // SONARPLUGINS-2940
  @Test
  public void should_set_clientspec_property_for_erforce() throws ScmException {
    when(conf.getUrl()).thenReturn("/url");
    when(conf.getScmProvider()).thenReturn("perforce");
    when(conf.getPerforceClientspecName()).thenReturn("myclientspec");

    when(manager.blame(any(BlameScmRequest.class))).thenReturn(blameScmResult);

    scmFacade.blame(new File("src/source.java"));

    assertThat(System.getProperty("maven.scm.perforce.clientspec.name")).isEqualTo("myclientspec");
  }
}
