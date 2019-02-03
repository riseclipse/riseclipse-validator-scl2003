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

import java.util.Map;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DocumentRoot;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TBasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TBasicTypes;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TCDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TDataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TDataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TFunctionalConstraint;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TFunctionalConstraints;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TLNClasses;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.TNS;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.impl.DAIImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.impl.DAImpl;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NSDEObjectValidator implements EValidator {
    
    private Resource nsdResource;

    public NSDEObjectValidator( Resource nsdResource ) {
        this.nsdResource = nsdResource;
        
        // Cet attribut contient le fichier NSD qui a été chargé.
        // On peut par exemple faire:
        DocumentRoot root = (DocumentRoot) nsdResource.getContents().get( 0 );
        TNS tns = (TNS) root.getNS();
        AbstractRiseClipseConsole.getConsole().info( "    NS.id: " + tns.getId() );
        TFunctionalConstraints fcs = tns.getFunctionalConstraints();
        // La suite ne marche que pour IEC_61850-7-2_2007B.nsd
        if( fcs != null ) {
            TFunctionalConstraint fc0 = fcs.getFunctionalConstraint().get( 0 );
            AbstractRiseClipseConsole.getConsole().info( "    FunctionalConstraint.titleID: " + fc0.getTitleID() );
        }
   
        
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EClass ): " + eClass.getName());
        
        // TODO: use nsdResource to validate eObject

        DocumentRoot root = (DocumentRoot) nsdResource.getContents().get( 0 );
        TNS tns = (TNS) root.getNS();
        
        if(eClass.getName().equals("DA")) {
        	log("\nClass " + eClass.getName());
        	EList<TBasicType> tda = tns.getBasicTypes().getBasicType();
        	DAImpl da = (DAImpl) eObject;
        	log("DA " + da.getBType());
        	for(int i = 0; i < tda.size(); i++) {
        		if(da.getBType().equals(tda.get(i).getName())) {
        			log("tda " + tda.get(i).getName());
        			log("true");
        			return true;
        		}
        	}
        }
        
        return true;
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EDataType ): " + eDataType.getName() );
        
        // TODO: use nsdResource to validate value
        

        return true;
    }
    
    public void log(String message) {
        AbstractRiseClipseConsole.getConsole().info(message);
    }

}
