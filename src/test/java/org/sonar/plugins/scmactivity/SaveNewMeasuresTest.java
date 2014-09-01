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
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.resources.Resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SaveNewMeasuresTest {
  Resource resource = mock(Resource.class);
  SensorContext context = mock(SensorContext.class);

  @Test
  public void should_save_new_measures() {
    Measure authors = measure("key1");
    Measure dates = measure("key2");
    Measure revisions = measure("key3");

    SaveNewMeasures saveNewMeasures = new SaveNewMeasures(resource, authors, dates, revisions);
    saveNewMeasures.execute(null, context);

    verify(context).saveMeasure(resource, authors.setPersistenceMode(PersistenceMode.DATABASE));
    verify(context).saveMeasure(resource, dates.setPersistenceMode(PersistenceMode.DATABASE));
    verify(context).saveMeasure(resource, revisions.setPersistenceMode(PersistenceMode.DATABASE));
  }

  static Measure measure(String key) {
    return new Measure(key);
  }
}
