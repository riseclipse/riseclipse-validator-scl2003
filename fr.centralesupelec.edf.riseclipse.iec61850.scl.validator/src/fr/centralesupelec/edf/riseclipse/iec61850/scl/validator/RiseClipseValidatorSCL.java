/*
*************************************************************************
**  Copyright (c) 2016-2024 CentraleSupélec & EDF.
**  All rights reserved. This program and the accompanying materials
**  are made available under the terms of the Eclipse Public License v2.0
**  which accompanies this distribution, and is available at
**  https://www.eclipse.org/legal/epl-v20.html
** 
**  This file is part of the RiseClipse tool
**  
**  Contributors:
**      Computer Science Department, CentraleSupélec
**      EDF R&D
**  Contacts:
**      dominique.marcadet@centralesupelec.fr
**      aurelie.dehouck-neveu@edf.fr
**  Web site:
**      https://riseclipse.github.io/
*************************************************************************
*/

package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.ConstructedAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DependsOn;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DocumentRoot;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.PresenceCondition;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SCL;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.utilities.SclModelLoader;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd.NsdValidator;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.FileRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseMessage;
import fr.centralesupelec.edf.riseclipse.util.Severity;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.validation.ocl.OCLValidator;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EValidator.SubstitutionLabelProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.ocl.pivot.validation.ComposedEValidator;
import org.eclipse.ocl.pivot.validation.ValidationRegistryAdapter;

public class RiseClipseValidatorSCL {
    
    private static final String TOOL_VERSION = "1.2.8-SNAPSHOT (01 February 2024)";

    private static final String NSDOC_FILE_EXTENSION = ".nsdoc";
    private static final String APP_NS_FILE_EXTENSION = ".AppNS";
    private static final String SNSD_FILE_EXTENSION = ".snsd";
    private static final String NSD_FILE_EXTENSION = ".nsd";
    private static final String OCL_FILE_EXTENSION = ".ocl";
    private static final String ZIP_FILE_EXTENSION = ".zip";

    private static final String HELP_OPTION                            = "--help";
    private static final String HELP_ENVIRONMENT_OPTION                = "--help-environment";
    

//    private static final String EMERGENCY_OPTION                     = "--emergency";  // NOSONAR
//    private static final String ALERT_OPTION                         = "--alert";      // NOSONAR
//    private static final String CRITICAL_OPTION                      = "--critical";   // NOSONAR
    private static final String ERROR_OPTION                           = "--error";
    private static final String WARNING_OPTION                         = "--warning";
    private static final String NOTICE_OPTION                          = "--notice";
    private static final String INFO_OPTION                            = "--info";
    private static final String DEBUG_OPTION                           = "--debug";
    private static final String LEVEL_OPTION                           =           ERROR_OPTION + " | " + WARNING_OPTION + " | " + NOTICE_OPTION
                                                                         + " | " + INFO_OPTION     + " | " + DEBUG_OPTION ;
    private static final String OUTPUT_OPTION                          = "--output";
    private static final String XSD_OPTION                             = "--xml-schema";
    private static final String FORMAT_OPTION                          = "--format-string";
    
    private static final String MAKE_EXPLICIT_LINKS_OPTION             = "--make-explicit-links";
    private static final String USE_COLOR_OPTION                       = "--use-color";
    private static final String DISPLAY_NSD_MESSAGES_OPTION            = "--display-nsd-messages";
    private static final String DO_NOT_DISPLAY_COPYRIGHT_OPTION        = "--do-not-display-copyright";
    private static final String USE_FILENAMES_STARTING_WITH_DOT_OPTION = "--use-filenames-starting-with-dot";
    
    private static final String RISECLIPSE_VARIABLE_PREFIX                    = "RISECLIPSE_";
    private static final String CONSOLE_LEVEL_VARIABLE_NAME                   = RISECLIPSE_VARIABLE_PREFIX + "CONSOLE_LEVEL";
    private static final String OUTPUT_FILE_VARIABLE_NAME                     = RISECLIPSE_VARIABLE_PREFIX + "OUTPUT_FILE";
    private static final String XSD_FILE_VARIABLE_NAME                        = RISECLIPSE_VARIABLE_PREFIX + "XSD_FILE";
    private static final String USE_COLOR_VARIABLE_NAME                       = RISECLIPSE_VARIABLE_PREFIX + "USE_COLOR";
    private static final String MAKE_EXPLICIT_LINKS_VARIABLE_NAME             = RISECLIPSE_VARIABLE_PREFIX + "MAKE_EXPLICIT_LINKS";
    private static final String DISPLAY_NSD_MESSAGES_VARIABLE_NAME            = RISECLIPSE_VARIABLE_PREFIX + "DISPLAY_NSD_MESSAGES";
    private static final String DO_NOT_DISPLAY_COPYRIGHT_VARIABLE_NAME        = RISECLIPSE_VARIABLE_PREFIX + "DO_NOT_DISPLAY_COPYRIGHT";
    private static final String USE_FILENAMES_STARTING_WITH_DOT_VARIABLE_NAME = RISECLIPSE_VARIABLE_PREFIX + "USE_FILENAMES_STARTING_WITH_DOT";
    private static final String FORMAT_STRING_VARIABLE_NAME                   = RISECLIPSE_VARIABLE_PREFIX + "FORMAT_STRING";

