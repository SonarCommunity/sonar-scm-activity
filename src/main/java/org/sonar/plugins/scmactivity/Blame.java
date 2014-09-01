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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class Blame implements BatchExtension {
  private static final Logger LOG = LoggerFactory.getLogger(Blame.class);
  private static final Pattern NON_ASCII_CHARS = Pattern.compile("[^\\x00-\\x7F]");
  private static final Pattern ACCENT_CODES = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private final ScmFacade scmFacade;

  public Blame(ScmFacade scmFacade) {
    this.scmFacade = scmFacade;
  }

  public MeasureUpdate save(File file, Resource resource, int lineCount) {
    BlameScmResult result = retrieveBlame(file);
    if (result == null) {
      return new CopyPreviousMeasures(resource);
    }

    PropertiesBuilder<Integer, String> authors = propertiesBuilder(CoreMetrics.SCM_AUTHORS_BY_LINE);
    PropertiesBuilder<Integer, String> dates = propertiesBuilder(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
    PropertiesBuilder<Integer, String> revisions = propertiesBuilder(CoreMetrics.SCM_REVISIONS_BY_LINE);

    int lineNumber = 1;
    for (BlameLine line : result.getLines()) {
      authors.add(lineNumber, normalizeString(line.getAuthor()));
      dates.add(lineNumber, DateUtils.formatDateTime(line.getDate()));
      revisions.add(lineNumber, line.getRevision());

      lineNumber++;
      // SONARPLUGINS-3097 For some SCM blame is missing on last empty line
      if (lineNumber > result.getLines().size() && lineNumber == lineCount) {
        authors.add(lineNumber, normalizeString(line.getAuthor()));
        dates.add(lineNumber, DateUtils.formatDateTime(line.getDate()));
        revisions.add(lineNumber, line.getRevision());
      }
    }

    return new SaveNewMeasures(resource, authors.build(), dates.build(), revisions.build());
  }

  private BlameScmResult retrieveBlame(File file) {
    LOG.info("Retrieve SCM info for {}", file);

    try {
      BlameScmResult result = scmFacade.blame(file);
      if (result.isSuccess()) {
        return result;
      }
      LOG.warn(String.format("Fail to retrieve SCM info of: %s. Reason: %s%n%s", file, result.getProviderMessage(), result.getCommandOutput()));
    } catch (ScmException e) {
      // See SONARPLUGINS-368. Can occur on generated source
      LOG.warn(String.format("Fail to retrieve SCM info of: %s", file), e);
    }

    return null;
  }

  private static PropertiesBuilder<Integer, String> propertiesBuilder(Metric metric) {
    return new PropertiesBuilder<Integer, String>(metric);
  }

  private String normalizeString(String inputString) {
    String lowerCasedString = inputString.toLowerCase();
    String stringWithoutAccents = removeAccents(lowerCasedString);
    return removeNonAsciiCharacters(stringWithoutAccents);
  }

  private String removeAccents(String inputString) {
    String unicodeDecomposedString = Normalizer.normalize(inputString, Normalizer.Form.NFD);
    return ACCENT_CODES.matcher(unicodeDecomposedString).replaceAll("");
  }

  private String removeNonAsciiCharacters(String inputString) {
    return NON_ASCII_CHARS.matcher(inputString).replaceAll("_");
  }
}
