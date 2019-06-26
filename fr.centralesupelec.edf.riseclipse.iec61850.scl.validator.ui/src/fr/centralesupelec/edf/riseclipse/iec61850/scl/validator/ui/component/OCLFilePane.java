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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

@SuppressWarnings( "serial" )
public class OCLFilePane extends JTree {

    private DefaultMutableTreeNode root;

    public OCLFilePane( File fileRoot ) {
        root = new DefaultMutableTreeNode( new SclFileCheckBox( fileRoot ) );
        setModel( new DefaultTreeModel( root ) );
        setShowsRootHandles( true );

        createChildren( fileRoot, root );

        setCellRenderer( new SclFileCellRenderer() );

        addMouseListener( new MouseAdapter() {
            public void mousePressed( MouseEvent e ) {
                int selRow = getRowForLocation( e.getX(), e.getY() );

                if( selRow != -1 ) {
                    TreePath selPath = getPathForLocation( e.getX(), e.getY() );
                    DefaultMutableTreeNode node = (( DefaultMutableTreeNode ) selPath.getLastPathComponent() );
                    SclFileCheckBox checkbox = ( SclFileCheckBox ) node.getUserObject();
                    propagateInTree( node, ! checkbox.getCheckBox().isSelected() );
                    repaint();
                }
            }
            
            private void propagateInTree( DefaultMutableTreeNode node, boolean selected ) {
                SclFileCheckBox checkbox = ( SclFileCheckBox ) node.getUserObject();
                checkbox.getCheckBox().setSelected( selected );
                for( int i = 0; i < node.getChildCount(); ++i ) {
                    propagateInTree( ( DefaultMutableTreeNode ) node.getChildAt( i ), selected );
                }
            }
        } );

    }

    private void createChildren( File fileRoot, DefaultMutableTreeNode node ) {
        File[] files = fileRoot.listFiles();
        if( files == null ) return;

        for( File file : files ) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( new SclFileCheckBox( file ) );
            node.add( childNode );
            if( file.isDirectory() ) {
                createChildren( file, childNode );
            }
        }
    }

    protected class SclFileCellRenderer implements TreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus ) {
            DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) value;
            SclFileCheckBox file = ( SclFileCheckBox ) node.getUserObject();
            JCheckBox checkbox = file.getCheckBox();
            checkbox.setEnabled( isEnabled() );
            checkbox.setFont( getFont() );
            checkbox.setFocusPainted( false );
            checkbox.setBorderPainted( true );
            return checkbox;
        }
    }

    public void getOclFiles( ArrayList< File > oclFiles ) {
        getOclFiles( root, oclFiles );
    }

    private void getOclFiles( DefaultMutableTreeNode node, ArrayList< File > oclFiles ) {
        SclFileCheckBox checkbox = ( SclFileCheckBox ) node.getUserObject();
        if( checkbox.getFile().isFile() ) {
            if( checkbox.getCheckBox().isSelected() ) {
                oclFiles.add( checkbox.getFile() );
            }
        }
        else {
            for( int i = 0; i < node.getChildCount(); ++i ) {
                getOclFiles( ( DefaultMutableTreeNode ) node.getChildAt( i ), oclFiles );
            }
        }
    }
}
