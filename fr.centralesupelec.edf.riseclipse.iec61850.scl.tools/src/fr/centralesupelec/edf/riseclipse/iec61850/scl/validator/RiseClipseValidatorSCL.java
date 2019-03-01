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

import java.io.File;
import java.util.ArrayList;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.provider.NsdItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.validation.ocl.OCLValidator;

import org.eclipse.emf.ecore.EValidator;
//import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.ocl.pivot.validation.ComposedEValidator;

public class RiseClipseValidatorSCL {

    //private static OCLValidator oclValidator;
    private static SclItemProviderAdapterFactory sclAdapter;
    private static SCLModelLoader sclLoader;
    private static NsdValidator nsdValidator;
    //private static boolean oclValidation = false;
    private static boolean nsdValidation = false;
    private static NsdItemProviderAdapterFactory nsdAdapter;

    public static void usage( IRiseClipseConsole console ) {
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        console.info( "java -jar RiseClipseValidatorSCL.jar [--verbose] [--make-explicit-links] [<oclFile> | <nsdFile> | <sclFile>]*" );
        console.info( /*"Files ending with \".ocl\" are considered OCL files, "
                    +*/ "files ending with \\\".nsd\\\" are considered NSD files, "
                    + "all others are considered SCL files" );
        System.exit( -1 );
    }

    public static void main( String[] args ) {
        
        final IRiseClipseConsole console = new TextRiseClipseConsole();
        
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        displayLegal( console );
        console.setLevel( IRiseClipseConsole.WARNING_LEVEL );
        
        if( args.length == 0 ) usage( console );
        
        boolean make_explicit_links = false;

        int posFiles = 0;
        for( int i = 0; i < args.length; ++i ) {
            if( args[i].startsWith( "--" )) {
                posFiles = i + 1;
                if( "--verbose".equals( args[i] )) {
                    console.setLevel( IRiseClipseConsole.INFO_LEVEL );
                }
                else if( "--make-explicit-links".equals( args[i] )) {
                    make_explicit_links = true;
                }
                else {
                    console.error( "Unrecognized option " + args[i] );
                    usage( console );
                }
            }
        }

        //ArrayList< String > oclFiles = new ArrayList<>();
        ArrayList< String > nsdFiles = new ArrayList<>();
        ArrayList< String > sclFiles = new ArrayList<>();
        for( int i = posFiles; i < args.length; ++i ) {
            /*if( args[i].endsWith( ".ocl" )) {
                oclFiles.add( args[i] );
                oclValidation = true;
            }
            else*/ if( args[i].endsWith( ".nsd" )) {
                nsdFiles.add( args[i] );
                nsdValidation = true;
            }
            else {
                sclFiles.add( args[i] );
            }
        }
        
        prepare( console, /*oclFiles,*/ nsdFiles );
        for( int i = 0; i < sclFiles.size(); ++i ) {
            run( console, make_explicit_links, sclFiles.get( i ));
        }
    }
    
    public static void displayLegal( IRiseClipseConsole console ) {
        console.info( "Copyright (c) 2019 CentraleSupélec & EDF." );
        console.info( "All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0" );
        console.info( "which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html" );
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
        console.info( "RiseClipseValidatorSCL version: 1.0.0 (28 January 2019)" );
        console.info( "" );
    }

    public static void prepare( IRiseClipseConsole console, /*ArrayList< String > oclFiles,*/ ArrayList< String > nsdFiles ) {
        @NonNull
        ComposedEValidator validator = ComposedEValidator.install( SclPackage.eINSTANCE );
        
        /*if( oclValidation ) {
            oclValidator = new OCLValidator( validator, true );
    
            for( int i = 0; i < oclFiles.size(); ++i ) {
                oclValidator.addOCLDocument( oclFiles.get( i ), console );
            }
        }*/
        
        if( nsdValidation ) {
            nsdValidator = new NsdValidator( validator, console );
            for( int i = 0; i < nsdFiles.size(); ++i ) {
                nsdValidator.addNsdDocument( nsdFiles.get( i ), console );
            }
            //nsdAdapter = new NsdItemProviderAdapterFactory();
        }

        sclLoader = new SCLModelLoader( console );
        sclAdapter = new SclItemProviderAdapterFactory();
        
    	/*for(EValidator v: validator.getChildren()) {
    		if(v.getClass() == NsdEObjectValidator.class) {
    			NsdEObjectValidator nsdValidator = (NsdEObjectValidator) v;
    			nsdValidator.initializeValidationData();
    		}
    	}*/
    }

    public static void run( IRiseClipseConsole console, boolean make_explicit_links, String sclFile ) {
        sclLoader.reset();
        Resource resource = sclLoader.loadWithoutValidation( sclFile );
        if( make_explicit_links ) {
            console.info( "Making explicit links for file: " + sclFile );
            sclLoader.finalizeLoad();
        }
        if( resource != null ) {
            /*if( oclValidation ) {
                console.info( "Validating file: " + sclFile + " with OCL" );
                oclValidator.validate( resource, sclAdapter, console );
            }*/
            if( nsdValidation ) {
                console.info( "Validating file: " + sclFile + " with NSD" );
                nsdValidator.validate( resource, sclAdapter, console );
            }
       }
    }

}

