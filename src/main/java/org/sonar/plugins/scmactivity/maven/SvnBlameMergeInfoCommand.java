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

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.blame.AbstractBlameCommand;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.svn.command.SvnCommand;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.command.SvnCommandLineUtils;
import org.apache.maven.scm.provider.svn.svnexe.command.blame.SvnBlameConsumer;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import com.google.common.base.Splitter;

/**
 * @author Evgeny Mandrikov
 * @author Olivier Lamy
 * @author Vladimir Piyanov
 */
public class SvnBlameMergeInfoCommand extends AbstractBlameCommand
implements SvnCommand
{
    private final String svnOptions;
    
    public SvnBlameMergeInfoCommand() {
        this(null);
    }
    
    public SvnBlameMergeInfoCommand(String svnOptions) {
        this.svnOptions = svnOptions;
    }

/**
 * {@inheritDoc}
 */
public BlameScmResult executeBlameCommand( ScmProviderRepository repo, ScmFileSet workingDirectory,
                                           String filename )
    throws ScmException
{
    Commandline cl = createCommandLine( (SvnScmProviderRepository) repo, workingDirectory.getBasedir(), filename );

    SvnBlameMergeInfoConsumer consumer = new SvnBlameMergeInfoConsumer( getLogger() );

    CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

    if ( getLogger().isInfoEnabled() )
    {
        getLogger().info( "Executing: " + SvnCommandLineUtils.cryptPassword( cl ) );
        getLogger().info( "Working directory: " + cl.getWorkingDirectory().getAbsolutePath() );
    }

    int exitCode;

    try
    {
        exitCode = SvnCommandLineUtils.execute( cl, consumer, stderr, getLogger() );
    }
    catch ( CommandLineException ex )
    {
        throw new ScmException( "Error while executing command.", ex );
    }

    if ( exitCode != 0 )
    {
        return new BlameScmResult( cl.toString(), "The svn command failed.", stderr.getOutput(), false );
    }

    return new BlameScmResult( cl.toString(), consumer.getLines() );
}

public static Commandline createCommandLine( SvnScmProviderRepository repository, File workingDirectory,
                                             String filename )
{
    Commandline cl = SvnCommandLineUtils.getBaseSvnCommandLine( workingDirectory, repository );
    cl.createArg().setValue( "blame" );
    cl.createArg().setValue( "--xml" );
    cl.createArg().setValue( "--use-merge-history" );

    String svnOptions = System.getProperty("maven.scm.svn.options");
    if(!isEmpty(svnOptions))
    {
        for (String option : Splitter.on(' ').omitEmptyStrings().split(svnOptions))
        {
             cl.createArg().setValue(option);
        }
    }
    
    cl.createArg().setValue( filename );
    return cl;
}
}
