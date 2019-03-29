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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EValidator.SubstitutionLabelProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
//import org.eclipse.ocl.pivot.delegate.OCLDelegateDomain;
import org.eclipse.emf.edit.provider.IItemLabelProvider;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NsdPackage;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.provider.NsdItemProviderAdapterFactory;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceFactoryImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.RiseClipseModelLoader;
import fr.centralesupelec.edf.riseclipse.util.TextRiseClipseConsole;

public class NsdModelLoader extends RiseClipseModelLoader {

    public NsdModelLoader( IRiseClipseConsole console ) {
        super( console );
    }

    @Override
    public void reset() {
        super.reset( new NsdResourceSetImpl( true, console ) );

        // Register the appropriate resource factory to handle all file
        // extensions.
        getResourceSet().getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put( Resource.Factory.Registry.DEFAULT_EXTENSION, new NsdResourceFactoryImpl() );

        // Register the package to ensure it is available during loading.
        getResourceSet().getPackageRegistry().put( NsdPackage.eNS_URI, NsdPackage.eINSTANCE );
    }

    @Override
    public NsdResourceSetImpl getResourceSet() {
        return ( NsdResourceSetImpl ) super.getResourceSet();
    }

    public Resource loadWithoutValidation( String name ) {
        Object eValidator = EValidator.Registry.INSTANCE.remove( NsdPackage.eINSTANCE );

        Resource resource = load( name );

        if( eValidator != null ) {
            EValidator.Registry.INSTANCE.put( NsdPackage.eINSTANCE, eValidator );
        }
        return resource;
    }

    public static void main( String[] args ) {
        IRiseClipseConsole console = new TextRiseClipseConsole();
        //console.setLevel( IRiseClipseConsole.ERROR_LEVEL );
        NsdModelLoader loader = new NsdModelLoader( console );

        org.eclipse.ocl.xtext.oclinecore.OCLinEcoreStandaloneSetup.doSetup();

        Map< Object, Object > context = new HashMap< Object, Object >();
        SubstitutionLabelProvider substitutionLabelProvider = new EValidator.SubstitutionLabelProvider() {

            @Override
            public String getValueLabel( EDataType eDataType, Object value ) {
                return Diagnostician.INSTANCE.getValueLabel( eDataType, value );
            }

            @Override
            public String getObjectLabel( EObject eObject ) {
                NsdItemProviderAdapterFactory adapter = new NsdItemProviderAdapterFactory();
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

        for( int i = 0; i < args.length; ++i ) {
            Resource resource = loader.load( args[i] );
            if( resource == null ) continue;
            if( resource.getContents().size() == 0 ) continue;
            Diagnostic diagnostic = Diagnostician.INSTANCE.validate( resource.getContents().get( 0 ), context );

            if( diagnostic.getSeverity() == Diagnostic.ERROR || diagnostic.getSeverity() == Diagnostic.WARNING ) {
                for( Iterator< Diagnostic > d = diagnostic.getChildren().iterator(); d.hasNext(); ) {
                    Diagnostic childDiagnostic = d.next();
                    switch( childDiagnostic.getSeverity() ) {
                    case Diagnostic.ERROR:
                    case Diagnostic.WARNING:
                        console.error( "\t" + childDiagnostic.getMessage() );
                    }
                }
            }
        }
        loader.getResourceSet().finalizeLoad( console );
    }

}
