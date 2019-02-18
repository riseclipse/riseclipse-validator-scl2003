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
import java.util.HashSet;
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
import fr.centralesupelec.edf.riseclipse.iec61850.scl.Val;
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
        //AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validate( EDataType ): " + eDataType.getName() );
        
        // TODO: use nsdResource to validate value
        

        return true;
    }
    
    
    public boolean validateLN(AnyLN ln) {
    	AbstractRiseClipseConsole.getConsole().info("");
    	AbstractRiseClipseConsole.getConsole().info("");
        AbstractRiseClipseConsole.getConsole().info( "NSDEObjectValidator.validateLN( " + ln.getLnClass() + " )" );
        
        // TODO: inheritance of LNClass must be taken into account
        
        Optional< LNClass > lnClassFound = nsdResourceSet.getLNClassStream().filter( lNClass -> lNClass.getName().equals( ln.getLnClass() )).findAny();
        if( ! lnClassFound.isPresent() ) {
            AbstractRiseClipseConsole.getConsole().error( "LNClass " + ln.getLnClass() + " not found in NSD files" );
	    	return false; 
        }
        AbstractRiseClipseConsole.getConsole().info( "found LNClass " + ln.getLnClass() + " in NSD files" );
        
        // lnClassFound contains DataObject which describes allowed DOI in LN
        if( ! validateDO(ln.getDOI(), lnClassFound.get()) ) {
        	return false;
        }
        
        // TODO: check that compulsory DataObject in lnClassFound are present in ln 

        return true;
    }
    
    
    public boolean validateDO(EList<DOI> lnDOI, LNClass lnClassFound) {
    	HashMap<String, HashSet<String>> presenceDO = generatePresenceDO(lnClassFound);
    	for( DOI doi : lnDOI ) {
	        Optional< DataObject > dataObjectFound = lnClassFound.getDataObject().stream().filter( dataObject -> dataObject.getName().equals( doi.getName()) ).findAny();
	        AbstractRiseClipseConsole.getConsole().info(" ");
	        AbstractRiseClipseConsole.getConsole().info( "validateDO( " + doi.getName() + " )" );
	        if( ! dataObjectFound.isPresent() ) {
	            AbstractRiseClipseConsole.getConsole().error( "DO " + doi.getName() + " not found in LNClass " +  lnClassFound.getName());
	        	return false;
	        }
	        
	        try {
	        	presenceDO = updatePresenceDO(presenceDO, dataObjectFound.get());
	        } catch(Exception e) {
	        	AbstractRiseClipseConsole.getConsole().error( "LN contains forbidden DO for class " + lnClassFound.getName());
	        	return false;
	        }
	        
	        // dataObjectFound refers to a CDC which describes allowed DAI in DOI
	        CDC cdcFound = dataObjectFound.get().getRefersToCDC();
	        AbstractRiseClipseConsole.getConsole().info( "found DO " + doi.getName() + " (CDC: " + cdcFound.getName() + ") in LNClass " +  lnClassFound.getName());
	        if( ! validateDA(doi.getDAI(), cdcFound) ) {
	        	return false;
	        }
	        
	        // TODO: check that compulsory DataAttribute in cdcFound are present in doi
    	}
    	
    	if(presenceDO.get("mandatory").size() > 0) {
            AbstractRiseClipseConsole.getConsole().error( "LN does not contain all mandatory DO from class " + lnClassFound.getName());
    		return false;
    	}
    	
    	return true;
    }
    
    public HashMap <String, HashSet<String>> generatePresenceDO(LNClass lnClass) {
    	HashMap <String, HashSet<String>> sets = new HashMap<>();
    	HashSet<String> mandatory = new HashSet<>();
    	HashSet<String> forbidden = new HashSet<>();
    	for(DataObject dObj : lnClass.getDataObject()) {
        	switch(dObj.getPresCond()) {
        	case "M":
        	case "AtLeastOne":
    			mandatory.add(dObj.getName());
    			break;
        	case "F":
    			forbidden.add(dObj.getName());
        		break;
    		default:
    			break;
        	}
    	}
    	sets.put("mandatory", mandatory);
    	sets.put("forbidden", forbidden);
		return sets;
    }
    
    public HashMap <String, HashSet<String>> updatePresenceDO(HashMap <String, HashSet<String>> sets, DataObject dObj) throws Exception {
    	HashSet<String> mandatory = sets.get("mandatory");
    	HashSet<String> forbidden = sets.get("forbidden");
    	switch(dObj.getPresCond()) {
    	case "M":
    	case "AtLeastOne":
			mandatory.remove(dObj.getName());
			break;
    	case "AtMostOne":
    		if(forbidden.contains(dObj.getName())) {
    			throw new Exception("Forbidden");
    		}
    		forbidden.add(dObj.getName());
    		break;
    	case "F":
    		throw new Exception("Forbidden");
		default:
			break;
    	}
    	return sets;
    }
    
    
    public boolean validateDA(EList<DAI> doiDAI, CDC cdcFound) {
    	HashMap<String, HashSet<String>> presenceDA = generatePresenceDA(cdcFound);
    	for( DAI dai : doiDAI ) {
	        AbstractRiseClipseConsole.getConsole().info(" ");
	    	AbstractRiseClipseConsole.getConsole().info( "validateDA( " + dai.getName() + " )" );
	        Optional< DataAttribute > dataAttributeFound = cdcFound.getDataAttribute().stream().filter( dataAttribute -> dataAttribute.getName().equals( dai.getName() ) ).findAny();
	        
	        if( ! dataAttributeFound.isPresent() ) {
	        	AbstractRiseClipseConsole.getConsole().error( "DA " + dai.getName() + " not found in CDC " +  cdcFound.getName());
	        	return false;
	        }
	        AbstractRiseClipseConsole.getConsole().info( "found DA " + dai.getName() + " in CDC " +  cdcFound.getName());
	        
	        try {
	        	presenceDA = updatePresenceDA(presenceDA, dataAttributeFound.get());
	        } catch(Exception e) {
	        	AbstractRiseClipseConsole.getConsole().error( "DO contains forbidden DA for class " + cdcFound.getName());
	        	return false;
	        }
	        
	        // dataAttributeFound that are BASIC have a BasicType which describes allowed Val of DA
	        if(dataAttributeFound.get().getTypeKind().getName().equals("BASIC")) {
	            for(Val val : dai.getVal()) {
	            	if( ! validateVal(val.getValue(), dataAttributeFound.get().getType()) ) {
	            		AbstractRiseClipseConsole.getConsole().error( "Val " + val.getValue() + " of DA " +  dai.getName() + 
	            				" is not of type " + dataAttributeFound.get().getType());;
	            		return false;
	            	}
	            	AbstractRiseClipseConsole.getConsole().info( "Val " +  val.getValue() + " of DA " +  dai.getName() + 
	        				" is of type " + dataAttributeFound.get().getType());
	            }                	
	        }
    	}    
    	
    	if(presenceDA.get("mandatory").size() > 0) {
            AbstractRiseClipseConsole.getConsole().error( "DO does not contain all mandatory DA from class " + cdcFound.getName());
    		return false;
    	}
    	
    	return true;
    }
    
    public HashMap <String, HashSet<String>> generatePresenceDA(CDC cdc) {
    	HashMap <String, HashSet<String>> sets = new HashMap<>();
    	HashSet<String> mandatory = new HashSet<>();
    	HashSet<String> forbidden = new HashSet<>();
    	for(DataAttribute da : cdc.getDataAttribute()) {
        	switch(da.getPresCond()) {
        	case "M":
        	case "AtLeastOne":
    			mandatory.add(da.getName());
    			break;
        	case "F":
    			forbidden.add(da.getName());
        		break;
    		default:
    			break;
        	}
    	}
    	sets.put("mandatory", mandatory);
    	sets.put("forbidden", forbidden);
		return sets;
    }
    
    public HashMap <String, HashSet<String>> updatePresenceDA(HashMap <String, HashSet<String>> sets, DataAttribute da) throws Exception {
    	HashSet<String> mandatory = sets.get("mandatory");
    	HashSet<String> forbidden = sets.get("forbidden");
    	switch(da.getPresCond()) {
    	case "M":
    	case "AtLeastOne":
			mandatory.remove(da.getName());
			break;
    	case "AtMostOne":
    		if(forbidden.contains(da.getName())) {
    			throw new Exception("Forbidden");
    		}
    		forbidden.add(da.getName());
    		break;
    	case "F":
    		throw new Exception("Forbidden");
		default:
			break;
    	}
    	return sets;
    }
    	
    	
    public boolean validateVal(String val, String type) {
    	int v;
    	long l;
    	float f;
    	switch(type) {
    	case "BOOLEAN":
    		return (val.equals("0") || val.equals("1") || val.equals("false") || val.equals("true"));
    	case "INT8":
    		v = Integer.parseInt(val);
    		return v >= -128 && v <= 127;
    	case "INT16":
    		v = Integer.parseInt(val);
    		return v >= -32768 && v <= 32767;
    	case "INT32":
    		v = Integer.parseInt(val);
    		return v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE;
    	case "INT64":
    		l = Long.parseLong(val);
    		return l >= Long.MIN_VALUE && l <= Long.MAX_VALUE;
    	case "INT8U":
    		v = Integer.parseInt(val);
    		return v >= 0 && v <= 255;
    	case "INT16U":
    		v = Integer.parseInt(val);
    		return v >= 0 && v <= 65535;
    	case "INT32U":
    		l = Long.parseLong(val);
            String max = "4294967295";
    		return l >= 0 && l <= Long.parseLong(max);
    	case "FLOAT32":
    		f = Float.parseFloat(val);
    		return f >= Float.MIN_VALUE && f <= Float.MAX_VALUE;
    	case "Octet64":
    		byte[] bytes = val.getBytes();
    		return bytes.length <= 64;
    	case "VisString64":
    		return val.length() <= 255;
    	case "VisString129":
    		return val.length() <= 129;
    	case "Unicode255":
    	case "VisString255":
    		return val.length() <= 255;
    	default:
    		return false;
    	}
    }
    

    
    public void log(String message) {
        AbstractRiseClipseConsole.getConsole().info(message);
    }

    
}
