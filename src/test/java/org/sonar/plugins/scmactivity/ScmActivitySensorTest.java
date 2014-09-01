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
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScmActivitySensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  ScmActivitySensor scmActivitySensor;

  BlameVersionSelector blameVersionSelector = mock(BlameVersionSelector.class);
  ScmConfiguration conf = mock(ScmConfiguration.class);
  UrlChecker urlChecker = mock(UrlChecker.class);
  DefaultFileSystem fs = new DefaultFileSystem();
  Project project = mock(Project.class);
  SensorContext context = mock(SensorContext.class);
  TimeMachine timeMachine = mock(TimeMachine.class);
  org.sonar.api.resources.File file = mock(org.sonar.api.resources.File.class);
  MeasureUpdate measureUpdate = mock(MeasureUpdate.class);
  File baseDir;

  @Before
  public void before() throws IOException {
    baseDir = temp.newFolder();
    fs.setBaseDir(baseDir);
    scmActivitySensor = new ScmActivitySensor(conf, blameVersionSelector, urlChecker, timeMachine, fs);
  }

  @Test
  public void should_execute() {
    when(conf.isEnabled()).thenReturn(true);

    boolean shouldExecute = scmActivitySensor.shouldExecuteOnProject(project);

    assertThat(shouldExecute).isTrue();
  }

  @Test
  public void should_not_execute_if_disabled() {
    when(conf.isEnabled()).thenReturn(false);

    boolean shouldExecute = scmActivitySensor.shouldExecuteOnProject(project);

    assertThat(shouldExecute).isFalse();
  }

  @Test
  public void should_generate_metrics() {
    List<Metric> metrics = scmActivitySensor.generatesMetrics();

    assertThat(metrics).hasSize(3);
  }

  @Test(timeout = 2000)
  public void should_check_url() {
    when(conf.getThreadCount()).thenReturn(1);
    when(conf.getUrl()).thenReturn("scm:url");

    scmActivitySensor.analyse(project, context);

    verify(urlChecker).check("scm:url");
  }

  @Test
  public void should_execute_measure_update_for_known_files() {
    InputFile source = file("source.java");
    InputFile test = file("UNKNOWN.java");
    when(conf.getThreadCount()).thenReturn(1);
    fs.add(source);
    fs.add(test);
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(new Measure()));
    when(blameVersionSelector.detect(any(org.sonar.api.resources.File.class), eq(source), eq(context), eq(true))).thenReturn(measureUpdate);
    when(context.getResource(any(org.sonar.api.resources.File.class))).thenReturn(file).thenReturn(null);
    scmActivitySensor.analyse(project, context);

    verify(measureUpdate, only()).execute(timeMachine, context);
  }

  @Test
  public void should_carry_on_after_error() {
    InputFile first = file("source.java");
    InputFile second = file("UNKNOWN.java");
    when(conf.getThreadCount()).thenReturn(1);
    fs.add(first);
    fs.add(second);
    when(timeMachine.getMeasures(any(TimeMachineQuery.class))).thenReturn(Arrays.asList(new Measure()));
    when(context.getResource(any(org.sonar.api.resources.File.class))).thenReturn(file);
    when(blameVersionSelector.detect(file, first, context, true)).thenThrow(new RuntimeException("BUG"));
    when(blameVersionSelector.detect(file, second, context, true)).thenReturn(measureUpdate);

    scmActivitySensor.analyse(project, context);

    verify(measureUpdate).execute(timeMachine, context);
  }

  @Test
  public void should_have_debug_name() {
    String debugName = scmActivitySensor.toString();

    assertThat(debugName).isEqualTo("ScmActivitySensor");
  }

  InputFile file(String name) {
    return new DefaultInputFile(name)
      .setFile(new File(baseDir, name));
  }
}
