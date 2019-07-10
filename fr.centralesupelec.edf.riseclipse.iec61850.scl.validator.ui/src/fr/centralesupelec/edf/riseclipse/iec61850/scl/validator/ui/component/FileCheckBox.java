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
package fr.centralesupelec.edf.riseclipse.iec61850.scl.validator.ui.component;

import java.io.File;

import javax.swing.JCheckBox;

public class FileCheckBox {

    private File file;
    private JCheckBox checkBox;

    public FileCheckBox( File file ) {
        this.file = file;
        this.checkBox = new JCheckBox( file.getName() );
        this.checkBox.setSelected( true );
    }

    public File getFile() {
        return file;
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }
}
