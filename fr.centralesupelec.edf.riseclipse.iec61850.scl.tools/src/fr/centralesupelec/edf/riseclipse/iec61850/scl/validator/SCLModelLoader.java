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

import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.SclPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.util.SclResourceFactoryImpl;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseModelLoader;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;


public class SCLModelLoader extends RiseClipseModelLoader {
    
    public SCLModelLoader( IRiseClipseConsole console ) {
        super( console );
    }

    @Override
    public void reset() {
        super.reset();

        // Register the appropriate resource factory to handle all file
        // extensions.
        getResourceSet().getResourceFactoryRegistry().getExtensionToFactoryMap()
            .put( Resource.Factory.Registry.DEFAULT_EXTENSION, new SclResourceFactoryImpl() );

        // Register the package to ensure it is available during loading.
        getResourceSet().getPackageRegistry().put( SclPackage.eNS_URI, SclPackage.eINSTANCE );
    }
    
    public Resource loadWithoutValidation( String name ) {
        Object eValidator = EValidator.Registry.INSTANCE.remove( SclPackage.eINSTANCE );

        Resource resource = load( name );
        
        if( eValidator != null ) {
            EValidator.Registry.INSTANCE.put( SclPackage.eINSTANCE, eValidator );
        }
        return resource;
    }
    
    public static void main( String[] args ) {
        IRiseClipseConsole console = new TextRiseClipseConsole();
        SCLModelLoader loader = new SCLModelLoader( console );
        
        for( int i = 0; i < args.length; ++i ) {
            @SuppressWarnings( "unused" )
            Resource resource = loader.load( args[i] );
        }
    }

}
