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

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenScmConfigurationTest {
  MavenScmConfiguration mavenScmConfiguration;

  MavenProject mavenProject = mock(MavenProject.class);
  Scm scm = mock(Scm.class);

  @Before
  public void setUp() {
    mavenScmConfiguration = new MavenScmConfiguration(mavenProject);
  }

  @Test
  public void should_get_developer_url() {
    when(mavenProject.getScm()).thenReturn(scm);
    when(scm.getDeveloperConnection()).thenReturn("/developerUrl");

    String developerUrl = mavenScmConfiguration.getDeveloperUrl();

    assertThat(developerUrl).isEqualTo("/developerUrl");
  }

  @Test
  public void should_get_url() {
    when(mavenProject.getScm()).thenReturn(scm);
    when(scm.getConnection()).thenReturn("/url");

    String developerUrl = mavenScmConfiguration.getUrl();

    assertThat(developerUrl).isEqualTo("/url");
  }

  @Test
  public void should_get_empty_urls_if_no_scm() {
    assertThat(mavenScmConfiguration.getDeveloperUrl()).isNull();
    assertThat(mavenScmConfiguration.getUrl()).isNull();
  }
}
