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

import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.utilities.SclModelLoader;
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
import org.eclipse.ocl.pivot.validation.ComposedEValidator;

public class RiseClipseValidatorSCL {

    public static final String DIAGNOSTIC_SOURCE = "fr.centralesupelec.edf.riseclipse";
    
    private static OCLValidator oclValidator;
    private static SclItemProviderAdapterFactory sclAdapter;
    private static SclModelLoader sclLoader;
    private static NsdValidator nsdValidator;
    private static boolean oclValidation = false;
    private static boolean nsdValidation = false;

    private static final IRiseClipseConsole console = new TextRiseClipseConsole();

    private static void usage() {
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        console.info(
                "java -jar RiseClipseValidatorSCL.jar [--info | --verbose] [--make-explicit-links] [<oclFile> | <nsdFile> | <sclFile>]*" );
        console.info( "Files ending with \".ocl\" are considered OCL files, "
                + "files ending with \\\".nsd\\\" are considered NSD files, "
                + "all others are considered SCL files" );
        System.exit( -1 );
    }

    public static void main( String[] args ) {

        if( args.length == 0 ) usage();

        boolean make_explicit_links = false;
        boolean displayCopyright = true;

        int posFiles = 0;
        for( int i = 0; i < args.length; ++i ) {
            if( args[i].startsWith( "--" ) ) {
                posFiles = i + 1;
                if( "--info".equals( args[i] ) ) {
                    console.setLevel( IRiseClipseConsole.INFO_LEVEL );
                }
                else if( "--verbose".equals( args[i] ) ) {
                    console.setLevel( IRiseClipseConsole.VERBOSE_LEVEL );
                }
                else if( "--make-explicit-links".equals( args[i] ) ) {
                    make_explicit_links = true;
                }
                else if( "--do-not-display-copyright".equals( args[i] ) ) {
                    displayCopyright = false;
                }
                else {
                    console.error( "Unrecognized option " + args[i] );
                    usage();
                }
            }
        }

        if( displayCopyright ) {
            int level = console.setLevel( IRiseClipseConsole.INFO_LEVEL );
            displayLegal();
            console.setLevel( level );
        }
        
        console.doNotDisplayIdenticalMessages();

        ArrayList< String > oclFiles = new ArrayList<>();
        ArrayList< String > nsdFiles = new ArrayList<>();
        ArrayList< String > sclFiles = new ArrayList<>();
        for( int i = posFiles; i < args.length; ++i ) {
            if( args[i].endsWith( ".ocl" ) ) {
                oclFiles.add( args[i] );
                oclValidation = true;
            }
            else if( args[i].endsWith( ".nsd" ) ) {
                nsdFiles.add( args[i] );
                nsdValidation = true;
            }
            else {
                sclFiles.add( args[i] );
            }
        }

        prepare( oclFiles, nsdFiles );
        for( int i = 0; i < sclFiles.size(); ++i ) {
            run( make_explicit_links, sclFiles.get( i ));
        }
    }

    private static void displayLegal() {
        console.info( "Copyright (c) 2019 CentraleSupélec & EDF." );
        console.info(
                "All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0" );
        console.info(
                "which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html" );
        console.info( "" );
        console.info( "This file is part of the RiseClipse tool." );
        console.info( "Contributors:" );
        console.info( "    Computer Science Department, CentraleSupélec" );
        console.info( "    EDF R&D" );
        console.info( "Contacts:" );
        console.info( "    dominique.marcadet@centralesupelec.fr" );
        console.info( "    aurelie.dehouck-neveu@edf.fr" );
        console.info( "Web site:" );
        console.info( "    http://wdi.supelec.fr/software/RiseClipse/" );
        console.info( "" );
        console.info( "RiseClipseValidatorSCL version: 1.0.0 (2 april 2019)" );
        console.info( "" );
    }

    private static void prepare( ArrayList< String > oclFiles, ArrayList< String > nsdFiles ) {
        SclPackage sclPg = SclPackage.eINSTANCE;
        if( sclPg == null ) {
            throw new RiseClipseFatalException( "SCL package not found", null );
        }

        ComposedEValidator validator = ComposedEValidator.install( sclPg );

        if( oclValidation ) {
            oclValidator = new OCLValidator( sclPg, console );

            for( int i = 0; i < oclFiles.size(); ++i ) {
                oclValidator.addOCLDocument( oclFiles.get( i ), console );
            }
            oclValidator.prepare( validator, console );
        }

        if( nsdValidation ) {
            nsdValidator = new NsdValidator( sclPg, console );
            for( int i = 0; i < nsdFiles.size(); ++i ) {
                nsdValidator.addNsdDocument( nsdFiles.get( i ), console );
            }
            nsdValidator.prepare( validator, console );
        }

        sclLoader = new SclModelLoader( console );
        sclAdapter = new SclItemProviderAdapterFactory();

    }

    private static void run( boolean make_explicit_links, String sclFile ) {
        sclLoader.reset();
        Resource resource = sclLoader.loadWithoutValidation( sclFile );
        if( make_explicit_links ) {
            console.info( "Making explicit links for file: " + sclFile );
            sclLoader.finalizeLoad();
        }
        if( resource != null ) {
            console.info( "Validating file: " + sclFile );
            validate( resource, sclAdapter );
        }
    }

    private static void validate( Resource resource, final AdapterFactory adapter ) {
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

            if( diagnostic.getSeverity() == Diagnostic.ERROR || diagnostic.getSeverity() == Diagnostic.WARNING ) {
                for( Iterator< Diagnostic > i = diagnostic.getChildren().iterator(); i.hasNext(); ) {
                    Diagnostic childDiagnostic = i.next();
                    switch( childDiagnostic.getSeverity() ) {
                    case Diagnostic.ERROR:
                    case Diagnostic.WARNING:
                        List< ? > data = childDiagnostic.getData();
                        EObject object = ( EObject ) data.get( 0 );
                        if( data.size() == 1 ) {
                            console.error( "\t" + childDiagnostic.getMessage() );
                        }
                        else if( data.get( 1 ) instanceof EAttribute ) {
                            EAttribute attribute = ( EAttribute ) data.get( 1 );
                            if( attribute == null ) continue;
                            console.error( "\tAttribute " + attribute.getName() + " of "
                                    + substitutionLabelProvider.getObjectLabel( object ) + " : "
                                    + childDiagnostic.getChildren().get( 0 ).getMessage() );
                        }
                        else {
                            console.error( "\t" + childDiagnostic.getMessage() );
                        }
                    }
                }
            }
        }
    }

}
