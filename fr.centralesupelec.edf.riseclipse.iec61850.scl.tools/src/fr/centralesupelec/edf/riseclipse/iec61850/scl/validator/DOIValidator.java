package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

public class DOIValidator {

	private String cdc;
	private HashMap<String, DataAttribute> daMap;
	
	public DOIValidator(CDC cdc) {
		this.cdc = cdc.getName();
		this.daMap = new HashMap<>(); // link between DAI (name) and its respective DataAttribute
    	for(DataAttribute da : cdc.getDataAttribute()){
    		this.daMap.put(da.getName(), da);
    	}
	}

	
	public boolean validateDOI(DOI doi) {
        HashSet<String> checkedDA = new HashSet<>();

    	for( DAI dai : doi.getDAI() ) {
            AbstractRiseClipseConsole.getConsole().info( "validateDAI( " + dai.getName()+ " )" );
    		
            // Test if DAI is a possible DAI in this DOI
    		if(!this.daMap.containsKey(dai.getName())) {
	            AbstractRiseClipseConsole.getConsole().error( "DA " + dai.getName() + " not found in CDC " +  this.cdc);
    			return false;
    		}
    		
    		// Control of DAI presence in DOI
    		String presCond = this.daMap.get(dai.getName()).getPresCond(); 
    		this.updateCompulsory(dai.getName(), presCond, checkedDA);
    		
    		// Validation of DAI content
		    if( ! validateDAI(dai) ){
		    	return false;
		    }
		    
    	}

    	// Verify all necessary DAI were present
    	if(!this.daMap.entrySet().stream().map(x -> checkCompulsory(x.getKey(), x.getValue().getPresCond(), checkedDA)).reduce((a, b) -> a && b).get()) {
            AbstractRiseClipseConsole.getConsole().error( "DO does not contain all mandatory DA from CDC " + this.cdc);
            return false;
    	}
		return true;
	}
	
	public boolean checkCompulsory(String name, String presCond, HashSet<String> checked) {
    	switch(presCond) {
    	case "M":
    		if(!checked.contains(name)) {
    			AbstractRiseClipseConsole.getConsole().error( "DA "+ name + " is missing");
    			return false;
    		}
    	}
    	return true;
    }
    
	public boolean updateCompulsory(String name, String presCond, HashSet<String> checked) {
		switch(presCond) {
		case "M":
		case "O":
			if(checked.contains(name)) {
				AbstractRiseClipseConsole.getConsole().error("DA " + name + " cannot appear more than once");
				return false;
			} else {
				checked.add(name);
				break;
			}
		case "F":
            AbstractRiseClipseConsole.getConsole().error("DA " + name + " is forbidden");
			return false;
		}
		return true;
	}
	
	
	public boolean validateDAI(DAI dai) {

        AbstractRiseClipseConsole.getConsole().info( "found DA " + dai.getName() + " in CDC " + this.cdc);
         
        // DataAttributes that are BASIC have a BasicType which describes allowed Val of DA
        DataAttribute da = this.daMap.get(dai.getName());
        if(da.getTypeKind().getName().equals("BASIC")) {
            for(Val val : dai.getVal()) {
            	if( ! validateVal(val.getValue(), da.getType()) ) {
            		AbstractRiseClipseConsole.getConsole().error( "Val " + val.getValue() + " of DA " +  dai.getName() + 
            				" is not of type " + da.getType());;
            		return false;
            	}
            	AbstractRiseClipseConsole.getConsole().info( "Val " +  val.getValue() + " of DA " +  dai.getName() + 
        				" is of type " + da.getType());
            }                	
        }
        
        return true;
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
    
	
}
