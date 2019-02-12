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
import java.util.Optional;

import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DocumentRoot;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.FunctionalConstraint;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataAttribute;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NS;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DAI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOI;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNode;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NsdEObjectValidator implements EValidator {
    
    private NsdResourceSetImpl nsdResourceSet;

    public NsdEObjectValidator( NsdResourceSetImpl nsdResourceSet ) {
        this.nsdResourceSet = nsdResourceSet;
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        
        switch(eClass.getName()) {
        case "LN0":
        case "LN":
        	AnyLN ln = (AnyLN) eObject;
        	return validateLN(ln);
        default:
        	return false;
        }
    }

    @Override
    public boolean validate( EDataType eDataType, Object value, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EDataType ): " + eDataType.getName() );
        
        // TODO: use nsdResource to validate value
        

        return true;
    }
    
    
    public boolean validateLN(AnyLN ln) {
    	AbstractRiseClipseConsole.getConsole().info(" ");
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validateLN( " + ln.getLnClass() + " )" );
        
        // TODO: inheritance of LNClass must be taken into account
        
        Optional< LNClass > lnClassFound = nsdResourceSet.getLNClassStream().filter( lNClass -> lNClass.getName().equals( ln.getLnClass() )).findAny();
        if( ! lnClassFound.isPresent() ) {
            AbstractRiseClipseConsole.getConsole().error( "LNClass " + ln.getLnClass() + " not found in NSD files" );
	    	return false; 
        }
        AbstractRiseClipseConsole.getConsole().info( "found LNClass " + ln.getLnClass() + " in NSD files" );
        
        // lnClassFound contains DataObject which describes allowed DOI in LN
        for( DOI doi : ln.getDOI() ) {
            Optional< DataObject > dataObjectFound = lnClassFound.get().getDataObject().stream().filter( dataObject -> dataObject.getName().equals( doi.getName()) ).findAny();

            if( ! dataObjectFound.isPresent() ) {
                // TODO: add message
                AbstractRiseClipseConsole.getConsole().error( "DO " + doi.getName() + " not found in LNClass " +  ln.getLnClass());
            	return false;
            }
            
            // dataObjectFound refers to a CDC which describes allowed DAI in DOI
            CDC cdcFound = dataObjectFound.get().getRefersToCDC();
            AbstractRiseClipseConsole.getConsole().info( "found DO " + doi.getName() + " (CDC: " + cdcFound.getName() + ") in LNClass " +  ln.getLnClass());
            for( DAI dai : doi.getDAI() ) {
                Optional< DataAttribute > dataAttributeFound = cdcFound.getDataAttribute().stream().filter( dataAttribute -> dataAttribute.getName().equals( dai.getName() ) ).findAny();
                
                if( ! dataAttributeFound.isPresent() ) {
                    // TODO: add message
                	AbstractRiseClipseConsole.getConsole().error( "DA " + dai.getName() + " not found in CDC " +  cdcFound.getName());
                	return false;
                }
                AbstractRiseClipseConsole.getConsole().info( "found DA " + dai.getName() + " in CDC " +  cdcFound.getName());
                
                // TODO: is there anything else to check ?
                
            }
            
            // TODO: check that compulsory DataAttribute in cdcFound are present in doi 
        }
        
        // TODO: check that compulsory DataObject in lnClassFound are present in ln 

        return true;
    }
    
    
    public void log(String message) {
        AbstractRiseClipseConsole.getConsole().info(message);
    }

    
}
