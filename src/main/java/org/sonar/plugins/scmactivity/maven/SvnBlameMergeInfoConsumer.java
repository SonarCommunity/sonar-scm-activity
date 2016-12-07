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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.util.AbstractConsumer;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

/**
 * @author Evgeny Mandrikov
 * @author Olivier Lamy
 * @author Vladimir Piyanov
 */
public class SvnBlameMergeInfoConsumer extends AbstractConsumer
{
    private static final String SVN_TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String LINE_PATTERN = "line-number=\"(.*)\"";

    private static final String REVISION_PATTERN = "revision=\"(.*)\"";

    private static final String AUTHOR_PATTERN = "<author>(.*)</author>";

    private static final String DATE_PATTERN = "<date>(.*)T(.*)\\.(.*)Z</date>";

    private boolean insideCommitSection = false;
    private boolean insideMergedSection = false;
    
    /**
     * @see #LINE_PATTERN
     */
    private RE lineRegexp;

    /**
     * @see #REVISION_PATTERN
     */
    private RE revisionRegexp;

    /**
     * @see #AUTHOR_PATTERN
     */
    private RE authorRegexp;

    /**
     * @see #DATE_PATTERN
     */
    private RE dateRegexp;

    private SimpleDateFormat dateFormat;

    private List<BlameLine> lines = new ArrayList<BlameLine>();

    public SvnBlameMergeInfoConsumer( ScmLogger logger )
    {
        super( logger );

        dateFormat = new SimpleDateFormat( SVN_TIMESTAMP_PATTERN );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

        try
        {
            lineRegexp = new RE( LINE_PATTERN );
            revisionRegexp = new RE( REVISION_PATTERN );
            authorRegexp = new RE( AUTHOR_PATTERN );
            dateRegexp = new RE( DATE_PATTERN );
        }
        catch ( RESyntaxException ex )
        {
            throw new RuntimeException(
                "INTERNAL ERROR: Could not create regexp to parse git log file. This shouldn't happen. Something is probably wrong with the oro installation.",
                ex );
        }
    }

    private int lineNumber;

    private String committerRevision;

    private String committer;
    
    private Date committerDate;
    
    private String authorRevision;
    
    private String author;
    
    private Date authorDate;

    public void consumeLine( String line )
    {
        if ( lineRegexp.match( line ) )
        {
            String lineNumberStr = lineRegexp.getParen( 1 );
            lineNumber = Integer.parseInt( lineNumberStr );
            insideCommitSection = false;
            insideMergedSection = false;
        }
        else if ( line.contains("<commit") && !insideMergedSection )
        {
            insideCommitSection = true;
        }
        else if ( line.contains("<merged") )
        {
            insideMergedSection = true;
            insideCommitSection = false;
        }
        else if ( revisionRegexp.match( line ) )
        {
            if(insideCommitSection) 
            {
                committerRevision = revisionRegexp.getParen( 1 );
            }
            else if(insideMergedSection) 
            {
                authorRevision = revisionRegexp.getParen( 1 );
            }
        }
        else if ( authorRegexp.match( line ) )
        {
            if(insideCommitSection) 
            {
                committer = authorRegexp.getParen( 1 );
            }
            else if(insideMergedSection) 
            {
                author = authorRegexp.getParen( 1 );
            }
        }
        else if ( dateRegexp.match( line ) )
        {
            if(insideCommitSection) 
            {
                String date = dateRegexp.getParen( 1 );
                String time = dateRegexp.getParen( 2 );
                committerDate = parseDateTime( date + " " + time ); 
            }
            else if(insideMergedSection) 
            {
                String date = dateRegexp.getParen( 1 );
                String time = dateRegexp.getParen( 2 );
                authorDate = parseDateTime( date + " " + time ); 
            }
        }
        else if ( line.contains("</entry>") )
        {
            if(author == null)
            {
                lines.add( new BlameLine( committerDate, committerRevision, committer, committer ) );
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Line " + lineNumber + ": committer " + committer + " rev" + committerRevision + " (" + committerDate + ")");
                }
            }
            else
            {
                lines.add( new BlameLine( committerDate, committerRevision, author, committer ) );
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Line " + lineNumber + ": committer " + committer + " rev" + committerRevision + " (" + committerDate + ")"
                            + ": author " + author + " rev" + authorRevision + " (" + authorDate + ")");
                }
            }
            
            insideCommitSection = false;
            insideMergedSection = false;
        }
    }

    protected Date parseDateTime( String dateTimeStr )
    {
        try
        {
            return dateFormat.parse( dateTimeStr );
        }
        catch ( ParseException e )
        {
            getLogger().error( "skip ParseException: " + e.getMessage() + " during parsing date " + dateTimeStr, e );
            return null;
        }
    }

    public List<BlameLine> getLines()
    {
        return lines;
    }
}
