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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

import java.util.List;

public class CopyPreviousMeasures implements MeasureUpdate {
  private static final List<Metric> METRICS = ImmutableList.of(
    CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE,
    CoreMetrics.SCM_REVISIONS_BY_LINE,
    CoreMetrics.SCM_AUTHORS_BY_LINE);

  private final Resource resource;

  public CopyPreviousMeasures(Resource resource) {
    this.resource = resource;
  }

  public void execute(TimeMachine timeMachine, SensorContext context) {
    TimeMachineQuery query = new TimeMachineQuery(resource).setOnlyLastAnalysis(true).setMetrics(METRICS);

    for (Measure measure : timeMachine.getMeasures(query)) {
      saveMeasure(context, measure);
    }
  }

  private void saveMeasure(SensorContext context, Measure measure) {
    context.saveMeasure(resource, new Measure(measure.getMetric(), measure.getData()).setPersistenceMode(PersistenceMode.DATABASE));
  }
}
