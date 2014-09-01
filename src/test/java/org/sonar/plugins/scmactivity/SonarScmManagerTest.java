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

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.accurev.AccuRevScmProvider;
import org.apache.maven.scm.provider.bazaar.BazaarScmProvider;
import org.apache.maven.scm.provider.clearcase.ClearCaseScmProvider;
import org.apache.maven.scm.provider.cvslib.cvsexe.CvsExeScmProvider;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.provider.hg.HgScmProvider;
import org.apache.maven.scm.provider.integrity.IntegrityScmProvider;
import org.apache.maven.scm.provider.jazz.JazzScmProvider;
import org.apache.maven.scm.provider.perforce.PerforceScmProvider;
import org.apache.maven.scm.provider.svn.svnexe.SvnExeScmProvider;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.sonar.plugins.scmactivity.maven.SonarTfsScmProvider;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarScmManagerTest {
  ScmConfiguration conf = mock(ScmConfiguration.class);
  Logger logger = mock(Logger.class);
  Throwable error = mock(Throwable.class);

  @Test
  public void should_use_native_providers() throws NoSuchScmProviderException {
    when(conf.isEnabled()).thenReturn(true);

    SonarScmManager scmManager = new SonarScmManager();

    assertThat(scmManager.getProviderByType("svn")).isInstanceOf(SvnExeScmProvider.class);
    assertThat(scmManager.getProviderByType("git")).isInstanceOf(GitExeScmProvider.class);
    assertThat(scmManager.getProviderByType("cvs")).isInstanceOf(CvsExeScmProvider.class);
    assertThat(scmManager.getProviderByType("hg")).isInstanceOf(HgScmProvider.class);
    assertThat(scmManager.getProviderByType("bazaar")).isInstanceOf(BazaarScmProvider.class);
    assertThat(scmManager.getProviderByType("clearcase")).isInstanceOf(ClearCaseScmProvider.class);
    assertThat(scmManager.getProviderByType("accurev")).isInstanceOf(AccuRevScmProvider.class);
    assertThat(scmManager.getProviderByType("perforce")).isInstanceOf(PerforceScmProvider.class);
    assertThat(scmManager.getProviderByType("tfs")).isInstanceOf(SonarTfsScmProvider.class);
    assertThat(scmManager.getProviderByType("jazz")).isInstanceOf(JazzScmProvider.class);
    assertThat(scmManager.getProviderByType("integrity")).isInstanceOf(IntegrityScmProvider.class);
  }

  @Test
  public void should_log() {
    when(error.getMessage()).thenReturn("errorMessage");

    SonarScmManager.SonarScmLogger log = new SonarScmManager.SonarScmLogger(logger);
    log.isDebugEnabled();
    log.debug("message");
    log.debug("message", error);
    log.debug(error);
    log.isInfoEnabled();
    log.info("message");
    log.info("message", error);
    log.info(error);
    log.isWarnEnabled();
    log.warn("message");
    log.warn("message", error);
    log.warn(error);
    log.isErrorEnabled();
    log.error("message");
    log.error("message", error);
    log.error(error);

    InOrder inOrder = Mockito.inOrder(logger);
    inOrder.verify(logger).isDebugEnabled();
    inOrder.verify(logger).debug("message");
    inOrder.verify(logger).debug("message", error);
    inOrder.verify(logger).debug("errorMessage", error);
    inOrder.verify(logger).isDebugEnabled();
    inOrder.verify(logger).info("message");
    inOrder.verify(logger).info("message", error);
    inOrder.verify(logger).info("errorMessage", error);
    inOrder.verify(logger).isWarnEnabled();
    inOrder.verify(logger).warn("message");
    inOrder.verify(logger).warn("message", error);
    inOrder.verify(logger).warn("errorMessage", error);
    inOrder.verify(logger).isErrorEnabled();
    inOrder.verify(logger).error("message");
    inOrder.verify(logger).error("message", error);
    inOrder.verify(logger).error("errorMessage", error);
  }

  @Test
  public void should_enable_maven_logs_when_debug_active() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(true);

    SonarScmManager.SonarScmLogger log = new SonarScmManager.SonarScmLogger(logger);
    assertThat(log.isInfoEnabled()).isTrue();
  }

  @Test
  public void should_inhibit_maven_logs_when_level_is_info() throws Exception {
    when(logger.isDebugEnabled()).thenReturn(false);

    SonarScmManager.SonarScmLogger log = new SonarScmManager.SonarScmLogger(logger);
    assertThat(log.isInfoEnabled()).isFalse();
  }
}