    private static final String FALSE_VARIABLE_VALUE = "FALSE";

//    private static final String EMERGENCY_KEYWORD = "EMERGENCY";  // NOSONAR
//    private static final String ALERT_KEYWORD     = "ALERT";      // NOSONAR
//    private static final String CRITICAL_KEYWORD  = "CRITICAL";   // NOSONAR
    private static final String ERROR_KEYWORD       = "ERROR";
    private static final String WARNING_KEYWORD     = "WARNING";
    private static final String NOTICE_KEYWORD      = "NOTICE";
    private static final String INFO_KEYWORD        = "INFO";
    private static final String DEBUG_KEYWORD       = "DEBUG";

    public  static final String DIAGNOSTIC_SOURCE = "fr.centralesupelec.edf.riseclipse";
    
    private static final String DEFAULT_NAMESPACE_ID = "IEC 61850-7-4";
    private static final Integer DEFAULT_NAMESPACE_VERSION = Integer.valueOf( 2007 );
    private static final String DEFAULT_NAMESPACE_REVISION = "B";
    private static final Integer DEFAULT_NAMESPACE_RELEASE = Integer.valueOf( 1 );
    
    public  static final NsIdentification DEFAULT_NS_IDENTIFICATION = NsIdentification.of(
            DEFAULT_NAMESPACE_ID,
            DEFAULT_NAMESPACE_VERSION,
            DEFAULT_NAMESPACE_REVISION,
            DEFAULT_NAMESPACE_RELEASE
    );
    
    private static final String VALIDATOR_SCL_CATEGORY = "SCL/Validator";
    private static final String INFO_FORMAT_STRING = "%6$s%1$-8s%7$s: %4$s";
    
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;
    
    private static ComposedEValidator composedValidator;
    private static OCLValidator oclValidator;
    private static SclItemProviderAdapterFactory sclAdapter;
    private static SclModelLoader sclLoader;
    private static NsdValidator nsdValidator;

    private static boolean hiddenDoor = false;
    private static boolean makeExplicitLinks = false;
    private static boolean useColor = false;
    private static boolean displayCopyright = true;
    private static boolean displayNsdMessages = false;
    private static boolean keepDotFiles = false;
    private static Severity consoleLevel = Severity.WARNING;
    private static String outputFile = null;
    private static String xsdFile = null;
    private static String formatString = null;
    
    private static List< @NonNull String> oclFiles;
    private static List< @NonNull String > nsdFiles;
    private static List< @NonNull String > sclFiles;

    private static void usage() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setLevel( Severity.INFO );
        console.setFormatString( INFO_FORMAT_STRING );
        
