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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmConfigurationTest {
  ScmConfiguration scmConfiguration;

  Settings settings = new Settings(new PropertyDefinitions(ScmActivityPlugin.class));
  ScmUrlGuess scmUrlGuess = mock(ScmUrlGuess.class);
  MavenScmConfiguration mavenConf = mock(MavenScmConfiguration.class);

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    scmConfiguration = new ScmConfiguration(settings, scmUrlGuess, mavenConf);
  }

  @Test
  public void shouldReturnUsername() {
    settings.setProperty(ScmActivityPlugin.USER, "godin");

    assertThat(scmConfiguration.getUser()).isEqualTo("godin");
  }

  @Test
  public void shouldReturnPassword() {
    settings.setProperty(ScmActivityPlugin.PASSWORD, "pass");

    assertThat(scmConfiguration.getPassword()).isEqualTo("pass");
  }

  @Test
  public void shouldReturnUrlFromConfiguration() {
    settings.setProperty(ScmActivityPlugin.URL, "http://test");

    assertThat(scmConfiguration.getUrl()).isEqualTo("http://test");
  }

  @Test
  public void should_be_enabled_by_default() {
    assertThat(scmConfiguration.isEnabled()).isTrue();
  }

  @Test
  public void should_disable() {
    settings.setProperty(ScmActivityPlugin.ENABLED, false);

    assertThat(scmConfiguration.isEnabled()).isFalse();
  }

  @Test
  public void should_get_default_thread_count() {
    assertThat(scmConfiguration.getThreadCount()).isEqualTo(4);
  }

  @Test
  public void should_get_thread_count() {
    settings.setProperty(ScmActivityPlugin.THREAD_COUNT, 8);

    assertThat(scmConfiguration.getThreadCount()).isEqualTo(8);
  }

  @Test
  public void should_use_single_thread() {
    settings.setProperty(ScmActivityPlugin.THREAD_COUNT, 1);

    assertThat(scmConfiguration.getThreadCount()).isEqualTo(1);
  }

  @Test
  public void should_fail_on_invalid_thread_count() {
    settings.setProperty(ScmActivityPlugin.THREAD_COUNT, 0);

    exception.expect(SonarException.class);
    exception.expectMessage("SCM Activity Plugin is configured to use [0] thread(s). The minimum is 1.");

    scmConfiguration.getThreadCount();
  }

  @Test
  public void should_accept_thread_count_too_high() {
    settings.setProperty(ScmActivityPlugin.THREAD_COUNT, 1000);

    assertThat(scmConfiguration.getThreadCount()).isEqualTo(1000);
  }

  @Test
  public void shouldGetMavenDeveloperUrlIfCredentials() {
    when(mavenConf.getDeveloperUrl()).thenReturn("scm:svn:https:writable");
    settings.setProperty(ScmActivityPlugin.USER, "godin");
    settings.setProperty(ScmActivityPlugin.PASSWORD, "pass");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:https:writable");
  }

  @Test
  public void shouldNotGetMavenDeveloperUrlIfNoCredentials() {
    when(mavenConf.getDeveloperUrl()).thenReturn("scm:svn:https:writable");
    when(mavenConf.getUrl()).thenReturn("scm:svn:https:readonly");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:https:readonly");
  }

  @Test
  public void shouldGetMavenUrlIfNoDeveloperUrl() {
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:readonly");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:http:readonly");
  }

  @Test
  public void shouldOverrideMavenUrl() {
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:readonly");
    settings.setProperty(ScmActivityPlugin.URL, "scm:svn:http:override");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:http:override");
  }

  @Test
  public void should_guess_url_if_nothing_configured() {
    when(scmUrlGuess.guess()).thenReturn("scm:svn:guessed");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:guessed");
  }

  @Test
  public void should_guess_url_only_once() {
    when(scmUrlGuess.guess()).thenReturn("scm:svn:guessed");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:guessed");
    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:guessed");
    verify(scmUrlGuess, times(1)).guess();
  }

  @Test
  public void should_guess_first() {
    settings.setProperty("sonar.scm.hidden.guess", true);
    when(scmUrlGuess.guess()).thenReturn("scm:svn:guessed");
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:readonly");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:guessed");
  }

  @Test
  public void should_disable_guess_with_hidden_configuration() {
    settings.setProperty("sonar.scm.hidden.guess", false);
    when(scmUrlGuess.guess()).thenReturn("scm:svn:guessed");
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:readonly");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:http:readonly");
  }

  @Test
  public void should_guess_by_default() {
    when(scmUrlGuess.guess()).thenReturn("scm:svn:guessed");
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:readonly");

    assertThat(scmConfiguration.getUrl()).isEqualTo("scm:svn:guessed");
  }

  @Test
  public void shouldGetScmProvider() {
    when(mavenConf.getUrl()).thenReturn("scm:svn:http:foo");

    assertThat(scmConfiguration.getScmProvider()).isEqualTo("svn");
  }

  @Test
  public void should_get_empty_scm_provider() {
    when(mavenConf.getUrl()).thenReturn(" ");

    assertThat(scmConfiguration.getScmProvider()).isNull();
  }

  @Test
  public void should_get_maven_url_in_non_maven_environment() {
    scmConfiguration = new ScmConfiguration(settings, scmUrlGuess);

    assertThat(scmConfiguration.getUrl()).isNull();
  }

  @Test
  public void shouldReturnPerforceClientSpec() {
    settings.setProperty(ScmActivityPlugin.PERFORCE_CLIENTSPEC_NAME, "myclientspec");

    assertThat(scmConfiguration.getPerforceClientspecName()).isEqualTo("myclientspec");
  }
}
