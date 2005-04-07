/*
 * TransformGUI.java
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.virus.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.virus.*;

public class MIMEConfigJPanel extends MEditTableJPanel {

    public MIMEConfigJPanel(TransformContext transformContext) {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP virus scan MIME types");
        super.setDetailsTitle("MIME type description");

        // create actual table model
        MIMETableModel mimeTableModel = new MIMETableModel(transformContext);
        this.setTableModel( mimeTableModel );
        this.setAddRemoveEnabled(true);
    }
}


class MIMETableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 130; /* MIME type */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();

    MIMETableModel(TransformContext transformContext){
        super(transformContext);

        refresh();
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "undefined type", "MIME type");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold( sc.TITLE_BLOCK ));
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );

        return tableColumnModel;
    }


    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        List elemList = new ArrayList();
        VirusSettings virusSettings = ((VirusTransform) transformContext.transform()).getVirusSettings();
        MimeTypeRule newElem;

        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);

            newElem = new MimeTypeRule();

            newElem.setMimeType( new MimeType( (String)rowVector.elementAt(2) ) );
            newElem.setLive( (Boolean)rowVector.elementAt(3)  );
            newElem.setName( (String)rowVector.elementAt(4) );
            elemList.add(newElem);

        }
        virusSettings.setHttpMimeTypes( elemList );
        return virusSettings;
    }

    public Vector generateRows(Object transformSettings){
        VirusSettings virusSettings = (VirusSettings) transformSettings;
        Vector allRows = new Vector();
        Vector newRow;

        MimeTypeRule newElem;
        List newElemList = virusSettings.getHttpMimeTypes();

        Iterator newElemIterator = newElemList.iterator();
        int counter = 1;
        while( newElemIterator.hasNext() ){
            newElem = (MimeTypeRule) newElemIterator.next();
            newRow = new Vector();

            newRow.add( new Integer(counter) );
            newRow.add( super.ROW_SAVED );
            newRow.add( newElem.getMimeType().getType() );
            newRow.add( newElem.isLive() );
            newRow.add( newElem.getName() );

            allRows.add( newRow );
            counter++;
        }

        return allRows;
    }
}
