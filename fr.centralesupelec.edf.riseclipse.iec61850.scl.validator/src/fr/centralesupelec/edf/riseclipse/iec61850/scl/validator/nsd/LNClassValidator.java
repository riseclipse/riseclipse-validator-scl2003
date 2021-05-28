/*
*************************************************************************
**  Copyright (c) 2019-2021 CentraleSupélec & EDF.
**  All rights reserved. This program and the accompanying materials
**  are made available under the terms of the Eclipse Public License v2.0
**  which accompanies this distribution, and is available at
**  https://www.eclipse.org/legal/epl-v20.html
** 
**  This file is part of the RiseClipse tool
**  
**  Contributors:
**      Computer Science Department, CentraleSupélec
**      EDF R&D
**  Contacts:
**      dominique.marcadet@centralesupelec.fr
**      aurelie.dehouck-neveu@edf.fr
**  Web site:
**      https://riseclipse.github.io/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.nsd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.DiagnosticChain;

import fr.centralesupelec.edf.riseclipse.iec61850.nsd.AnyLNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.DataObject;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.LNClass;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentification;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsIdentificationName;
import fr.centralesupelec.edf.riseclipse.iec61850.nsd.util.NsdResourceSetImpl;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.DO;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.LNodeType;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

public class LNClassValidator {
    
    private static HashMap< NsIdentificationName, LNClassValidator > validators = new HashMap<>();
    
    public static LNClassValidator get( NsIdentification nsIdentification, String lnClassName ) {
        if( validators == null ) return null;
        if( nsIdentification == null ) return null;
        if( lnClassName == null ) return null;
        return validators.get( new NsIdentificationName( nsIdentification, lnClassName ));
    }
    
    public static void buildValidators( NsIdentification nsIdentification, Stream< LNClass > stream, IRiseClipseConsole console ) {
        stream
        .forEach( lnClass -> validators.put(
                new NsIdentificationName( nsIdentification, lnClass.getName() ),
                new LNClassValidator( nsIdentification, lnClass, console )));
    }

    /*
     * Called before another file is validated
     */
    public static void resetValidators() {
        validators.values().stream().forEach( v -> v.reset() );
    }

    private void reset() {
        validatedLNodeType = new HashSet<>();
        
        dataObjectValidatorMap.values().stream().forEach( v -> v.reset() );
    }

    private NsIdentification nsIdentification;
    private HashSet< String > validatedLNodeType;

    // An LNClass defines a set of DataObject, each has a name, a type (name of a CDC) and a presence condition
    // An LNClass is referenced by an LNodeType (lnClass attribute) 
    // An LNodeType defines a set of DO, each has a name, a type (id of a DOType)
    // The association between DataObject and DO is done using their name attribute
    // A DOType has also a cdc attribute which is the name of a CDC
    
    // To validate an LNodeType:
    // - find its LNClass (LNodeType.lnClass == LNClass.name) in the namespace of the LNodeType
    //   - if the LNodeType has no namespace, use the namespaces (there may be several) of AnyLN referencing the LNodeType
    // - check whether all the DO of the LNType respect the presence conditions of the corresponding DataObject (DO.name == DataObject.name)
    //   - this cannot be done if the namespace of the DO is not the same as the namespace of the LNClass
    // - for each DO, verify that its DOType.cdc is the same as DataObject.cdc
    // - validate its DOType using the CDC
    
    // This will check the presence condition
    private DataObjectPresenceConditionValidator dataObjectPresenceConditionValidator;
    // Key is DataObject name (the corresponding DO has the same name)
    // Value is the CDCValidator given by the DataObject type
    private HashMap< String, CDCValidator > dataObjectValidatorMap = new HashMap<>();

    private LNClassValidator( NsIdentification nsIdentification, AnyLNClass anyLNClass, IRiseClipseConsole console ) {
        console.verbose( "[NSD setup] (" + anyLNClass.getFilename() + ":" + anyLNClass.getLineNumber() + ") build LNClassValidator for "
                + anyLNClass.getName() + " in namespace \"" + nsIdentification + "\"" );
        
        this.nsIdentification = nsIdentification;
        dataObjectPresenceConditionValidator = DataObjectPresenceConditionValidator.get( nsIdentification, anyLNClass );
        
        AnyLNClass lnClass = anyLNClass;
        while( lnClass != null ) {
            for( DataObject do_ : lnClass.getDataObject() ) { 
                // When we look for a validator, we first search in the current namespace.
                // If this fails, we use DependsOn links
                CDCValidator cdcValidator = null;
                // Reinitialize before searching
                nsIdentification = this.nsIdentification;
                while( nsIdentification != null ) {
                    cdcValidator = CDCValidator.get( nsIdentification, do_.getType() );
                    if( cdcValidator != null ) break;
                    // TODO: NsdObjectImpl.getResourceSet() should be available in interface
                    if(( do_.eResource() != null ) && ( do_.eResource().getResourceSet() instanceof NsdResourceSetImpl )) {
                        nsIdentification = (( NsdResourceSetImpl ) do_.eResource().getResourceSet() ).getDependsOn( nsIdentification );
                    }
                    else {
                        break;
                    }
                }
                if( cdcValidator != null ) {
                    dataObjectValidatorMap.put( do_.getName(), cdcValidator );
                    console.verbose( "[NSD setup] (" + do_.getFilename() + ":" + do_.getLineNumber()
                        + ") CDC validator for DataObject " + do_.getName() + " found with type " + do_.getType() );
                }
                else {
                    console.warning( "[NSD setup] (" + do_.getFilename() + ":" + do_.getLineNumber()
                        + ") CDC validator with type " + do_.getType() + " not found for DataObject " + do_.getName()
                        + " in namespace \"" + this.nsIdentification + "\"" );
                }
            }

            lnClass = lnClass.getRefersToAbstractLNClass();
        }
        
        reset();
    }
    
    public boolean validateLNodeType( LNodeType lNodeType, DiagnosticChain diagnostics ) {
        if( validatedLNodeType.contains( lNodeType.getId() )) return true;
        AbstractRiseClipseConsole.getConsole().verbose( "[NSD validation] LNClassValidator.validateLNodeType( " + lNodeType.getId()
                + " ) at line " + lNodeType.getLineNumber() + " in namespace \"" + nsIdentification + "\"" );
        validatedLNodeType.add( lNodeType.getId() );

        boolean res = true;

        // Each DO of an LNodeType must satisfy the presence condition of the corresponding DataObject (same name)
        // Do with another namespace are not concerned by this rule
        dataObjectPresenceConditionValidator.reset();
        lNodeType
        .getDO()
        .stream()
        .forEach( do_ -> {
            if(( do_.getNamespace() == null ) || nsIdentification.equals( new NsIdentification( do_.getNamespace() ))) {
                dataObjectPresenceConditionValidator.addDO( do_, diagnostics );
            }
            else {
                AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] Presence condition of DO " + do_.getName()
                    + " at line " + do_.getLineNumber() + " is not checked because its namespace \"" + do_.getNamespace()
                    + "\" is not the same as the namespace of its LNodeType \"" + nsIdentification + "\"" );
            }
        });
      
        res = dataObjectPresenceConditionValidator.validate( lNodeType, diagnostics ) && res;
        
        // The type of each DO must conform to the CDC of the corresponding DataObject
        for( DO do_ : lNodeType.getDO() ) {
            String[] names;
            if( do_.getName().matches( "[a-zA-Z]+\\d+" )) {
                names = do_.getName().split( "(?=\\d)", 2 );
            }
            else {
                names = new String[] { do_.getName() };
            }
            if( names.length == 0 ) {
                // error should have been already displayed
                //AbstractRiseClipseConsole.getConsole().error( "[NSD validation] Unexpected DO name " + do_.getName() + " in LNodeType (line " + do_.getParentLNodeType().getLineNumber() );
                continue;
            }
            if(( do_.getNamespace() == null ) || nsIdentification.equals( new NsIdentification( do_.getNamespace() ))) {
                CDCValidator cdcValidator = dataObjectValidatorMap.get( names[0] );
                if( cdcValidator != null ) {
                    if(( do_.getRefersToDOType() != null ) && ! cdcValidator.getName().equals( do_.getRefersToDOType().getCdc() )) {
                        AbstractRiseClipseConsole.getConsole().error( "[NSD validation] DOType " + " at line " + do_.getRefersToDOType().getLineNumber()
                                + " used by DO " + do_.getName() + " at line " + do_.getLineNumber() + " has wrong CDC " + do_.getRefersToDOType().getCdc()
                                + ", it should be " + cdcValidator.getName() + " in namespace \"" + nsIdentification + "\"" );
                    }
                    res = cdcValidator.validateDO( do_, diagnostics ) && res;
                }
                else {
                    AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] DO " + do_.getName() + " at line "
                        + do_.getLineNumber() + " cannot be verified because there is no validator for it in namespace \"" + nsIdentification + "\"" );
                }
            }
            else {
                if( do_.getRefersToDOType() == null ) {
                    AbstractRiseClipseConsole.getConsole().error( "[NSD validation] DO " + do_.getName() + " at line "
                            + do_.getLineNumber() + " cannot be verified because its DOType is unknown" );
                    res = false;
                    continue;
                }

                AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] DO " + do_.getName() + " at line "
                        + do_.getLineNumber() + " cannot be checked against the CDC given by the LNClass of its AnyLN  because its namespace \""
                        + do_.getNamespace() + "\" differs from current namespace \"" + nsIdentification + "\". It will be checked using the CDC "
                        + "of its DOType " + do_.getRefersToDOType().getCdc() );
                CDCValidator cdcValidator = CDCValidator.get( new NsIdentification( do_.getNamespace() ), do_.getRefersToDOType().getCdc() );
                if( cdcValidator != null ) {
                    res = cdcValidator.validateDO( do_, diagnostics ) && res;
                }
                else {
                    AbstractRiseClipseConsole.getConsole().warning( "[NSD validation] DO " + do_.getName() + " at line "
                        + do_.getLineNumber() + " cannot be verified because there is no CDC validator for it in namespace \"" + do_.getNamespace() + "\"" );
                    
                }
            }
        }
        
        return res;
    }
}
