/**
 *  Copyright (c) 2019 CentraleSupélec & EDF.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  This file is part of the RiseClipse tool
 *  
 *  Contributors:
 *      Computer Science Department, CentraleSupélec
 *      EDF R&D
 *  Contacts:
 *      dominique.marcadet@centralesupelec.fr
 *      aurelie.dehouck-neveu@edf.fr
 *  Web site:
 *      http://wdi.supelec.fr/software/RiseClipse/
 */
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SCL;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.utilities.SclModelLoader;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd.NsdValidator;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseFatalException;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.validation.ocl.OCLValidator;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EValidator.SubstitutionLabelProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.ocl.pivot.validation.ComposedEValidator;

public class RiseClipseValidatorSCL {

    public static final String DIAGNOSTIC_SOURCE = "fr.centralesupelec.edf.riseclipse";
    
    private static final String DEFAULT_NAMESPACE_ID = "IEC 61850-7-4";
    private static final Integer DEFAULT_NAMESPACE_VERSION = new Integer( 2007 );
    private static final String DEFAULT_NAMESPACE_REVISION = "B";
    private static final Integer DEFAULT_NAMESPACE_RELEASE = new Integer( 1 );
    
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

    private static void usage() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        console.info( "java -jar RiseClipseValidatorSCL.jar --help" );
        console.info( "java -jar RiseClipseValidatorSCL.jar [--verbose | --info | --warning | --error] [--make-explicit-links] (<oclFile> | <nsdFile> | <sclFile>)+" );
        console.info( "Files ending with \".ocl\" are considered OCL files, "
                + "files ending with \".nsd\" are considered NS files, "
                + "files ending with \".snsd\" are considered ServiceNS files, "
                + "files ending with \".AppNS\" are considered ApplicableServiceNS files, "
                + "files ending with \".nsdoc\" are considered NSDoc files, "
                + "all others are considered SCL files" );
        System.exit( -1 );
    }

    private static void help() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        displayLegal();
        console.info( "java -jar RiseClipseValidatorSCL.jar option* file*" );
        console.info( "\tFiles ending with \".ocl\" are considered OCL files," );
        console.info( "\tfiles ending with \".nsd\" are considered NS files," );
        console.info( "\tfiles ending with \".snsd\" are considered ServiceNS files," );
        console.info( "\tfiles ending with \".AppNS\" are considered ApplicableServiceNS files (at most one should be given)," );
        console.info( "\tfiles ending with \".nsdoc\" are considered NSDoc files," );
        console.info( "\tall others are considered SCL files." );
        console.info( "" );
        console.info( "The following options are recognized:" );
        console.info( "\t--verbose" );
        console.info( "\t--info" );
        console.info( "\t--warning" );
        console.info( "\t--error" );
        console.info( "\t\tThe amount of messages displayed is chosen according to this option, default is --warning." );
        console.info( "\t--use-color" );
        console.info( "\t\tcolors (using ANSI escape sequences) are used on message prefixes." );
        console.info( "\t--make-explicit-links" );
        console.info( "\t\tImplicit links in SCL files are made explicit, this is usually needed for complete validation. "
                + "Warnings are displayed when problems are detected. Infos are displayed about explicit links being made. "
                + "Verbosity is about how explicit links are made." );
        console.info( "\t--display-nsd-messages" );
        console.info( "\t\tOnly errors detected in NSD files are displayed by default. "
                + "This option allows for other messages to be displayed (according to the chosen level).");
        console.info( "\t--do-not-display-copyright" );
        console.info( "\t\tThe tool information is not displayed at the beginning." );
        System.exit( 0 );
    }
    
    public static void main( @NonNull String[] args ) {

        if( args.length == 0 ) {
            usage();
        }

        boolean makeExplicitLinks = false;
        boolean useColor = false;
        boolean displayCopyright = true;
        boolean displayNsdMessages = false;
        
        int consoleLevel = IRiseClipseConsole.WARNING_LEVEL;

        int posFiles = 0;
        for( int i = 0; i < args.length; ++i ) {
            if( args[i].startsWith( "--" ) ) {
                posFiles = i + 1;
                if( "--help".equals( args[i] ) ) {
                    help();
                }
                else if( "--verbose".equals( args[i] ) ) {
                    consoleLevel = IRiseClipseConsole.VERBOSE_LEVEL;
                }
                else if( "--info".equals( args[i] ) ) {
                    consoleLevel = IRiseClipseConsole.INFO_LEVEL;
                }
                else if( "--warning".equals( args[i] ) ) {
                    consoleLevel = IRiseClipseConsole.WARNING_LEVEL;
                }
                else if( "--error".equals( args[i] ) ) {
                    consoleLevel = IRiseClipseConsole.ERROR_LEVEL;
                }
                else if( "--make-explicit-links".equals( args[i] ) ) {
                    makeExplicitLinks = true;
                }
                else if( "--use-color".equals( args[i] ) ) {
                    useColor = true;
                }
                else if( "--do-not-display-copyright".equals( args[i] ) ) {
                    displayCopyright = false;
                }
                else if( "--display-nsd-messages".equals( args[i] ) ) {
                    displayNsdMessages = true;
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
        
        IRiseClipseConsole console = new TextRiseClipseConsole( useColor );
        AbstractRiseClipseConsole.changeConsole( console );
        console.setLevel( consoleLevel );

        if( displayCopyright ) {
            int level = console.setLevel( IRiseClipseConsole.INFO_LEVEL );
            displayLegal();
            console.setLevel( level );
        }
        
        //console.doNotDisplayIdenticalMessages();

        ArrayList< @NonNull String > oclFiles = new ArrayList<>();
        ArrayList< @NonNull String > nsdFiles = new ArrayList<>();
        ArrayList< @NonNull String > sclFiles = new ArrayList<>();
        for( int i = posFiles; i < args.length; ++i ) {
            if( args[i].endsWith( ".ocl" )) {
                oclFiles.add( args[i] );
            }
            else if( args[i].endsWith( ".nsd" )) {
                nsdFiles.add( args[i] );
            }
            else if( args[i].endsWith( ".snsd" )) {
                nsdFiles.add( args[i] );
            }
            else if( args[i].endsWith( ".AppNS" )) {
                nsdFiles.add( args[i] );
            }
            else if( args[i].endsWith( ".nsdoc" )) {
                nsdFiles.add( args[i] );
            }
            else {
                sclFiles.add( args[i] );
            }
        }
        
        if( hiddenDoor ) {
            doHiddenDoor( oclFiles, nsdFiles, sclFiles );
        }

        prepare( oclFiles, nsdFiles, displayNsdMessages );
        for( int i = 0; i < sclFiles.size(); ++i ) {
            run( makeExplicitLinks, sclFiles.get( i ));
        }
    }

    private static void doHiddenDoor( List< @NonNull String > oclFiles, List< @NonNull String > nsdFiles, List<String> sclFiles ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        prepare( oclFiles, nsdFiles, false );
        
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
                                });
                            });
                        });
                    }
                });
            });
        }
        
        System.exit( 0 );
    }

    // public because used by ui
    public static void displayLegal() {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        console.info( "Copyright (c) 2019 CentraleSupélec & EDF." );
        console.info(
                "All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0" );
        console.info(
                "which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html" );
        console.info( "" );
        console.info( "This tool is part of RiseClipse." );
        console.info( "Contributors:" );
        console.info( "    Computer Science Department, CentraleSupélec" );
        console.info( "    EDF R&D" );
        console.info( "Contacts:" );
        console.info( "    dominique.marcadet@centralesupelec.fr" );
        console.info( "    aurelie.dehouck-neveu@edf.fr" );
        console.info( "Web site:" );
        console.info( "    http://wdi.supelec.fr/software/RiseClipse/" );
        console.info( "" );
        console.info( "RiseClipseValidatorSCL version: 1.1.0 a7 (5 June 2019)" );
        console.info( "" );
    }

    // public because used by ui
    public static void prepare( List< @NonNull String > oclFiles, List< @NonNull String > nsdFiles, boolean displayNsdMessages ) {
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
            nsdValidator = new NsdValidator( sclPg );
            for( int i = 0; i < nsdFiles.size(); ++i ) {
                nsdValidator.addNsdDocument( nsdFiles.get( i ), console );
            }
            nsdValidator.prepare( validator, console, displayNsdMessages );
        }

        sclLoader = new SclModelLoader();
        sclAdapter = new SclItemProviderAdapterFactory();

    }

    // public because used by ui
    public static void run( boolean make_explicit_links, @NonNull String sclFile ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        sclLoader.reset();
        Resource resource = sclLoader.loadWithoutValidation( sclFile );
        if( make_explicit_links ) {
            console.info( "Making explicit links for file: " + sclFile );
            sclLoader.finalizeLoad( console );
        }
        if( resource != null ) {
            console.info( "Validating file: " + sclFile );
            validate( resource, sclAdapter );
        }
    }

    private static void validate( @NonNull Resource resource, final AdapterFactory adapter ) {
        IRiseClipseConsole console = AbstractRiseClipseConsole.getConsole();
        
        Map< Object, Object > context = new HashMap< Object, Object >();
        SubstitutionLabelProvider substitutionLabelProvider = new EValidator.SubstitutionLabelProvider() {

            @Override
            public String getValueLabel( EDataType eDataType, Object value ) {
                return Diagnostician.INSTANCE.getValueLabel( eDataType, value );
            }

            @Override
            public String getObjectLabel( EObject eObject ) {
                IItemLabelProvider labelProvider = ( IItemLabelProvider ) adapter.adapt( eObject,
                        IItemLabelProvider.class );
                return labelProvider.getText( eObject );
            }

            @Override
            public String getFeatureLabel( EStructuralFeature eStructuralFeature ) {
                return Diagnostician.INSTANCE.getFeatureLabel( eStructuralFeature );
            }
        };
        context.put( EValidator.SubstitutionLabelProvider.class, substitutionLabelProvider );

        for( int n = 0; n < resource.getContents().size(); ++n ) {
            Diagnostic diagnostic = Diagnostician.INSTANCE.validate( resource.getContents().get( n ), context );

            for( Iterator< Diagnostic > i = diagnostic.getChildren().iterator(); i.hasNext(); ) {
                Diagnostic childDiagnostic = i.next();
                
                List< ? > data = childDiagnostic.getData();
                EObject object = ( EObject ) data.get( 0 );
                String message = childDiagnostic.getMessage();
                if(( data.size() > 1 ) && ( data.get( 1 ) instanceof EAttribute )) {
                    EAttribute attribute = ( EAttribute ) data.get( 1 );
                    if( attribute == null ) continue;
                    message = "\tAttribute " + attribute.getName() + " of "
                                + substitutionLabelProvider.getObjectLabel( object ) + " : "
                                + childDiagnostic.getChildren().get( 0 ).getMessage();
                }

                switch( childDiagnostic.getSeverity() ) {
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
