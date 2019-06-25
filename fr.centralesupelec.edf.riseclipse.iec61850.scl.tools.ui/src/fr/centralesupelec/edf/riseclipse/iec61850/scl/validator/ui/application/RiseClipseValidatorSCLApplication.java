/**
 *  Copyright (c) 2018 CentraleSupélec & EDF.
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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.application;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component.OCLFilePane;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component.SCLFilePane;

import javax.swing.JScrollPane;
import javax.swing.JPanel;

public class RiseClipseValidatorSCLApplication {

    private JFrame frame;
    private OCLFilePane oclTree;

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

        File fileRoot = new File( System.getProperty( "user.dir" ) + "/OCL" );
        oclTree = new OCLFilePane( fileRoot );
        oclPane.setViewportView( oclTree );

    }

    public ArrayList< File > getOclFiles() {
        ArrayList< File > oclFiles = new ArrayList<>();
        oclTree.getOclFiles( oclFiles );
        return oclFiles;
    }

}
