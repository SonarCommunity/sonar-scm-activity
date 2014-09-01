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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.provider.ScmUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ScmConfiguration implements BatchExtension {
  private static final Logger LOG = LoggerFactory.getLogger(ScmConfiguration.class);

  private final Settings settings;
  private final ScmUrlGuess scmUrlGuess;
  private final MavenScmConfiguration mavenConfonfiguration;
  private final Supplier<String> url;

  public ScmConfiguration(Settings settings, ScmUrlGuess scmUrlGuess, @Nullable MavenScmConfiguration mavenConfiguration) {
    this.settings = settings;
    this.scmUrlGuess = scmUrlGuess;
    this.mavenConfonfiguration = mavenConfiguration;
    url = Suppliers.memoize(new UrlSupplier());
  }

  public ScmConfiguration(Settings settings, ScmUrlGuess scmUrlGuess) {
    this(settings, scmUrlGuess, null /* not in maven environment */);
  }

  public String getScmProvider() {
    if (StringUtils.isBlank(getUrl())) {
      return null;
    }
    return ScmUrlUtils.getProvider(getUrl());
  }

  public boolean isEnabled() {
    return settings.getBoolean(ScmActivityPlugin.ENABLED);
  }

  public String getUser() {
    return settings.getString(ScmActivityPlugin.USER);
  }

  public String getPassword() {
    return settings.getString(ScmActivityPlugin.PASSWORD);
  }

  public int getThreadCount() {
    int threadCount = settings.getInt(ScmActivityPlugin.THREAD_COUNT);

    if (threadCount < 1) {
      throw new SonarException(String.format("SCM Activity Plugin is configured to use [%d] thread(s). The minimum is 1.", threadCount));
    }
    if (threadCount > Runtime.getRuntime().availableProcessors()) {
      LOG.warn("SCM Activity Plugin is configured to use more threads than actually available on this machine.");
    }

    return threadCount;
  }

  @CheckForNull
  public String getPerforceClientspecName() {
    String clientspecName = settings.getString(ScmActivityPlugin.PERFORCE_CLIENTSPEC_NAME);
    if (StringUtils.isBlank(clientspecName)) {
      return null;
    }
    return clientspecName;
  }

  public String getUrl() {
    return url.get();
  }

  private class UrlSupplier implements Supplier<String> {
    public String get() {
      // Default to true if not set
      String guessStr = settings.getString("sonar.scm.hidden.guess");
      if (StringUtils.isEmpty(guessStr) || Boolean.parseBoolean(guessStr)) {
        String guessedUrl = guessUrl();
        if (!StringUtils.isBlank(guessedUrl)) {
          return guessedUrl;
        }
      }

      String urlProperty = settings.getString(ScmActivityPlugin.URL);
      if (!StringUtils.isBlank(urlProperty)) {
        return urlProperty;
      }

      String mavenUrl = getMavenUrl();
      if (!StringUtils.isBlank(mavenUrl)) {
        return mavenUrl;
      }

      return null;
    }

    private String getMavenUrl() {
      if (mavenConfonfiguration == null) {
        return null;
      }
      if (StringUtils.isNotBlank(mavenConfonfiguration.getDeveloperUrl()) && StringUtils.isNotBlank(getUser())) {
        return mavenConfonfiguration.getDeveloperUrl();
      }
      return mavenConfonfiguration.getUrl();
    }

    private String guessUrl() {
      return scmUrlGuess.guess();
    }
  }
}
