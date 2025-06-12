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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FileDialog;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.RiseClipseValidatorSCL;
import fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.application.RiseClipseValidatorSCLApplication;
import fr.centralesupelec.edf.riseclipse.util.AbstractRiseClipseConsole;
import fr.centralesupelec.edf.riseclipse.util.IRiseClipseConsole;

@SuppressWarnings( "serial" )
public class SCLFilePane extends JPanel implements ActionListener {

    private JButton btnAddSclFile;
    private JButton btnValidate;
    private SclFileList sclFilesList;
    private RiseClipseValidatorSCLApplication application;

    public SCLFilePane( RiseClipseValidatorSCLApplication application ) {
        this.application = application;
        
        setLayout( new BorderLayout( 0, 0 ));

        JPanel btnPanel = new JPanel();
        add( btnPanel, BorderLayout.SOUTH );

        btnAddSclFile = new JButton( "Add SCL file" );
        btnAddSclFile.addActionListener( this );
        btnPanel.add( btnAddSclFile );

        btnValidate = new JButton( "Validate" );
        btnValidate.setEnabled( false );
        btnValidate.addActionListener( this );
        btnPanel.add( btnValidate );

        JScrollPane sclFilesPane = new JScrollPane();
        add( sclFilesPane, BorderLayout.CENTER );
        
        sclFilesList = new SclFileList();
        sclFilesPane.setViewportView( sclFilesList );
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        Object source = e.getSource();

        if( source == btnAddSclFile ) {
/*
             if( fileChooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                sclFilesList.add( fileChooser.getSelectedFile() );
            }
*/

            JFrame frame = (JFrame) SwingUtilities.getRoot(( Component ) source );
            FileDialog fileDialog = new FileDialog( frame, "SCL - Choose a file" );
            fileDialog.setMode( FileDialog.LOAD );
            fileDialog.setVisible( true );
            if( fileDialog.getFiles().length != 0 ) {
                sclFilesList.add( fileDialog.getFiles()[0] );
                btnAddSclFile.setEnabled( false );
                btnValidate.setEnabled( true );
            }
            return;
        }
        if( source == btnValidate ) {
            ArrayList< File > oclFiles = application.getOclFiles();
            List< String > oclFileNames =
                    oclFiles
                    .stream()
                    .map( f -> f.getAbsolutePath() )
                    .collect( Collectors.toList() );
            
            ArrayList< File > nsdFiles = application.getNsdFiles();
            List< String > nsdFileNames =
                    nsdFiles
                    .stream()
                    .map( f -> f.getAbsolutePath() )
                    .collect( Collectors.toList() );
            
            ArrayList< String > sclFiles = sclFilesList.getSclFiles();

            ResultFrame result = new ResultFrame();
            
            final IRiseClipseConsole console = result.getMainConsole();
            AbstractRiseClipseConsole.changeConsole( console );
            RiseClipseValidatorSCL.displayLegal( );

            SwingWorker< Void, Void > prepareWorker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() throws Exception {
                    RiseClipseValidatorSCL.prepare( oclFileNames, nsdFileNames, false );
                    return null;
                }
                
                @Override
                protected void done() {
                    for( int i = 0; i < sclFiles.size(); ++i ) {
                        String sclFilename = sclFiles.get( i );
                        String shortName = sclFilename.substring( sclFiles.get( i ).lastIndexOf( '/' ) + 1 );
                        IRiseClipseConsole file_console = result.getConsoleFor( shortName);
                        AbstractRiseClipseConsole.changeConsole( file_console );
                        ActionListener progress = new ActionListener() {
                            private int count = 0;
                            public void actionPerformed( ActionEvent evt ) {
                                result.getMainConsole().info( RiseClipseValidatorSCL.VALIDATOR_SCL_CATEGORY, 0, "Validation running for ", shortName, " [", ++count, "] ..." );
                                result.repaint();
                            }
                        };
                        SwingWorker< Void, Void > worker = new SwingWorker<>() {
                            private Timer timer;
                            
                            @Override
                            protected Void doInBackground() throws Exception {
                                timer = new Timer( 1000, progress );  // milliseconds
                                timer.start();
                                
                                RiseClipseValidatorSCL.run( true, sclFilename);
                                return null;
                            }
                            
                            @Override
                            protected void done() {
                                result.getMainConsole().info( RiseClipseValidatorSCL.VALIDATOR_SCL_CATEGORY, 0, "Validation done for ", shortName );
                                result.repaint();
                                timer.stop();
                            }
                            
                        };
                        worker.execute();
                    }
                };

            };
            
            prepareWorker.execute();
        }
    }
}
