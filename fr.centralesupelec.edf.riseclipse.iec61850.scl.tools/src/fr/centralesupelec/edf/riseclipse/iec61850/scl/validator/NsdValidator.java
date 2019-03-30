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
import java.util.List;
import java.util.Map;

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

import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class NsdValidator {

    private NsdModelLoader nsdLoader;

    public NsdValidator( @NonNull ComposedEValidator validator, IRiseClipseConsole console ) {
        nsdLoader = new NsdModelLoader( console );
        validator.addChild( new NsdEObjectValidator( nsdLoader.getResourceSet() ) );
    }

    public void addNsdDocument( String nsdFile, IRiseClipseConsole console ) {
        console.info( "Loading nsd: " + nsdFile );
        nsdLoader.load( nsdFile );
    }
    
    public void prepare( @NonNull ComposedEValidator validator, IRiseClipseConsole console ) {
        nsdLoader.getResourceSet().finalizeLoad( console );

        for( EValidator v : validator.getChildren() ) {
            if( v.getClass() == NsdEObjectValidator.class ) {
                NsdEObjectValidator nsdValidator = ( NsdEObjectValidator ) v;
                nsdValidator.initializeValidationData();
            }
        }
    }

    public void validate( Resource resource, final AdapterFactory adapter, IRiseClipseConsole console ) {
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
                //EObject root = ( EObject ) diagnostic.getData().get( 0 );
                //URI uri = root.eResource().getURI();
                //console.error( "in file " + uri.lastSegment() );
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
