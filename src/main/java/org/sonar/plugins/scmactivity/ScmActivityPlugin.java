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

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

import java.util.List;

@Properties({
  @Property(
    key = ScmActivityPlugin.ENABLED,
    defaultValue = "true",
    name = "Activation of this SCM Activity plugin",
    description = "This property can be set to false in order to deactivate the SCM Activity plugin.",
    module = true,
    project = true,
    global = true,
    type = PropertyType.BOOLEAN
  ),
  @Property(
    key = ScmActivityPlugin.URL,
    defaultValue = "",
    name = "SCM URL",
    description = "The format is described in <a target=\"_blank\" href=\"http://maven.apache.org/scm/scm-url-format.html\">this page</a>. "
      + "Example: <i>scm:svn:https://svn.codehaus.org/sonar-plugins/trunk/scm-activity</i>. "
      + "This setting is not used with Git, Svn, and Hg since the url is discovered automatically.",
    module = true,
    project = true,
    global = false
  ),
  @Property(
    key = ScmActivityPlugin.USER,
    defaultValue = "",
    name = "User",
    description = "Optional user to be used to retrieve blame information from the SCM engine.",
    module = false,
    project = true,
    global = true
  ),
  @Property(
    key = ScmActivityPlugin.PASSWORD,
    defaultValue = "",
    name = "Password",
    description = "Optional password to be used to retrieve blame information from the SCM engine.",
    module = false,
    project = true,
    global = true,
    type = PropertyType.PASSWORD
  ),
  @Property(
    key = ScmActivityPlugin.THREAD_COUNT,
    defaultValue = "4",
    name = "Thread count",
    description = "Number of threads used to speed-up the retrieval of authors by line (aka blame information).",
    module = true,
    project = true,
    global = true
  ),
  @Property(
    key = ScmActivityPlugin.PERFORCE_CLIENTSPEC_NAME,
    defaultValue = "",
    name = "Perforce Clientspec name",
    description = "Will set the property "
      + "<a href=\"http://maven.apache.org/scm/maven-scm-providers/maven-scm-provider-perforce/\">maven.scm.perforce.clientspec.name</a> "
      + "used by Perforce SCM provider",
    module = false,
    project = false,
    global = false
  )})
public final class ScmActivityPlugin extends SonarPlugin {
  public static final String ENABLED = "sonar.scm.enabled";
  public static final String URL = "sonar.scm.url";
  public static final String USER = "sonar.scm.user.secured";
  public static final String PASSWORD = "sonar.scm.password.secured";
  public static final String THREAD_COUNT = "sonar.scm.threadCount";
  public static final String PERFORCE_CLIENTSPEC_NAME = "sonar.scm.perforce.clientspec.name";

  @SuppressWarnings("unchecked")
  public List getExtensions() {
    return ImmutableList.of(
      Blame.class,
      BlameVersionSelector.class,
      MavenScmConfiguration.class,
      ScmActivitySensor.class,
      ScmConfiguration.class,
      SonarScmManager.class,
      ScmFacade.class,
      ScmUrlGuess.class,
      UrlChecker.class);
  }
}
