/*
*************************************************************************
**  Copyright (c) 2019 CentraleSupélec & EDF.
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
**      http://wdi.supelec.fr/software/RiseClipse/
*************************************************************************
*/
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.application;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component.TreeFilePane;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component.SCLFilePane;

import javax.swing.JScrollPane;
import javax.swing.JPanel;

public class RiseClipseValidatorSCLApplication {

    private JFrame frame;
    private TreeFilePane oclTree;
    private TreeFilePane nsdTree;

    /**
     * Launch the application.
     */
    public static void main( String[] args ) {
        EventQueue.invokeLater( new Runnable() {
            public void run() {
                try {
                    RiseClipseValidatorSCLApplication window = new RiseClipseValidatorSCLApplication();
                    window.frame.setVisible( true );
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        } );
    }

    /**
     * Create the application.
     */
    public RiseClipseValidatorSCLApplication() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setTitle( "RiseClipseValidatorSCLApplication" );
        frame.setBounds( 100, 100, 800, 600 );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        JTabbedPane tabbedPane = new JTabbedPane( JTabbedPane.TOP );
        frame.getContentPane().add( tabbedPane );

        JPanel sclPanel = new SCLFilePane( this );
        tabbedPane.addTab( "SCL Files", null, sclPanel, null );

        JScrollPane oclPane = new JScrollPane();
        tabbedPane.addTab( "OCL Files", null, oclPane, null );

        File oclRoot = new File( System.getProperty( "user.dir" ) + "/OCL" );
        oclTree = new TreeFilePane( oclRoot );
        oclPane.setViewportView( oclTree );

        JScrollPane nsdPane = new JScrollPane();
        tabbedPane.addTab( "NSD Files", null, nsdPane, null );

        File nsdRoot = new File( System.getProperty( "user.dir" ) + "/NSD" );
        nsdTree = new TreeFilePane( nsdRoot );
        nsdPane.setViewportView( nsdTree );

    }

    public ArrayList< File > getOclFiles() {
        ArrayList< File > oclFiles = new ArrayList<>();
        oclTree.getSelectedFiles( oclFiles );
        return oclFiles;
    }

    public ArrayList< File > getNsdFiles() {
        ArrayList< File > nsdFiles = new ArrayList<>();
        nsdTree.getSelectedFiles( nsdFiles );
        return nsdFiles;
    }

}
