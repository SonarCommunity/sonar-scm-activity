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
import org.sonar.api.utils.SonarException;

public class UrlCheckerTest {
  UrlChecker checker;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    checker = new UrlChecker();
  }

  @Test
  public void shouldFailIfBlank() {
    exception.expect(SonarException.class);
    exception.expectMessage("SCM URL should be provided");

    checker.check(" ");
  }

  @Test
  public void shouldFailIfBadlyFormed() {
    exception.expect(SonarException.class);
    exception.expectMessage("URL does not respect the SCM URL format described in http://maven.apache.org/scm/scm-url-format.html");
    exception.expectMessage("[http://foo]");

    checker.check("http://foo");
  }

  @Test
  public void shouldFailIfProviderNotSupported() {
    exception.expect(SonarException.class);
    exception.expectMessage("Unsupported SCM");
    exception.expectMessage("[synergy]");

    checker.check("scm:synergy:foo");
  }

  @Test
  public void shouldAcceptSvnUrl() {
    checker.check("scm:svn:https://codehaus.org");
  }
}
