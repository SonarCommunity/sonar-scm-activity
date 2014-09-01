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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.plugins.scmactivity.test.TemporaryFile;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlameVersionSelectorTest {

  @Rule
  public TemporaryFile temporaryFile = new TemporaryFile();

  BlameVersionSelector blameVersionSelector;

  Blame blameSensor = mock(Blame.class);
  SensorContext context = mock(SensorContext.class);
  org.sonar.api.resources.File resource = mock(org.sonar.api.resources.File.class);
  MeasureUpdate saveBlame = mock(MeasureUpdate.class);

  @Before
  public void setUp() {
    blameVersionSelector = new BlameVersionSelector(blameSensor);
  }

  @Test
  public void should_save_blame_when_file_changed() throws IOException {
    File file = file("source.java", "foo");
    DefaultInputFile inputFile = new DefaultInputFile("source.java").setFile(file);
    inputFile.setStatus(InputFile.Status.CHANGED);
    inputFile.setLines(1);
    when(blameSensor.save(file, resource, 1)).thenReturn(saveBlame);

    MeasureUpdate update = blameVersionSelector.detect(resource, inputFile, context, true);

    assertThat(update).isSameAs(saveBlame);
  }

  // SONARPLUGINS-3633
  @Test
  public void should_save_blame_when_file_is_the_same_but_no_previous_measures_present() throws IOException {
    File file = file("source.java", "foo");
    DefaultInputFile inputFile = new DefaultInputFile("source.java").setFile(file);
    inputFile.setStatus(InputFile.Status.SAME);
    inputFile.setLines(1);
    when(blameSensor.save(file, resource, 1)).thenReturn(saveBlame);

    MeasureUpdate update = blameVersionSelector.detect(resource, inputFile, context, false);

    assertThat(update).isSameAs(saveBlame);
  }

  @Test
  public void should_copy_previous_measures_when_file_is_the_same_and_previous_measures_present() throws IOException {

    File file = file("source.java", "foo");
    DefaultInputFile inputFile = new DefaultInputFile("source.java").setFile(file);
    inputFile.setStatus(InputFile.Status.SAME);
    inputFile.setLines(2);
    when(blameSensor.save(file, resource, 1)).thenReturn(saveBlame);

    MeasureUpdate update = blameVersionSelector.detect(resource, inputFile, context, true);

    assertThat(update).isInstanceOf(CopyPreviousMeasures.class);
  }

  File file(String name, String content) throws IOException {
    return temporaryFile.create(name, content);
  }
}
