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
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.BasicType;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.CDC;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.NS;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.AnyLN;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DA;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DOType;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNode;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;

public class NSDEObjectValidator implements EValidator {
    
    private NS ns;

    public NSDEObjectValidator( Resource nsdResource ) {
        DocumentRoot root = (DocumentRoot) nsdResource.getContents().get( 0 );
        ns = (NS) root.getNS();
    }

    @Override
    public boolean validate( EObject eObject, DiagnosticChain diagnostics, Map< Object, Object > context ) {
        return validate( eObject.eClass(), eObject, diagnostics, context );
    }

    @Override
    public boolean validate( EClass eClass, EObject eObject, DiagnosticChain diagnostics,
            Map< Object, Object > context ) {
        
        switch(eClass.getName()) {
        case "LNode":
        	LNode lnd = (LNode) eObject;
        	return validateLN(lnd.getLnClass());
        case "LNodeType":
        	LNodeType lnt = (LNodeType) eObject;
        	return validateLN(lnt.getLnClass());
        case "LN0":
        case "LN":
        	AnyLN ln = (AnyLN) eObject;
        	return validateLN(ln.getLnClass());
        case "DOType":
        	DOType dot = (DOType) eObject;
        	return validateDO(dot.getCdc());
        case "DA":
        	DA da = (DA) eObject;
        	return validateDA(da.getBType());
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
    
    
    public boolean validateLN(String lnClassName) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validateLN( " + lnClassName + " )" );
        if(ns.getLNClasses() != null) {
	        EList<LNClass> lnClass = ns.getLNClasses().getLNClass();
	    	for(int i = 0; i < lnClass.size(); i++) {
	    		if(lnClassName.equals(lnClass.get(i).getName())) {
	    			//log("is valid");
	    			return true;
	    		}
	    	}
	    	//log("is not valid");
	    	return false; 
        } else {
        	return true;
        }
    }
    
    public boolean validateDO(String cdcName) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validateDO( " + cdcName + " )" );
        if(ns.getCDCs() != null) {
	    	EList<CDC> cdc = ns.getCDCs().getCDC();
	    	for(int i = 0; i < cdc.size(); i++) {
	    		if(cdcName.equals(cdc.get(i).getName())) {
	    			//log("is valid");
	    			return true;
	    		}
	    	}
	    	//log("is not valid");
	    	return false;
        } else {
        	return true;
        }
    }

    public boolean validateDA(String basicTypeName) {
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validateDA( " + basicTypeName + " )" );
        if(ns.getBasicTypes() != null) {
	    	EList<BasicType> basicTypes = ns.getBasicTypes().getBasicType();
	    	for(int i = 0; i < basicTypes.size(); i++) {
	    		if(basicTypeName.equals(basicTypes.get(i).getName())) {
	    			//log("is valid");
	    			return true;
	    		}
	    	}
	    	//log("is not valid");
	    	return false;
        } else {
        	return true;
        }
    }
    
    
    public void log(String message) {
        AbstractRiseClipseConsole.getConsole().info(message);
    }

    
}
