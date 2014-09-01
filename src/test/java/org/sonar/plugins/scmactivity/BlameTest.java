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
import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.scmactivity.test.MoreConditions.reflectionEqualTo;

public class BlameTest {
  private static final String FILENAME = "source.java";
  private static final String UNKNOWN = "UNKNOWN";

  Blame blame;

  ScmFacade scmFacade = mock(ScmFacade.class);

  @Before
  public void setUp() {
    blame = new Blame(scmFacade);
  }

  @Test
  public void should_save_blame_measures() throws Exception {
    when(scmFacade.blame(file(FILENAME))).thenReturn(new BlameScmResult("fake", Arrays.asList(
      new BlameLine(new Date(13), "20", "godin"),
      new BlameLine(new Date(10), "21", "godin"))));

    MeasureUpdate update = blame.save(file(FILENAME), resource(FILENAME), 2);

    assertThat(((SaveNewMeasures) update).getAuthors()).is(reflectionEqualTo(new Measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "1=godin;2=godin")));
    assertThat(((SaveNewMeasures) update).getRevisions()).is(reflectionEqualTo(new Measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "1=20;2=21")));
  }

  // SONARPLUGINS-3097
  @Test
  public void should_add_missing_blame_line() throws Exception {
    when(scmFacade.blame(file(FILENAME))).thenReturn(new BlameScmResult("fake", Arrays.asList(
      new BlameLine(new Date(13), "20", "godin"),
      new BlameLine(new Date(10), "21", "godin"))));

    MeasureUpdate update = blame.save(file(FILENAME), resource(FILENAME), 3);

    assertThat(((SaveNewMeasures) update).getAuthors()).is(reflectionEqualTo(new Measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "1=godin;2=godin;3=godin")));
    assertThat(((SaveNewMeasures) update).getRevisions()).is(reflectionEqualTo(new Measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "1=20;2=21;3=21")));
  }

  /**
   * See SONARPLUGINS-368 - can occur with generated sources
   * @throws ScmException
   */
  @Test
  public void should_not_throw_scm_exception() throws ScmException {
    when(scmFacade.blame(file(UNKNOWN))).thenThrow(new ScmException("ERROR"));

    MeasureUpdate update = blame.save(file(UNKNOWN), resource(UNKNOWN), 0);

    assertThat(update).isInstanceOf(CopyPreviousMeasures.class);
  }

  @Test
  public void should_not_save_measures_if_blame_is_unsuccessful() throws ScmException {
    when(scmFacade.blame(file(UNKNOWN))).thenReturn(new BlameScmResult("", "", "", false));

    MeasureUpdate update = blame.save(file(UNKNOWN), resource(UNKNOWN), 0);

    assertThat(update).isInstanceOf(CopyPreviousMeasures.class);
  }

  @Test
  public void should_escape_non_ascii_char_in_author() throws Exception {
    when(scmFacade.blame(file(FILENAME))).thenReturn(new BlameScmResult("fake", Arrays.asList(
      new BlameLine(new Date(1), "9", "Firstname Lastname"),
      new BlameLine(new Date(2), "10", "a-valid_committer"),
      new BlameLine(new Date(3), "11", "Frédéricö ßaôl"),
      new BlameLine(new Date(4), "12", "çaà"),
      new BlameLine(new Date(5), "13", "valid-user@email.com"))));

    MeasureUpdate update = blame.save(file(FILENAME), resource(FILENAME), 5);

    assertThat(((SaveNewMeasures) update).getAuthors().getMetric()).isEqualTo(CoreMetrics.SCM_AUTHORS_BY_LINE);
    assertThat(((SaveNewMeasures) update).getAuthors().getData()).isEqualTo("1=firstname lastname;2=a-valid_committer;3=frederico _aol;4=caa;5=valid-user@email.com");
  }

  @Test
  public void should_lowercase_all_chars() throws Exception {
    when(scmFacade.blame(file(FILENAME))).thenReturn(new BlameScmResult("fake", Arrays.asList(
      new BlameLine(new Date(1), "9", "aAaRegR ZePp"),
      new BlameLine(new Date(1), "10", "aÄoÔ"))));

    MeasureUpdate update = blame.save(file(FILENAME), resource(FILENAME), 2);

    assertThat(((SaveNewMeasures) update).getAuthors().getMetric()).isEqualTo(CoreMetrics.SCM_AUTHORS_BY_LINE);
    assertThat(((SaveNewMeasures) update).getAuthors().getData()).isEqualTo("1=aaaregr zepp;2=aaoo");
  }

  static File file(String name) {
    return new File("src", name);
  }

  static Resource resource(String name) {
    return new org.sonar.api.resources.File(name);
  }
}