        console.info( VALIDATOR_SCL_CATEGORY, 0, "java -jar RiseClipseValidatorSCL.jar " + HELP_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "java -jar RiseClipseValidatorSCL.jar " + HELP_ENVIRONMENT_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                          "java -jar RiseClipseValidatorSCL.jar"
                        + " [" + LEVEL_OPTION + "]"
                        + " [" + OUTPUT_OPTION + " <file>]"
                        + " [" + MAKE_EXPLICIT_LINKS_OPTION + "]"
                        + " (<directory> | <oclFile> | <nsdFile> | <sclFile> | <zipFile>)+" 
        );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                  "Directories are searched recursively, "
                + "files ending with \".ocl\" are considered OCL files, "
                + "files ending with \".nsd\" are considered NS files, "
                + "files ending with \".snsd\" are considered ServiceNS files, "
                + "files ending with \".AppNS\" are considered ApplicableServiceNS files, "
                + "files ending with \".nsdoc\" are considered NSDoc files, "
                + "files ending with \".zip\" are decompressed and each file inside is taken into account "
                + "(case is ignored for all these extensions), "
                + "all others are considered SCL files" );
        System.exit( -1 );
    }

    private static void help() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setLevel( Severity.INFO );
        console.setFormatString( INFO_FORMAT_STRING );
        
        displayLegal();
        console.info( VALIDATOR_SCL_CATEGORY, 0, "java -jar RiseClipseValidatorSCL.jar option* file*" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tDirectories are searched recursively," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tFiles ending with \".ocl\" are considered OCL files," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tfiles ending with \".nsd\" are considered NS files," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tfiles ending with \".snsd\" are considered ServiceNS files," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tfiles ending with \".AppNS\" are considered ApplicableServiceNS files (at most one should be given)," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tfiles ending with \".nsdoc\" are considered NSDoc files," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tfiles ending with \".zip\" are decompressed and each file inside is taken into account," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tcase is ignored for all these extensions," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\tall others are considered SCL files." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "The following options are recognized:" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + ERROR_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + WARNING_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + NOTICE_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + INFO_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + DEBUG_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tThe amount of messages displayed is chosen according to this option, default is " + WARNING_OPTION + "." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + OUTPUT_OPTION + " <file>" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tmessages are outputed in the given file." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + XSD_OPTION + " <file>" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tA preliminary XML validation is done against the given XML schema file." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + FORMAT_OPTION + " <format-string>" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tmessages are outputed with a java.util.Formatter using the given format string," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\t1$ is severity, 2$ is category, 3$ is line number, 4$ is message, 5$ is filename," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\t6$ is color start, 7$ is color end (these last two are only used if the " + USE_COLOR_OPTION + " option is active)," );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tdefault is '%6$s%1$-7s%7$s: [%2$s] %4$s (%5$s:%3$d)'." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + USE_COLOR_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tcolors (using ANSI escape sequences) are used when displaying messages." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + MAKE_EXPLICIT_LINKS_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, 
                  "\t\tImplicit links in SCL files are made explicit, this is usually needed for complete validation. "
                + "Warnings are displayed when problems are detected. Infos are displayed about explicit links being made. "
                + "Verbosity is about how explicit links are made." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + DISPLAY_NSD_MESSAGES_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, 
                  "\t\tOnly errors detected in NSD files are displayed by default. "
                + "This option allows for other messages to be displayed (according to the chosen level).");
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + DO_NOT_DISPLAY_COPYRIGHT_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tThe tool information is not displayed at the beginning." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + USE_FILENAMES_STARTING_WITH_DOT_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tFiles whose name begins with a dot are not ignored." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + HELP_ENVIRONMENT_OPTION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t\tEnvironment variables used are displayed." );
        System.exit( 0 );
    }
    
    private static void helpEnvironment() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setLevel( Severity.INFO );
        console.setFormatString( INFO_FORMAT_STRING );
        
        displayLegal();
        
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                      "The folowing environment variables may be used in addition to command line options, "
                    + "however, the latter have precedence." );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                      "\t" + CONSOLE_LEVEL_VARIABLE_NAME + ": if its value is one of (ignoring case) "
                    + ERROR_KEYWORD + ", " + WARNING_KEYWORD + ", " + NOTICE_KEYWORD + ", " + INFO_KEYWORD + " or " + DEBUG_KEYWORD
                    + ", then the corresponding level is set, otherwise the variable is ignored." );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                      "\t" + OUTPUT_FILE_VARIABLE_NAME + ": name of the output file for messages." );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                      "\t" + FORMAT_STRING_VARIABLE_NAME + ": string used to format messages (see description of " + FORMAT_OPTION + " option)." );
        console.info( VALIDATOR_SCL_CATEGORY, 0,
                      "\t" + USE_COLOR_VARIABLE_NAME + ": if its value is not equal to FALSE "
                    + "(ignoring case), it is equivalent to the use of " + USE_COLOR_OPTION + " option." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + MAKE_EXPLICIT_LINKS_VARIABLE_NAME + ": if its value is not equal to FALSE "
                    + "(ignoring case), it is equivalent to the use of " + MAKE_EXPLICIT_LINKS_OPTION + " option." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + DISPLAY_NSD_MESSAGES_VARIABLE_NAME + ": if its value is not equal to FALSE "
                    + "(ignoring case), it is equibvalent to the use of " + DISPLAY_NSD_MESSAGES_OPTION + " option." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + DO_NOT_DISPLAY_COPYRIGHT_VARIABLE_NAME + ": if its value is not equal to FALSE "
                    + "(ignoring case), it is equivalent to the use of " + DO_NOT_DISPLAY_COPYRIGHT_OPTION + " option." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "\t" + USE_FILENAMES_STARTING_WITH_DOT_VARIABLE_NAME + ": if its value is not equal to FALSE "
                    + "(ignoring case), it is equivalent to the use of " + USE_FILENAMES_STARTING_WITH_DOT_OPTION + " option." );
        System.exit( 0 );
    }
    
    private static void setOptionsFromEnvironmentVariables() {
        String s = System.getenv( CONSOLE_LEVEL_VARIABLE_NAME );
        if( s != null ) {
                 if( s.equalsIgnoreCase( ERROR_KEYWORD )) {
                consoleLevel  = Severity.ERROR;
            }
            else if( s.equalsIgnoreCase( WARNING_KEYWORD )) {
                consoleLevel  = Severity.WARNING;
            }
            else if( s.equalsIgnoreCase( NOTICE_KEYWORD )) {
                consoleLevel  = Severity.NOTICE;
            }
            else if( s.equalsIgnoreCase( INFO_KEYWORD )) {
                consoleLevel  = Severity.INFO;
            }
            else if( s.equalsIgnoreCase( DEBUG_KEYWORD )) {
                consoleLevel  = Severity.DEBUG;
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning(
                    VALIDATOR_SCL_CATEGORY, 0,
                    "Value of environment variable " + CONSOLE_LEVEL_VARIABLE_NAME + " is not recognized and ignored" );
            }
        }
        
        outputFile = System.getenv( OUTPUT_FILE_VARIABLE_NAME );
        
        xsdFile = System.getenv( XSD_FILE_VARIABLE_NAME );
        
        formatString = System.getenv( FORMAT_STRING_VARIABLE_NAME );
        
        s = System.getenv( USE_COLOR_VARIABLE_NAME );
        if( s != null ) {
            if( ! s.equalsIgnoreCase( FALSE_VARIABLE_VALUE )) {
                useColor = true;
            }
        }
        
        s = System.getenv( MAKE_EXPLICIT_LINKS_VARIABLE_NAME );
        if( s != null ) {
            if( ! s.equalsIgnoreCase( FALSE_VARIABLE_VALUE )) {
                makeExplicitLinks = true;
            }
        }
        
        s = System.getenv( DISPLAY_NSD_MESSAGES_VARIABLE_NAME );
        if( s != null ) {
            if( ! s.equalsIgnoreCase( FALSE_VARIABLE_VALUE )) {
                displayNsdMessages = true;
            }
        }
        
        s = System.getenv( DO_NOT_DISPLAY_COPYRIGHT_VARIABLE_NAME );
        if( s != null ) {
            if( ! s.equalsIgnoreCase( FALSE_VARIABLE_VALUE )) {
                displayCopyright = false;
            }
        }
        
        s = System.getenv( USE_FILENAMES_STARTING_WITH_DOT_VARIABLE_NAME );
        if( s != null ) {
            if( ! s.equalsIgnoreCase( FALSE_VARIABLE_VALUE )) {
                keepDotFiles = true;
            }
        }
    }
    
    public static void main( @NonNull String[] args ) {
        // Do everything in a big try/catch bloc to avoid displaying stack traces on unexpected exceptions
        try {
            if( args.length == 0 ) {
                usage();
            }

            setOptionsFromEnvironmentVariables();

            int posFiles = 0;
            for( int i = 0; i < args.length; ++i ) {
                if( args[i].startsWith( "--" ) ) {
                    posFiles = i + 1;
                    if( HELP_OPTION.equals( args[i] )) {
                        help();
                    }
                    else if( HELP_ENVIRONMENT_OPTION.equals( args[i] )) {
                        helpEnvironment();
                    }
                    else if( ERROR_OPTION.equals( args[i] )) {
                        consoleLevel = Severity.ERROR;
                    }
                    else if( WARNING_OPTION.equals( args[i] )) {
                        consoleLevel = Severity.WARNING;
                    }
                    else if( NOTICE_OPTION.equals( args[i] )) {
                        consoleLevel = Severity.NOTICE;
                    }
                    else if( INFO_OPTION.equals( args[i] )) {
                        consoleLevel = Severity.INFO;
                    }
                    else if( DEBUG_OPTION.equals( args[i] )) {
                        consoleLevel = Severity.DEBUG;
                    }
                    else if( OUTPUT_OPTION.equals( args[i] )) {
                        if( ++i < args.length ) {
                            outputFile = args[i];
                            ++posFiles;
                        }
                        else usage();
                    }
                    else if( XSD_OPTION.equals( args[i] )) {
                        if( ++i < args.length ) {
                            xsdFile = args[i];
                            ++posFiles;
                        }
                        else usage();
                    }
                    else if( MAKE_EXPLICIT_LINKS_OPTION.equals( args[i] )) {
                        makeExplicitLinks = true;
                    }
                    else if( FORMAT_OPTION.equals( args[i] )) {
                        if( ++i < args.length ) {
                            formatString = args[i];
                            ++posFiles;
                        }
                        else usage();
                    }
                    else if( USE_COLOR_OPTION.equals( args[i] )) {
                        useColor = true;
                    }
                    else if( DO_NOT_DISPLAY_COPYRIGHT_OPTION.equals( args[i] )) {
                        displayCopyright = false;
                    }
                    else if( DISPLAY_NSD_MESSAGES_OPTION.equals( args[i] )) {
                        displayNsdMessages = true;
                    }
                    else if( USE_FILENAMES_STARTING_WITH_DOT_OPTION.equals( args[i] )) {
                        keepDotFiles = true;
                    }
                    else if( "--hidden-door".equals( args[i] ) ) {
                        hiddenDoor  = true;
                    }
                    else {
                        AbstractRiseClipseConsole.getConsole().error( VALIDATOR_SCL_CATEGORY, 0, "Unrecognized option " + args[i] );
                        usage();
                    }
                }
            }
            
            IRiseClipseConsole console = ( outputFile == null ) ? new TextRiseClipseConsole( useColor ) : new FileRiseClipseConsole( outputFile );
            if( formatString != null ) console.setFormatString( formatString );
            AbstractRiseClipseConsole.changeConsole( console );
            console.setLevel( consoleLevel );

            if( displayCopyright ) {
                Severity level = console.setLevel( Severity.INFO );
                displayLegal();
                console.setLevel( level );
            }
            
            //console.doNotDisplayIdenticalMessages();  // NOSONAR
            doValidation( args, posFiles );
        }
        catch( Exception unexpected ) {
            IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
            
            try {
                File logFile = File.createTempFile( "RiseClipseUnexpectedException", ".log" );
                try( PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter( logFile )))) {
                    pw.write( "An unexpected Java exception has occured.\n" );
                    pw.write( "Here is the stack trace:\n" );
                    unexpected.printStackTrace( pw );
                    pw.close();
                    
                    console.emergency( VALIDATOR_SCL_CATEGORY, 0, "An unexpected Java exception has occured, the stack trace is available in ", logFile.getAbsolutePath() );
                }
                catch( IOException e ) {
                    console.emergency( VALIDATOR_SCL_CATEGORY, 0, "An unexpected Java exception has occured: ", unexpected.getMessage(), " followed by an IOException: ", e.getMessage() );
                }
                
            }
            catch( IOException e ) {
                console.emergency( VALIDATOR_SCL_CATEGORY, 0, "An unexpected Java exception has occured: ", unexpected.getMessage(), " followed by an IOException: ", e.getMessage() );
            }
        }
    }

    private static void doValidation( @NonNull String[] args, int posFiles ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();

        oclFiles = new ArrayList<>();
        nsdFiles = new ArrayList<>();
        sclFiles = new ArrayList<>();
        for( int i = posFiles; i < args.length; ++i ) {
            getFiles( Paths.get( args[i] ), console );
        }
        
        if( hiddenDoor ) {
            doHiddenDoor();
        }

        prepare( displayNsdMessages );
        int returned_value = EXIT_SUCCESS;
        for( int i = 0; i < sclFiles.size(); ++i ) {
            if( run( makeExplicitLinks, sclFiles.get( i )) == EXIT_FAILURE ) {
                returned_value = EXIT_FAILURE;
            }
        }
        System.exit( returned_value );
    }

    private static void getFiles( Path path, IRiseClipseConsole console ) {
        if( path.getName( path.getNameCount() - 1 ).toString().startsWith( "." )) {
            if( ! keepDotFiles ) {
                console.info( VALIDATOR_SCL_CATEGORY, 0, path, " is ignored because it starts with a dot" );
                return;
            }
        }
        if( Files.isDirectory( path )) {
            try {
                Files.list( path )
                    .forEach( f -> getFiles( f.normalize(), console ));
            }
            catch( IOException e ) {
                console.error( VALIDATOR_SCL_CATEGORY, 0, "got IOException while listing content of directory ", path );
            }
        }
        else if( Files.isReadable( path )) {
            String name = path.toString();
            int dotPos = name.lastIndexOf( "." );
            if( dotPos != -1 ) {
                if( name.substring( dotPos ).equalsIgnoreCase( OCL_FILE_EXTENSION )) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as OCL file ", name );
                    oclFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( NSD_FILE_EXTENSION )) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as NSD file ", name );
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( SNSD_FILE_EXTENSION )) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as NSD file ", name );
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( APP_NS_FILE_EXTENSION )) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as NSD file ", name );
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( NSDOC_FILE_EXTENSION )) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as NSD file ", name );
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( ZIP_FILE_EXTENSION )) {
                    for( String file : getFilesFromZipFile( path, console )) {
                        getFiles( Paths.get( file ).normalize(), console );
                    }
                }
                else {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as SCL file ", name );
                    sclFiles.add( name );
                }
            }
            else {
                console.info( VALIDATOR_SCL_CATEGORY, 0, "adding as SCL file ", name );
                sclFiles.add( name );
            }
        }
        else {
            console.error( VALIDATOR_SCL_CATEGORY, 0, "Cannot read file ", path );
        }
        
    }

    // Code taken partially from https://www.baeldung.com/java-compress-and-uncompress
    // and also from https://stackoverflow.com/questions/9324933/what-is-a-good-java-library-to-zip-unzip-files
    private static @NonNull ArrayList< String > getFilesFromZipFile( @NonNull Path zipPath, @NonNull IRiseClipseConsole console ) {
        @NonNull ArrayList< String > files = new ArrayList<>();
        @NonNull String zipName = zipPath.getFileName().toString();
        zipName = zipName.substring( 0, zipName.lastIndexOf( '.' ));
        try( @NonNull ZipFile zipFile = new ZipFile( zipPath.toFile() )) {
            @NonNull Path unzipDir = Files.createTempDirectory( zipName );
            @NonNull Enumeration< ? extends ZipEntry > entries = zipFile.entries();
            while( entries.hasMoreElements() ) {
                @NonNull ZipEntry zipEntry = entries.nextElement();
                File newFile = newFileFromZipEntry( unzipDir.toFile(), zipEntry, console );
                if( newFile == null ) continue;
                if( zipEntry.isDirectory() ) {
                    if( ! newFile.isDirectory() && ! newFile.mkdirs() ) {
                        console.alert( VALIDATOR_SCL_CATEGORY, 0,
                                       "Failed to create directory for ",
                                       zipEntry.getName(),
                                       ", files after will be ignored" );
                        return files;
                    }
                } 
                else {
                    // fix for Windows-created archives
                    @NonNull File parent = newFile.getParentFile();
                    if( ! parent.isDirectory() && ! parent.mkdirs() ) {
                        console.alert( VALIDATOR_SCL_CATEGORY, 0,
                                "Failed to create directory for ",
                                parent.getName(),
                                ", files after will be ignored" );
                        return files;
                    }

                    // write file content
                    try( @NonNull FileOutputStream out = new FileOutputStream( newFile )) {
                        zipFile.getInputStream( zipEntry ).transferTo( out );
                    }
                    files.add( newFile.getAbsolutePath() );
                }
            }
        }
        catch( IOException e ) {
            console.alert( VALIDATOR_SCL_CATEGORY, 0,
                    "IOException while trying to handle: ",
                    zipPath.toString(),
                    ", it will be ignored" );
        }
        return files;
    }

    // From https://www.baeldung.com/java-compress-and-uncompress
    // The newFile() method guards against writing files to the file system outside the target folder.
    // This vulnerability is called Zip Slip.
    private static File newFileFromZipEntry( @NonNull File destinationDir, @NonNull ZipEntry zipEntry, @NonNull IRiseClipseConsole console ) {
        @NonNull File destFile = new File( destinationDir, zipEntry.getName() );

        try {
            String destDirPath = destinationDir.getCanonicalPath();
            String destFilePath = destFile.getCanonicalPath();

            if( ! destFilePath.startsWith( destDirPath + File.separator )) {
                console.alert( VALIDATOR_SCL_CATEGORY, 0,
                               "Entry: ",
                               zipEntry.getName(),
                               " is outside of the target dir: ",
                               destDirPath,
                               ", it will be ignored" );
                return null;
            }
        }
        catch( IOException e ) {
            console.alert( VALIDATOR_SCL_CATEGORY, 0,
                    "IOException while trying to get path for: ",
                    zipEntry.getName(),
                    ", it will be ignored" );
            return null;
        }

        return destFile;
    }

    @SuppressWarnings( "unused" )
    private static void doHiddenDoor_1() {
            
        prepare( false );
    
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setLevel( Severity.INFO );
        console.setFormatString( INFO_FORMAT_STRING );

        Stream< PresenceCondition > pc = nsdValidator.getNsdLoader().getResourceSet().getPresenceConditionStream( DEFAULT_NS_IDENTIFICATION, true );
        pc.forEach( c -> console.info( VALIDATOR_SCL_CATEGORY, 0, "PresenceCondition ", c.getName() ));
        
        Stream< ConstructedAttribute > ca = nsdValidator.getNsdLoader().getResourceSet().getConstructedAttributeStream( DEFAULT_NS_IDENTIFICATION, true );
        ca.forEach( c -> console.info( VALIDATOR_SCL_CATEGORY, 0, "ConstructedAttribute ", c.getName() ));
        
        System.exit( 0 );
    }
        
