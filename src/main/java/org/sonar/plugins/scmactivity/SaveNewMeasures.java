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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

public class SaveNewMeasures implements MeasureUpdate {
  private final Resource resource;
  private final Measure authors;
  private final Measure dates;
  private final Measure revisions;

  public SaveNewMeasures(Resource resource, Measure authors, Measure dates, Measure revisions) {
    this.resource = resource;
    this.authors = authors;
    this.dates = dates;
    this.revisions = revisions;
  }

  public void execute(TimeMachine timeMachine, SensorContext context) {
    saveMeasure(context, authors);
    saveMeasure(context, dates);
    saveMeasure(context, revisions);
  }

  private void saveMeasure(SensorContext context, Measure measure) {
    context.saveMeasure(resource, measure.setPersistenceMode(PersistenceMode.DATABASE));
  }

  @VisibleForTesting
  Measure getAuthors() {
    return authors;
  }

  @VisibleForTesting
  Measure getRevisions() {
    return revisions;
  }
}
