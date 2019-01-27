/**
 *  Copyright (c) 2018 CentraleSupélec & EDF.
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

import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.provider.SclItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.validation.ocl.OCLValidator;

//import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

public class RiseClipseValidatorSCL {

    private static OCLValidator ocl;
    private static SclItemProviderAdapterFactory adapter;
    private static SCLModelLoader loader;

    public static void usage( IRiseClipseConsole console ) {
        console.setLevel( IRiseClipseConsole.INFO_LEVEL );
        console.info( "java -jar RiseClipseValidatorSCL.jar [--verbose] [--make-explicit-links] [<oclFile> | <sclFile>]*" );
        console.info( "Files ending with \".ocl\" are considered OCL files, all others are considered SCL files" );
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

        ArrayList< File > oclFiles = new ArrayList<>();
        ArrayList< String > sclFiles = new ArrayList<>();
        for( int i = posFiles; i < args.length; ++i ) {
            if( args[i].endsWith( ".ocl" )) {
                oclFiles.add( new File( args[i] ));
            }
            else {
                sclFiles.add( args[i] );
            }
        }
        
        prepare( console, oclFiles );
        for( int i = 0; i < sclFiles.size(); ++i ) {
            run( console, make_explicit_links, sclFiles.get( i ));
        }
    }
    
    public static void displayLegal( IRiseClipseConsole console ) {
        console.info( "Copyright (c) 2018 CentraleSupélec & EDF." );
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
        console.info( "RiseClipseSCLValidator version: 1.0.1 (6 June 2018)" );
        console.info( "" );
    }

    public static void prepare( IRiseClipseConsole console, ArrayList< File > oclFiles ) {
        SclPackage sclPg = SclPackage.eINSTANCE;
        ocl = new OCLValidator( sclPg, true );

        for( int i = 0; i < oclFiles.size(); ++i ) {
            console.info( "Loading ocl: " + oclFiles.get( i ));
            // workaround for bug 486872
//          File file = new File( oclFiles.get( i ));
//          URI uri = file.isFile() ? URI.createFileURI( file.getAbsolutePath() ) : URI.createURI( oclFiles.get( i ));
//          oclFiles.add( uri );
//          ocl.addOCLDocument( uri, console );
            ocl.addOCLDocument( oclFiles.get( i ), console );
        }

        loader = new SCLModelLoader( console );
        adapter = new SclItemProviderAdapterFactory();
    }

    public static void run( IRiseClipseConsole console, boolean make_explicit_links, String sclFile ) {
        loader.reset();
        Resource resource = loader.loadWithoutValidation( sclFile );
        if( make_explicit_links ) {
            console.info( "Making explicit links for file: " + sclFile );
            loader.finalizeLoad();
        }
        if( resource != null ) {
            console.info( "Validating file: " + sclFile );
            ocl.validate( resource, adapter, console );
        }
    }

}

