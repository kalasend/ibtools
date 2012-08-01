package kyl.ib.gui;


import com.ib.client.*;
import kyl.ib.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.atomic.*;

public class ContractChooser
{
    public static final int CANCELED = 0;
    public static final int APPROVED = 1;

    int buttonClicked = CANCELED;
    ContractLibrary conlib = null;
    ContractChooserDialog dlg = null;
    private ContractTableModel tableModel = null;

    AtomicBoolean searchCriteriaChanged = new AtomicBoolean(false);
    Timer timer = null;
    TimerTask searchWorker = null;
    private Frame parentFrame = null;

    public ContractChooser(ContractLibrary cl)
    {
        setContractLibrary(cl);
    }

    public void setContractLibrary(ContractLibrary cl)
    {
        conlib = cl;
    }

    public int showDialog(boolean multi_select)
    {
        if (dlg == null)
        {
            initDialog();
        }
        dlg.getContractTable().setSelectionMode(multi_select ?
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
                ListSelectionModel.SINGLE_SELECTION
        );

        startSearchWorker();
        dlg.pack();
        Point p = MouseInfo.getPointerInfo().getLocation();
        p.translate(-1 * Math.min(dlg.getWidth() / 2, p.x - 5),
                -1 * Math.min(dlg.getHeight() / 2, p.y - 5));
        dlg.setLocation(p);
        dlg.setVisible(true);
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                dlg.toFront();
                dlg.repaint();
            }
        });

        return buttonClicked;
    }

    private void startSearchWorker()
    {
        if (searchWorker == null)
        {
            searchWorker = new TimerTask() {
                @Override
                public void run()
                {
                    while (searchCriteriaChanged.compareAndSet(true, false))
                    {
                        search();
                    }
                }
            };
            timer = new Timer(true);
            timer.schedule(searchWorker, 500, 1000);
        }
    }

    private void initDialog()
    {
        dlg = new ContractChooserDialog(getParentFrame());
        dlg.getContractTable().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        dlg.getContractTable().setModel(getTableModel());
        dlg.getContractTable().getColumnModel().getColumn(0).setPreferredWidth(400);
        dlg.getContractTable().addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent mouseEvent)
            {
                if (mouseEvent.getClickCount() == 2)
                {
                    buttonClicked = APPROVED;
                    dlg.setVisible(false);
                    dlg.dispose();
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        dlg.getButtonOK().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                buttonClicked = APPROVED;
            }
        });


        // text search field
        dlg.getSearchField().getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                searchCriteriaChanged.compareAndSet(false, true);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                searchCriteriaChanged.compareAndSet(false, true);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        dlg.getSearchField().requestFocus();


        // check boxes
        final JCheckBox indvBoxes[] = {
                dlg.getCASHCheckBox(),
                dlg.getSTKCheckBox(),
                dlg.getOPTCheckBox(),
                dlg.getFUTCheckBox(),
                dlg.getFOPCheckBox(),
                dlg.getINDCheckBox()
        };
        dlg.getAllCheckBox().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean allSelected = dlg.getAllCheckBox().isSelected();
                for (JCheckBox cb : indvBoxes)
                {
                    cb.setSelected(allSelected);
                }
            }
        });
        dlg.getAllCheckBox().setSelected(true);
        for (JCheckBox cb : indvBoxes)
        {
            cb.setSelected(true);
            cb.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (!(e.getSource() instanceof JCheckBox))
                        return;
                    JCheckBox src = (JCheckBox) e.getSource();
                    if (!src.isSelected())
                    {
                        dlg.getAllCheckBox().setSelected(false);
                    }
                }
            });
        }


        // dialog closed actions
        dlg.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                if (timer != null)
                {
                    timer.cancel();
                }
            }
        });
    }

    private TableModel getTableModel()
    {
        if (tableModel == null)
        {
            tableModel = new ContractTableModel(conlib);
        }
        return tableModel;
    }

    public int getSelectionCount()
    {
        return dlg.getContractTable().getSelectedRowCount();
    }

    public Contract getSelected()
    {
        if (getSelectionCount() == 1)
        {
            int selrow = dlg.getContractTable().getSelectedRow();
            return tableModel.contractByRowIndex(selrow);
        }
        else
        {
            return null;
        }
    }

    public Contract[] getAllSelected()
    {
        // TODO
        return null;
    }

    private void search()
    {
        String stext = dlg.getSearchField().getText().trim().toUpperCase();
        int i = 0;
        for (ContractDetails cd : conlib.getAllContractDetails())
        {
            Contract con = cd.m_summary;
            if ((con.m_symbol != null && con.m_symbol.equals(stext))
                    || (con.m_localSymbol != null && con.m_localSymbol.equals(stext))
                    || conlib.getSummaryDescription(con).contains(stext)
                    || cd.m_longName.toUpperCase().contains(stext))
            {
                dlg.getContractTable().getSelectionModel().setSelectionInterval(i, i);
                scrollTableTo(i);
                return;
            }
            i++;
        }
//        for (Contract con : conlib.getComboContracts())
//        {
//            if ((con.m_symbol != null && con.m_symbol.equals(stext))
//                    || (con.m_localSymbol != null && con.m_localSymbol.equals(stext))
//                    || conlib.getSummaryDescription(con).contains(stext))
//            {
//                dlg.getContractTable().getSelectionModel().setSelectionInterval(i, i);
//                scrollTableTo(i);
//                return;
//            }
//            i++;
//        }
    }

    private void scrollTableTo(int row)
    {
        JTable table = dlg.getContractTable();
        JViewport viewport = (JViewport) table.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = table.getCellRect(row, 0, true);

        // The location of the viewport relative to the table
        Point pt = viewport.getViewPosition();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0)
        rect.setLocation(rect.x-pt.x, rect.y-pt.y);

        // Scroll the area into view
        viewport.scrollRectToVisible(rect);

    }

    public Frame getParentFrame()
    {
        if (parentFrame == null)
        {
            parentFrame = new JFrame("ContractChooser");
        }
        return parentFrame;
    }

    public void setParentFrame(Frame pframe)
    {
        parentFrame = pframe;
    }
}
