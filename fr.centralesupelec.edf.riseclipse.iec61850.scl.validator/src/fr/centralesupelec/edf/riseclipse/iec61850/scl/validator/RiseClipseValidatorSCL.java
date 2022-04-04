/*
*************************************************************************
**  Copyright (c) 2016-2022 CentraleSupélec & EDF.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SCL;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.utilities.SclModelLoader;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd.NsdValidator;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.FileRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseFatalException;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.validation.ocl.OCLValidator;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
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

public class RiseClipseValidatorSCL {
    
    private static final String TOOL_VERSION = "1.2.1-SNAPSHOT (04 April 2022)";

    private static final String NSDOC_FILE_EXTENSION = ".nsdoc";
    private static final String APP_NS_FILE_EXTENSION = ".AppNS";
    private static final String SNSD_FILE_EXTENSION = ".snsd";
    private static final String NSD_FILE_EXTENSION = ".nsd";
    private static final String OCL_FILE_EXTENSION = ".ocl";

    private static final String HELP_OPTION                            = "--help";
    private static final String HELP_ENVIRONMENT_OPTION                = "--help-environment";
    
    private static final String VERBOSE_OPTION                         = "--verbose";
    private static final String INFO_OPTION                            = "--info";
    private static final String WARNING_OPTION                         = "--warning";
    private static final String ERROR_OPTION                           = "--error";
    private static final String LEVEL_OPTION                           = VERBOSE_OPTION + " | " + INFO_OPTION + " | " + WARNING_OPTION + " | " + ERROR_OPTION;
    private static final String OUTPUT_OPTION                          = "--output";
    private static final String XSD_OPTION                             = "--xml-schema";
    
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

    private static final String FALSE_VARIABLE_VALUE = "FALSE";

    private static final String VERBOSE_KEYWORD = "VERBOSE";
    private static final String INFO_KEYWORD    = "INFO";
    private static final String WARNING_KEYWORD = "WARNING";
    private static final String ERROR_KEYWORD   = "ERROR";

    //private static final String VERBOSE_PREFIX = VERBOSE_KEYWORD + ":";
    private static final String WARNING_PREFIX = WARNING_KEYWORD + ":";
    private static final String INFO_PREFIX = INFO_KEYWORD + ":";
    private static final String ERROR_PREFIX = ERROR_KEYWORD + ":";

    public static final String DIAGNOSTIC_SOURCE = "fr.centralesupelec.edf.riseclipse";
    
    private static final String DEFAULT_NAMESPACE_ID = "IEC 61850-7-4";
    private static final Integer DEFAULT_NAMESPACE_VERSION = Integer.valueOf( 2007 );
    private static final String DEFAULT_NAMESPACE_REVISION = "B";
    private static final Integer DEFAULT_NAMESPACE_RELEASE = Integer.valueOf( 1 );
    
    public static final NsIdentification DEFAULT_NS_IDENTIFICATION = new NsIdentification(
            DEFAULT_NAMESPACE_ID,
            DEFAULT_NAMESPACE_VERSION,
            DEFAULT_NAMESPACE_REVISION,
            DEFAULT_NAMESPACE_RELEASE
    );
    
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
    private static int consoleLevel = IRiseClipseConsole.WARNING_LEVEL;
    private static String outputFile = null;
    private static String xsdFile = null;
    
    private static List< @NonNull String> oclFiles;
    private static List< @NonNull String > nsdFiles;
    private static List< @NonNull String > sclFiles;

    private static void usage() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        console.info( "java -jar RiseClipseValidatorSCL.jar " + HELP_OPTION );
        console.info( "java -jar RiseClipseValidatorSCL.jar " + HELP_ENVIRONMENT_OPTION );
        console.info( "java -jar RiseClipseValidatorSCL.jar"
                        + " [" + LEVEL_OPTION + "]"
                        + " [" + OUTPUT_OPTION + " <file>]"
                        + " [" + MAKE_EXPLICIT_LINKS_OPTION + "]"
                        + " (<oclFile> | <nsdFile> | <sclFile>)+" 
        );
        console.info( "Files ending with \".ocl\" are considered OCL files, "
                + "files ending with \".nsd\" are considered NS files, "
                + "files ending with \".snsd\" are considered ServiceNS files, "
                + "files ending with \".AppNS\" are considered ApplicableServiceNS files, "
                + "files ending with \".nsdoc\" are considered NSDoc files "
                + " (case is ignored for all these extensions), "
                + "all others are considered SCL files" );
        System.exit( -1 );
    }

    private static void help() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        displayLegal();
        console.info( "java -jar RiseClipseValidatorSCL.jar option* file*" );
        console.info( "\tDirectories are searched recursively," );
        console.info( "\tFiles ending with \".ocl\" are considered OCL files," );
        console.info( "\tfiles ending with \".nsd\" are considered NS files," );
        console.info( "\tfiles ending with \".snsd\" are considered ServiceNS files," );
        console.info( "\tfiles ending with \".AppNS\" are considered ApplicableServiceNS files (at most one should be given)," );
        console.info( "\tfiles ending with \".nsdoc\" are considered NSDoc files," );
        console.info( "\tcase is ignored for all these extensions," );
        console.info( "\tall others are considered SCL files." );
        console.info( "" );
        console.info( "The following options are recognized:" );
        console.info( "\t" + VERBOSE_OPTION );
        console.info( "\t" + INFO_OPTION );
        console.info( "\t" + WARNING_OPTION );
        console.info( "\t" + ERROR_OPTION );
        console.info( "\t\tThe amount of messages displayed is chosen according to this option, default is " + WARNING_OPTION + "." );
        console.info( "\t" + OUTPUT_OPTION + " <file>" );
        console.info( "\t\tmessages are outputed in the given file" );
        console.info( "\t" + XSD_OPTION + " <file>" );
        console.info( "\t\tA preliminary XML validation is done against the given XML schema file" );
        console.info( "\t" + USE_COLOR_OPTION );
        console.info( "\t\tcolors (using ANSI escape sequences) are used on message prefixes." );
        console.info( "\t" + MAKE_EXPLICIT_LINKS_OPTION );
        console.info( "\t\tImplicit links in SCL files are made explicit, this is usually needed for complete validation. "
                + "Warnings are displayed when problems are detected. Infos are displayed about explicit links being made. "
                + "Verbosity is about how explicit links are made." );
        console.info( "\t" + DISPLAY_NSD_MESSAGES_OPTION );
        console.info( "\t\tOnly errors detected in NSD files are displayed by default. "
                + "This option allows for other messages to be displayed (according to the chosen level).");
        console.info( "\t" + DO_NOT_DISPLAY_COPYRIGHT_OPTION );
        console.info( "\t\tThe tool information is not displayed at the beginning." );
        console.info( "\t" + USE_FILENAMES_STARTING_WITH_DOT_OPTION );
        console.info( "\t\tFiles whose name begins with a dot are not ignored." );
        console.info( "\t" + HELP_ENVIRONMENT_OPTION );
        console.info( "\t\tEnvironment variables used are displayed." );
        System.exit( 0 );
    }
    
    private static void helpEnvironment() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        displayLegal();
        
        console.info( "The folowing environment variables may be used in addition to command line options, "
                    + "however, the latter have precedence." );
        console.info( "\t" + CONSOLE_LEVEL_VARIABLE_NAME + ": if its value is one of (ignoring case) "
                    + VERBOSE_KEYWORD + ", " + INFO_KEYWORD + ", " + WARNING_KEYWORD + " or " + ERROR_KEYWORD
                    + ", then the corresponding level is set, otherwise the variable is ignored." );
        console.info( "\t" + OUTPUT_FILE_VARIABLE_NAME + ": name of the output file for messages." );
        console.info( "\t" + XSD_FILE_VARIABLE_NAME + ": path to the SCL XML schema." );
        console.info( "\t" + USE_COLOR_VARIABLE_NAME + ": if its value is not equal to FALSE "
                + "(ignoring case), it is equivalent to the use of " + USE_COLOR_OPTION + " option." );
        console.info( "\t" + MAKE_EXPLICIT_LINKS_VARIABLE_NAME + ": if its value is not equal to FALSE "
                + "(ignoring case), it is equivalent to the use of " + MAKE_EXPLICIT_LINKS_OPTION + " option." );
        console.info( "\t" + DISPLAY_NSD_MESSAGES_VARIABLE_NAME + ": if its value is not equal to FALSE "
                + "(ignoring case), it is equibvalent to the use of " + DISPLAY_NSD_MESSAGES_OPTION + " option." );
        console.info( "\t" + DO_NOT_DISPLAY_COPYRIGHT_VARIABLE_NAME + ": if its value is not equal to FALSE "
                + "(ignoring case), it is equivalent to the use of " + DO_NOT_DISPLAY_COPYRIGHT_OPTION + " option." );
        console.info( "\t" + USE_FILENAMES_STARTING_WITH_DOT_VARIABLE_NAME + ": if its value is not equal to FALSE "
                + "(ignoring case), it is equivalent to the use of " + USE_FILENAMES_STARTING_WITH_DOT_OPTION + " option." );
        System.exit( 0 );
    }
    
    private static void setOptionsFromEnvironmentVariables() {
        String s = System.getenv( CONSOLE_LEVEL_VARIABLE_NAME );
        if( s != null ) {
            if( s.equalsIgnoreCase( VERBOSE_KEYWORD )) {
                consoleLevel = IRiseClipseConsole.VERBOSE_LEVEL;
            }
            else if( s.equalsIgnoreCase( INFO_KEYWORD )) {
                consoleLevel = IRiseClipseConsole.INFO_LEVEL;
            }
            else if( s.equalsIgnoreCase( WARNING_KEYWORD )) {
                consoleLevel = IRiseClipseConsole.WARNING_LEVEL;
            }
            else if( s.equalsIgnoreCase( ERROR_KEYWORD )) {
                consoleLevel = IRiseClipseConsole.ERROR_LEVEL;
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "Value of environment variable " + CONSOLE_LEVEL_VARIABLE_NAME + " is not recognized and ignored" );
            }
        }
        
        outputFile = System.getenv( OUTPUT_FILE_VARIABLE_NAME );
        
        xsdFile = System.getenv( XSD_FILE_VARIABLE_NAME );
        
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
                else if( VERBOSE_OPTION.equals( args[i] )) {
                    consoleLevel = IRiseClipseConsole.VERBOSE_LEVEL;
                }
                else if( INFO_OPTION.equals( args[i] )) {
                    consoleLevel = IRiseClipseConsole.INFO_LEVEL;
                }
                else if( WARNING_OPTION.equals( args[i] )) {
                    consoleLevel = IRiseClipseConsole.WARNING_LEVEL;
                }
                else if( ERROR_OPTION.equals( args[i] )) {
                    consoleLevel = IRiseClipseConsole.ERROR_LEVEL;
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
                    AbstractRiseClipseConsole.getConsole().error( "Unrecognized option " + args[i] );
                    usage();
                }
            }
        }
        
        IRiseClipseConsole console = ( outputFile == null ) ? new TextRiseClipseConsole( useColor ) : new FileRiseClipseConsole( outputFile );
        AbstractRiseClipseConsole.changeConsole( console );
        console.setLevel( consoleLevel );

        if( displayCopyright ) {
            int level = console.setLevel( IRiseClipseConsole.INFO_LEVEL );
            displayLegal();
            console.setLevel( level );
        }
        
        //console.doNotDisplayIdenticalMessages();

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
        for( int i = 0; i < sclFiles.size(); ++i ) {
            run( makeExplicitLinks, sclFiles.get( i ));
        }
    }

    private static void getFiles( Path path, IRiseClipseConsole console ) {
        if( path.getName( path.getNameCount() - 1 ).toString().startsWith( "." )) {
            if( ! keepDotFiles ) {
                console.info( path + " is ignored because it starts with a dot" );
                return;
            }
        }
        if( Files.isDirectory( path )) {
            try {
                Files.list( path )
                    .forEach( f -> getFiles( f.normalize(), console ));
            }
            catch( IOException e ) {
                console.error( "got IOException while listing content of directory " + path );
            }
        }
        else if( Files.isReadable( path )) {
            String name = path.toString();
            int dotPos = name.lastIndexOf( "." );
            if( dotPos != -1 ) {
                if( name.substring( dotPos ).equalsIgnoreCase( OCL_FILE_EXTENSION )) {
                    oclFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( NSD_FILE_EXTENSION )) {
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( SNSD_FILE_EXTENSION )) {
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( APP_NS_FILE_EXTENSION )) {
                    nsdFiles.add( name );
                }
                else if( name.substring( dotPos ).equalsIgnoreCase( NSDOC_FILE_EXTENSION )) {
                    nsdFiles.add( name );
                }
                else {
                    sclFiles.add( name );
                }
            }
            else {
                sclFiles.add( name );
            }
        }
        else {
            console.error(  "Cannot read file " + path );
        }
        
    }

    private static void doHiddenDoor() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        prepare( false );
        
//        Stream< PresenceCondition > pc = nsdValidator.getNsdLoader().getResourceSet().getPresenceConditionStream( DEFAULT_NS_IDENTIFICATION );
//        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
//        pc.forEach( c -> console.info(  "PresenceCondition " + c.getName() ));
        
//        Stream< ConstructedAttribute > ca = nsdValidator.getNsdLoader().getResourceSet().getConstructedAttributeStream( DEFAULT_NS_IDENTIFICATION );
//        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
//        ca.forEach( c -> console.info(  "ConstructedAttribute " + c.getName() ));
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        for( int i = 0; i < sclFiles.size(); ++i ) {
            sclLoader.reset();
            Resource resource = sclLoader.loadWithoutValidation( sclFiles.get( i ));
            sclLoader.finalizeLoad( console );
            SCL scl = ( SCL ) resource.getContents().get( 0 );
            scl
            .getIED()
            .stream()
            .forEach( ied -> {
                console.info(  "IED: " + ied.getName() );
                ied
                .getAccessPoint()
                .stream()
                .forEach( ap -> {
                    console.info(  "  AccessPoint: " + ap.getName() );
                    if( ap.getServer() != null ) {
                        ap
                        .getServer()
                        .getLDevice()
                        .stream()
                        .forEach( ld -> {
                            console.info(  "  LDevice: " + ld.getInst() + "\t\t" + ld.getNamespace() );
                            console.info(  "    LN: " + ld.getLN0().getLnClass() + "\t\t\t" + ld.getLN0().getNamespace() );
                            ld
                            .getLN()
                            .stream()
                            .forEach( ln -> {
                                console.info(  "    LN: " + ln.getLnClass() + "\t\t\t" + ln.getNamespace() );
                                ln
                                .getDOI()
                                .stream()
                                .forEach( doi -> {
                                    console.info(  "      DOI: " + doi.getName() + "\t\t\t" + doi.getNamespace() );
//                                    doi
//                                    .getDAI()
//                                    .stream()
//                                    .forEach( dai -> {
//                                        console.info(  "        DAI: " + dai.getName() + "\t\t\t" + dai.getNamespace() );
//                                    });
                                });
                            });
                        });
                    }
                });
            });
            
            for( TreeIterator< ? extends EObject > t = EcoreUtil.getAllContents( Collections.singleton( scl ) ); t.hasNext(); ) {
                EObject child = t.next();
                console.info( child.getClass().getName() );
            }

        }
        
        System.exit( 0 );
    }

    // public because used by ui
    public static void displayLegal() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.info( "Copyright (c) 2016-2021 CentraleSupélec & EDF." );
        console.info( "All rights reserved. This program and the accompanying materials" );
        console.info( "are made available under the terms of the Eclipse Public License v2.0" );
        console.info( "which accompanies this distribution, and is available at" );
        console.info( "https://www.eclipse.org/legal/epl-v20.html" );
        console.info( "" );
        console.info( "This tool is part of RiseClipse." );
        console.info( "Contributors:" );
        console.info( "    Computer Science Department, CentraleSupélec" );
        console.info( "    EDF R&D" );
        console.info( "Contacts:" );
        console.info( "    dominique.marcadet@centralesupelec.fr" );
        console.info( "    aurelie.dehouck-neveu@edf.fr" );
        console.info( "Web site:" );
        console.info( "    https://riseclipse.github.io/" );
        console.info( "" );
        console.info( "RiseClipseValidatorSCL version: " + TOOL_VERSION );
        console.info( "" );
    }

    // public because used by ui
    public static void prepare( List< String > oclFileNames, List< String > nsdFileNames, boolean displayNsdMessages ) {
        oclFiles = oclFileNames;
        nsdFiles = nsdFileNames;
        prepare( displayNsdMessages );
    }

    private static void prepare( boolean displayNsdMessages ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        SclPackage sclPg = SclPackage.eINSTANCE;
        if( sclPg == null ) {
            throw new RiseClipseFatalException( "SCL package not found", null );
        }

        ComposedEValidator validator = ComposedEValidator.install( sclPg );

        if(( oclFiles != null ) && ( ! oclFiles.isEmpty() )) {
            oclValidator = new OCLValidator( sclPg, console );

            for( int i = 0; i < oclFiles.size(); ++i ) {
                oclValidator.addOCLDocument( oclFiles.get( i ), console );
            }
            oclValidator.prepare( validator, console );
        }

        if(( nsdFiles != null ) && ( ! nsdFiles.isEmpty() )) {
            // There are some static attributes
            NsdValidator.initialize();
            
            nsdValidator = new NsdValidator( sclPg );
            for( int i = 0; i < nsdFiles.size(); ++i ) {
                nsdValidator.addNsdDocument( nsdFiles.get( i ), console );
            }
            nsdValidator.prepare( validator, console, displayNsdMessages );
        }

        sclLoader = new SclModelLoader();
        sclAdapter = new SclItemProviderAdapterFactory();

        if( xsdFile != null ) {
            XSDValidator.prepare( xsdFile );
        }
    }

    // public because used by ui
    public static void run( boolean make_explicit_links, @NonNull String sclFile ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        if( xsdFile != null ) {
            XSDValidator.validate( sclFile );
        }
        
        sclLoader.reset();
        Resource resource = sclLoader.loadWithoutValidation( sclFile );
        if( make_explicit_links ) {
            console.info( "Making explicit links for file: " + sclFile );
            sclLoader.finalizeLoad( console );
        }
        if( resource != null ) {
            console.info( "Validating file: " + sclFile );
            // Some attributes must be re-initalialized
            if( nsdValidator != null ) nsdValidator.reset();
            // Not needed for the OCL validator
            // if( oclValidator != null ) oclValidator.reset();
            validate( resource, sclAdapter );
        }
    }

    private static void validate( @NonNull Resource resource, final AdapterFactory adapter ) {
        if( resource.getContents().isEmpty() ) return;

        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        Map< Object, Object > context = new HashMap< Object, Object >();
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
            		IItemLabelProvider labelProvider = ( IItemLabelProvider ) adapter.adapt( eObject,
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
            Diagnostic diagnostic = Diagnostician.INSTANCE.validate( resource.getContents().get( 0 ), context );

            for( Iterator< Diagnostic > i = diagnostic.getChildren().iterator(); i.hasNext(); ) {
                Diagnostic childDiagnostic = i.next();
                
                List< ? > data = childDiagnostic.getData();
                EObject object = ( EObject ) data.get( 0 );
                String message = childDiagnostic.getMessage();
                if(( data.size() > 1 ) && ( data.get( 1 ) instanceof EAttribute ) && ( ! childDiagnostic.getChildren().isEmpty() )) {
                    EAttribute attribute = ( EAttribute ) data.get( 1 );
                    if( attribute == null ) continue;
                    message = "\tAttribute " + attribute.getName() + " of "
                                + substitutionLabelProvider.getObjectLabel( object ) + " : "
                                + childDiagnostic.getChildren().get( 0 ).getMessage();
                }

                // use severity given by OCL message if available
                int severity = childDiagnostic.getSeverity();
                if( message.startsWith( INFO_PREFIX )) {
                    severity = Diagnostic.INFO;
                    message = message.substring( INFO_PREFIX.length() );
                }
                else if( message.startsWith( WARNING_PREFIX )) {
                    severity = Diagnostic.WARNING;
                    message = message.substring( WARNING_PREFIX.length() );
                }
                else if( message.startsWith( ERROR_PREFIX )) {
                    severity = Diagnostic.ERROR;
                    message = message.substring( ERROR_PREFIX.length() );
                }
                switch( severity ) {
                case Diagnostic.INFO:
                    console.info( message );
                    break;
                case Diagnostic.WARNING:
                    console.warning( message );
                    break;
                case Diagnostic.ERROR:
                    console.error( message );
                    break;
                }
            }
        }
    }

}
