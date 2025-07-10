/*
*************************************************************************
**  Copyright (c) 2019-2025 CentraleSupélec & EDF.
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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

@SuppressWarnings( "serial" )
public class ResultFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private ResultPane mainConsole = new ResultPane( null, false );

    public ResultFrame() {
        setTitle( "RiseClipseValidatorSCL results" );
        setBounds( 200, 200, 800, 600 );
        
        tabbedPane = new JTabbedPane( JTabbedPane.TOP );
        getContentPane().add( tabbedPane );
        tabbedPane.addTab( "RiseClipseValidatorSCL", null, mainConsole, null );
        
        setVisible( true );
    }
    
    public IRiseClipseConsole getMainConsole() {
        return mainConsole;
    }

    public IRiseClipseConsole getConsoleFor( String filename ) {
        ResultPane result = new ResultPane( filename, true );
        String name = filename.substring( filename.lastIndexOf( '/' ) + 1 );
        tabbedPane.addTab( name, null, result, null );
        return result;
    }
    
}