//    @SuppressWarnings( "unused" )
    private static void doHiddenDoor() {
            
        prepare( false );
    
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setFormatString( INFO_FORMAT_STRING );
    
        for( int i = 0; i < sclFiles.size(); ++i ) {
            console.setLevel( Severity.WARNING );
            sclLoader.reset();
            Resource resource = sclLoader.loadWithoutValidation( sclFiles.get( i ));
            sclLoader.finalizeLoad( console );
            if( resource.getContents().size() == 0 ) continue;
            console.setLevel( Severity.INFO );

            SCL scl = ( SCL ) resource.getContents().get( 0 );
            scl
            .getIED()
            .stream()
            .forEach( ied -> {
                console.info( VALIDATOR_SCL_CATEGORY, 0, "IED: " + ied.getName() + " line: " + ied.getLineNumber() );
                ied
                .getAccessPoint()
                .stream()
                .forEach( ap -> {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "  AccessPoint: " + ap.getName() + " line: " + ap.getLineNumber() );
                    if( ap.getServer() != null ) {
                        ap
                        .getServer()
                        .getLDevice()
                        .stream()
                        .forEach( ld -> {
                            console.info( VALIDATOR_SCL_CATEGORY, 0, "  LDevice: " + ld.getInst() + " line: " + ld.getLineNumber() + "\t\t" + ld.getNamespace() );
                            console.info( VALIDATOR_SCL_CATEGORY, 0, "    LN: " + ld.getLN0().getLnClass() + "\t\t\t" + ld.getLN0().getNamespace() );
                            ld
                            .getLN()
                            .stream()
                            .forEach( ln -> {
                                console.info( VALIDATOR_SCL_CATEGORY, 0, "    LN: " + ln.getLnClass() + " line: " + ln.getLineNumber() + "\t\t\t" + ln.getNamespace() );
                                ln
                                .getDOI()
                                .stream()
                                .forEach( doi -> {
                                    console.info( VALIDATOR_SCL_CATEGORY, 0, "      DOI: " + doi.getName() + " line: " + doi.getLineNumber() + "\t\t\t" + doi.getNamespace() );
//                                    doi
//                                    .getDAI()
//                                    .stream()
//                                    .forEach( dai -> {
//                                        console.info( VALIDATOR_SCL_CATEGORY, 0, "        DAI: " + dai.getName() + "\t\t\t" + dai.getNamespace() );
//                                    });
                                });
                            });
                        });
                    }
                });
            });
            
        }
        
        System.exit( 0 );
    }

    @SuppressWarnings( "unused" )
    private static void doHiddenDoor_3() {
        
        prepare( false );

        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setFormatString( INFO_FORMAT_STRING );

        for( int i = 0; i < sclFiles.size(); ++i ) {
            console.setLevel( Severity.WARNING );
            sclLoader.reset();
            Resource resource = sclLoader.loadWithoutValidation( sclFiles.get( i ));
            sclLoader.finalizeLoad( console );
            console.setLevel( Severity.INFO );
            SCL scl = ( SCL ) resource.getContents().get( 0 );
            
            for( TreeIterator< ? extends EObject > t = EcoreUtil.getAllContents( Collections.singleton( scl ) ); t.hasNext(); ) {
                EObject child = t.next();
                console.info( VALIDATOR_SCL_CATEGORY, 0, child.getClass().getName() );
            }

        }
        
        System.exit( 0 );
    }
    
    @SuppressWarnings( "unused" )
    private static void doHiddenDoor_4() {
            
        prepare( false );

        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        console.setLevel( Severity.INFO );
        console.setFormatString( INFO_FORMAT_STRING );

        for( int i = 0; i < nsdValidator.getNsdLoader().getResourceSet().getResources().size(); ++i ) {
            Resource resource = nsdValidator.getNsdLoader().getResourceSet().getResources().get( i );
            DocumentRoot root = (DocumentRoot) resource.getContents().get( 0 );
            if( root.getNS() != null ) {
                console.info( VALIDATOR_SCL_CATEGORY, 0, "Id: " + root.getNS().getId() );
                console.info( VALIDATOR_SCL_CATEGORY, 0, "Version: " + root.getNS().getVersion() + "-" + root.getNS().getRevision()  + root.getNS().getRelease() + "-" + root.getNS().getPublicationStage() );
                DependsOn dependsOn = root.getNS().getDependsOn();
                if( dependsOn != null ) {
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "DependsOn Id: " + dependsOn.getId() );
                    console.info( VALIDATOR_SCL_CATEGORY, 0, "DependsOn Version: " + dependsOn.getVersion() + "-"  + dependsOn.getRevision()  + dependsOn.getRelease() + "-" + dependsOn.getPublicationStage() );
                    if( dependsOn.getRefersToNS() != null ) {
                        console.info( VALIDATOR_SCL_CATEGORY, 0, "DependsOn.refersToNS found " );
                    }
                    else {
                        console.info( VALIDATOR_SCL_CATEGORY, 0, "DependsOn.refersToNS NOT FOUND " );                        
                    }
                }
            }
            console.info( VALIDATOR_SCL_CATEGORY, 0, "" );
        }
        
        System.exit( 0 );
    }
    
    // public because used by ui
    public static void displayLegal() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        Severity oldLevel = console.setLevel( Severity.INFO );
        String oldFormat = console.setFormatString( INFO_FORMAT_STRING );
        
        console.info( VALIDATOR_SCL_CATEGORY, 0, "Copyright (c) 2016-2024 CentraleSupélec & EDF." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "All rights reserved. This program and the accompanying materials" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "are made available under the terms of the Eclipse Public License v2.0" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "which accompanies this distribution, and is available at" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "https://www.eclipse.org/legal/epl-v20.html" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "This tool is part of RiseClipse." );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "Contributors:" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "    Computer Science Department, CentraleSupélec" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "    EDF R&D" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "Contacts:" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "    dominique.marcadet@centralesupelec.fr" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "    aurelie.dehouck-neveu@edf.fr" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "Web site:" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "    https://riseclipse.github.io/" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "" );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "RiseClipseValidatorSCL version: " + TOOL_VERSION );
        console.info( VALIDATOR_SCL_CATEGORY, 0, "" );

        console.setFormatString( oldFormat );
        console.setLevel( oldLevel );
    }

    // public because used by ui
    public static void prepare( List< String > oclFileNames, List< String > nsdFileNames, boolean displayNsdMessages ) {
        oclFiles = new ArrayList<>( oclFileNames );
        nsdFiles = new ArrayList<>( nsdFileNames );
        prepare( displayNsdMessages );
    }

    private static void prepare( boolean displayNsdMessages ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        SclPackage sclPg = SclPackage.eINSTANCE;
        if( sclPg == null ) {
            console.emergency( VALIDATOR_SCL_CATEGORY, 0, "SCL package not found" );
            return;
        }
        
        composedValidator = new ComposedEValidator( null );

        if(( oclFiles != null ) && ( ! oclFiles.isEmpty() )) {
            oclValidator = new OCLValidator( sclPg, console );

            for( int i = 0; i < oclFiles.size(); ++i ) {
                oclValidator.addOCLDocument( oclFiles.get( i ), console );
            }
            oclValidator.prepare( console );
            composedValidator.addChild( oclValidator );
        }

        if(( nsdFiles != null ) && ( ! nsdFiles.isEmpty() )) {
            nsdValidator = new NsdValidator( sclPg );
            for( int i = 0; i < nsdFiles.size(); ++i ) {
                nsdValidator.addNsdDocument( nsdFiles.get( i ), console );
            }
            nsdValidator.prepare( console, displayNsdMessages );
            composedValidator.addChild( nsdValidator );
        }

        sclLoader = new SclModelLoader();
        sclAdapter = new SclItemProviderAdapterFactory();

        if( xsdFile != null ) {
            XSDValidator.prepare( xsdFile );
        }
    }

    // public because used by ui
    public static int run( boolean makeExplicitLinks, @NonNull String sclFile ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        if( xsdFile != null ) {
            XSDValidator.validate( sclFile );
        }
        
        sclLoader.reset();
        Resource resource = sclLoader.loadWithoutValidation( sclFile );
        if( makeExplicitLinks ) {
            console.info( VALIDATOR_SCL_CATEGORY, 0, "Making explicit links for file: " + sclFile );
            sclLoader.finalizeLoad( console );
        }
        if( resource != null ) {
            console.info( VALIDATOR_SCL_CATEGORY, 0, "Validating file: " + sclFile );
            // Some attributes must be re-initalialized
            if( nsdValidator != null ) nsdValidator.reset();
            // Not needed for the OCL validator
            // if( oclValidator != null ) oclValidator.reset();  // NOSONAR
            return validate( resource, sclAdapter );
        }
        return EXIT_SUCCESS;
    }

    private static int validate( @NonNull Resource resource, final AdapterFactory sclAdapter ) {
        int returned_value = EXIT_SUCCESS;
        if( resource.getContents().isEmpty() ) return returned_value;

        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        Map< Object, Object > context = new HashMap<>();
        SubstitutionLabelProvider substitutionLabelProvider = new EValidator.SubstitutionLabelProvider() {

            @Override
            public String getValueLabel( EDataType eDataType, Object value ) {
                return Diagnostician.INSTANCE.getValueLabel( eDataType, value );
            }

            @Override
            public String getObjectLabel( EObject eObject ) {
            	// plugin.properties files are not included in a fat jar when it is created
            	// with Export… → Java → Runnable JAR file, leading to IllegalArgumentException.
            	// If a string is missing, this is MissingResourceException
                // A NPE may also happen if eObject has no label provider (not an object of our metamodels)
            	try {
            		IItemLabelProvider labelProvider = ( IItemLabelProvider ) sclAdapter.adapt( eObject,
            				IItemLabelProvider.class );
            		return labelProvider.getText( eObject );
            	}
            	catch( NullPointerException | IllegalArgumentException | MissingResourceException ex ) {
            		return eObject.eClass().getName();
            	}
            }

            @Override
            public String getFeatureLabel( EStructuralFeature eStructuralFeature ) {
                return Diagnostician.INSTANCE.getFeatureLabel( eStructuralFeature );
            }
        };
        context.put( EValidator.SubstitutionLabelProvider.class, substitutionLabelProvider );
        
        // The resource should have only one root element, an SCL object.
        // If there are other objects, it means that something is wrong in the SCL file
        // and it is useless to try to validate them.
        if( resource.getContents().get( 0 ) instanceof SCL  ) {
            @NonNull
            ValidationRegistryAdapter adapter = ValidationRegistryAdapter.getAdapter( sclLoader.getResourceSet() );
            adapter.put( SclPackage.eINSTANCE, composedValidator );
            Diagnostician diagnostician = new Diagnostician( adapter );
            Diagnostic diagnostics = diagnostician.validate( resource.getContents().get( 0 ), context );

            for( Iterator< Diagnostic > i = diagnostics.getChildren().iterator(); i.hasNext(); ) {
                Diagnostic childDiagnostic = i.next();
                
                List< ? > data = childDiagnostic.getData();
                if(( data.size() == 2 ) && ( data.get( 1 ) instanceof RiseClipseMessage )) {
                    // Message from NSD validation added in diagnostic
                    @NonNull RiseClipseMessage message = ( RiseClipseMessage ) data.get( 1 );
                    if( message.getSeverity().compareTo( Severity.ERROR ) <= 0 ) {
                        returned_value = EXIT_FAILURE;
                    }
                    console.output( message );
                    continue;
                }
                String message = childDiagnostic.getMessage();
                String[] parts = message.split( ";" );
                if( parts.length == 4 ) {
                    Severity severity = Severity.ERROR;
                    try {
                        severity = Severity.valueOf( parts[0] );
                        if( severity.compareTo( Severity.ERROR ) <= 0 ) {
                            returned_value = EXIT_FAILURE;
                        }
                    }
                    catch( IllegalArgumentException ex ) {}
                    if( parts[1].startsWith( "OCL" )) {
                        // This should be a standard RiseClipse OCL message without the filename
                        // (before 15 April 2022)
                        int line = 0;
                        try {
                            line = Integer.valueOf( parts[2] );
                        }
                        catch( NumberFormatException ex ) {}
                        console.output( new RiseClipseMessage( severity, parts[1], line, parts[3] ));
                    }
                    else {
                        // This should be an IEC WG10-OCL-TF formatted message
                        int line = 0;
                        try {
                            line = Integer.valueOf( parts[3].substring( "line_".length() ));
                        }
                        catch( NumberFormatException ex ) {}
                        console.output( new RiseClipseMessage( severity, parts[1], line, parts[2] ));
                    }
                }
                else if(( parts.length == 5 ) && ( parts[1].startsWith( "OCL" ))) {
                    // This should be a standard RiseClipse OCL message with the added filename
                    // (after 15 April 2022)
                    Severity severity = Severity.ERROR;
                    try {
                        severity = Severity.valueOf( parts[0] );
                        if( severity.compareTo( Severity.ERROR ) <= 0 ) {
                            returned_value = EXIT_FAILURE;
                        }
                    }
                    catch( IllegalArgumentException ex ) {}
                    int line = 0;
                    try {
                        line = Integer.valueOf( parts[3] );
                    }
                    catch( NumberFormatException ex ) {}
                    console.output( new RiseClipseMessage( severity, parts[1], parts[2], line, parts[4] ));
                }
                else {
                    console.warning( VALIDATOR_SCL_CATEGORY, 0, "The structure of the following diagnostic message was not recognized by RiseClipseValidatorSCL" );
                    console.warning( VALIDATOR_SCL_CATEGORY, 0, message );
                }
                
                // The following was used before, therefore it was considered useful.
                // It is kept in case the need arises again.
                /*
                EObject object = ( EObject ) data.get( 0 );
                if(( data.size() > 1 ) && ( data.get( 1 ) instanceof EAttribute ) && ( ! childDiagnostic.getChildren().isEmpty() )) {
                    EAttribute attribute = ( EAttribute ) data.get( 1 );
                    if( attribute == null ) continue;
                    message = "\tAttribute " + attribute.getName() + " of "
                                + substitutionLabelProvider.getObjectLabel( object ) + " : "
                                + childDiagnostic.getChildren().get( 0 ).getMessage();
                }
                */
            }
            
        }
        return returned_value;
    }

}
