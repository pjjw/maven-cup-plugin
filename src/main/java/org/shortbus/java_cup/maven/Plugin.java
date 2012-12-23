/*
  Copyright (C) 2006 Petra Malik
  This file is part of the czt project.

  The czt project contains free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  The czt project is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with czt; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.shortbus.java_cup.maven;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import java_cup.Main;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal generate
 * @phase generate-sources
 * @description Maven JavaCup Plugin
 */
public class Plugin
  extends AbstractMojo
{
  /**
   * @parameter expression="${basedir}/src/main/cup"
   * @required
   */
  private File sourceDirectory;

  /**
   * @parameter expression="${project.build.directory}/generated-sources/cup"
   * @required
   */
  private String outputDirectory;

  /**
   * @parameter 
   */
  private boolean dumpGrammar;
  
  /**
   * @parameter 
   */
  private boolean dumpStates;

  /**
   * @parameter 
   */
  private boolean dumpTables;

  /**
   * @parameter
   */
  private int expectConflicts;
  
  /**
   * @parameter expression="${project}"
   * @required
   */
  private MavenProject project;

  public void execute()
    throws MojoExecutionException
  {
    final String fileSep = System.getProperty("file.separator");
    if (project != null)
    {
      project.addCompileSourceRoot(outputDirectory);
    }
    List<File> grammars = getGrammarFiles();
    getLog().info("CUP: Processing " + grammars.size() + " cup files");
    for (File file : grammars) {
      getLog().info("CUP: Processing " + file.getPath());
      String className = file.getName().replaceAll(".cup", "");
      try {
        String packageName = getPackage(file);
        // String destdir = outputDirectory + fileSep +
        //   packageName.replace(".", fileSep);
        String destdir = outputDirectory;
        File destDir = new File(destdir);
        File destFile = new File(destDir, "parser.java");
        getLog().debug("CUP: Checking file dates:\n\t" + new Date(destFile.lastModified()) + 
          "= " + destFile + "\n\t" + new Date(file.lastModified()) + "= " +
          file);
        if (destFile.exists() &&
            destFile.lastModified() >= file.lastModified()) {
          getLog().debug("CUP: Parser file is up-to-date.");
        }
        else {
          if (! destDir.exists()){ destDir.mkdirs();}
          List<String> args = new ArrayList<String>();
          if (dumpGrammar) {args.add("-dump_grammar");}
          if (dumpStates) {args.add("-dump_states");}
          if (dumpTables) {args.add("-dump_tables");}
          args.addAll(Arrays.asList("-destdir", destdir,
                                   "-package", packageName,
                                   "-parser", "parser",
                                   "-symbols", "sym",
                                   "-expect", String.valueOf(expectConflicts),
                                   file.getPath()));
          Main.main(args.toArray(new String[0]));
        }
      }
      catch (Exception e) {
        throw new MojoExecutionException("Cup generation failed", e);
      }
    }
  }

  private List<File> getGrammarFiles()
  {
    List<File> fileList = new ArrayList<File>();
    if (sourceDirectory != null) {
      collectGrammarFiles(sourceDirectory, fileList);
    }
    return fileList;
  }

  private void collectGrammarFiles(File directory, List<File> list)
  {
    File[] content = directory.listFiles();
    if (content == null) return;
    for (File file : content) {
      if (file.isDirectory()) collectGrammarFiles(file, list);
      else if (file.getName().endsWith(".cup")) list.add(file);
    }
  }

  private String getPackage(File cupfile)
    throws IOException
  {
    BufferedReader br = new BufferedReader(new FileReader(cupfile));
    while (br.ready()){
      String line = br.readLine();
      if (line.startsWith("package") && line.indexOf(";") != -1)
      {
        return line.substring(8, line.indexOf(";")).trim();
      }
    }
    return "";
  }
}
