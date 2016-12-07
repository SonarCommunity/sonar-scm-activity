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
package org.sonar.plugins.scmactivity.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.log.DefaultLog;
import org.apache.maven.scm.provider.git.gitexe.command.blame.GitBlameConsumer;
import org.junit.Test;

public class SvnBlameMergeInfoConsumerTest {

    @Test
    public void testSameAuthor() throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-merge-info.out");

        Assert.assertEquals(3, consumer.getLines().size());

        BlameLine blameLine = consumer.getLines().get(2);
        Assert.assertEquals("23048", blameLine.getRevision());
        Assert.assertEquals("ybaryshnikova", blameLine.getAuthor());
        Assert.assertEquals("ybaryshnikova", blameLine.getCommitter());
        Assert.assertNotNull(blameLine.getDate());
    }
    
    @Test
    public void testSameAuthor2() throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-merge-info.out");
        
        Assert.assertEquals(3, consumer.getLines().size());
        
        BlameLine blameLine = consumer.getLines().get(1);
        Assert.assertEquals("41615", blameLine.getRevision());
        Assert.assertEquals("a.marin", blameLine.getAuthor());
        Assert.assertEquals("a.marin", blameLine.getCommitter());
        Assert.assertNotNull(blameLine.getDate());
    }
    
    @Test
    public void testDifferentAuthor() throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-merge-info.out");
        
        Assert.assertEquals(3, consumer.getLines().size());
        
        BlameLine blameLine = consumer.getLines().get(0);
        Assert.assertEquals("38858", blameLine.getRevision());
        Assert.assertEquals("s.zamyslov", blameLine.getAuthor());
        Assert.assertEquals("vpiyanov", blameLine.getCommitter());
        Assert.assertNotNull(blameLine.getDate());
    }
    @Test
    public void testMergeInfoMissing() throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-merge-info-missing.out");
        
        Assert.assertEquals(3, consumer.getLines().size());
        
        BlameLine blameLine = consumer.getLines().get(0);
        Assert.assertEquals("38858", blameLine.getRevision());
        Assert.assertEquals("vpiyanov", blameLine.getAuthor());
        Assert.assertEquals("vpiyanov", blameLine.getCommitter());
        Assert.assertNotNull(blameLine.getDate());
    }
    

    @Test
    public void testConsumerEmptyFile()
      throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-empty.out");

      Assert.assertEquals(0, consumer.getLines().size());
    }


    @Test
    public void testConsumerOnNewFile()
      throws Exception {
        SvnBlameMergeInfoConsumer consumer = consumeFile("src/test/resources/svn/blame/svn-blame-new-file.out");

      Assert.assertEquals(0, consumer.getLines().size());
    }

    /**
     * Consume all lines in the given file with a fresh {@link GitBlameConsumer}.
     * 
     * @param fileName
     * @return the resulting {@link GitBlameConsumer}
     * @throws java.io.IOException
     */
    private SvnBlameMergeInfoConsumer consumeFile(String fileName) throws IOException, URISyntaxException {
        SvnBlameMergeInfoConsumer consumer = new SvnBlameMergeInfoConsumer(new DefaultLog());

        File f = getTestFile(fileName);

        BufferedReader r = new BufferedReader(new FileReader(f));

        String line;

        while ((line = r.readLine()) != null) {
            consumer.consumeLine(line);
        }
        return consumer;
    }

    private File getTestFile(String fileName) throws URISyntaxException {
        return new File(fileName);
    }
}
