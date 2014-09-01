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

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

import java.util.Arrays;

import static org.mockito.Matchers.refEq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CopyPreviousMeasuresTest {
  TimeMachine timeMachine = mock(TimeMachine.class);
  Resource resource = mock(Resource.class);
  SensorContext context = mock(SensorContext.class);

  @Test
  public void should_copy_previous_measures_and_current_hash() {
    when(timeMachine.getMeasures(refEq(expectedQuery(resource)))).thenReturn(Arrays.asList(
      measure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, "measure1"),
      measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "measure2"),
      measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "measure3")));

    CopyPreviousMeasures copy = new CopyPreviousMeasures(resource);
    copy.execute(timeMachine, context);

    verify(context).saveMeasure(same(resource), refEq(measure(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE, "measure1").setPersistenceMode(PersistenceMode.DATABASE)));
    verify(context).saveMeasure(same(resource), refEq(measure(CoreMetrics.SCM_REVISIONS_BY_LINE, "measure2").setPersistenceMode(PersistenceMode.DATABASE)));
    verify(context).saveMeasure(same(resource), refEq(measure(CoreMetrics.SCM_AUTHORS_BY_LINE, "measure3").setPersistenceMode(PersistenceMode.DATABASE)));
  }

  static TimeMachineQuery expectedQuery(Resource resource) {
    return new TimeMachineQuery(resource)
      .setOnlyLastAnalysis(true)
      .setMetrics(
        CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
        CoreMetrics.SCM_REVISIONS_BY_LINE,
        CoreMetrics.SCM_AUTHORS_BY_LINE);
  }

  static Measure measure(Metric metric, String data) {
    return new Measure(metric, data);
  }
}
