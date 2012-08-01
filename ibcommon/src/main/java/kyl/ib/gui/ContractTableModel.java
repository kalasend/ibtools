package kyl.ib.gui;

import com.ib.client.*;
import kyl.ib.*;

import javax.swing.table.*;
import java.lang.reflect.*;


public class ContractTableModel extends AbstractTableModel
{
    private static final String DISPLAYED_FIELDS[] = {
            "Summary",
            "m_conId", "m_symbol", "m_localSymbol", "m_exchange", "m_secType",
            "m_expiry", "m_multiplier", "m_right", "m_strike", "m_currency"
    };
    ContractLibrary conlib;
    int numConts = 0;
//    int numCombos = 0;

    public ContractTableModel(ContractLibrary cl)
    {
        conlib = cl;
        init();
    }

    private void init()
    {
        numConts = conlib.getAllContractDetails().size();

    }

    @Override
    public int getRowCount()
    {
        return numConts;
    }

    @Override
    public int getColumnCount()
    {
        return DISPLAYED_FIELDS.length;
    }

    @Override
    public String getColumnName(int columnIndex)
    {
        return DISPLAYED_FIELDS[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        try
        {
            Field f = null;
            String fname = DISPLAYED_FIELDS[columnIndex];
            if (fname.startsWith("m_"))
            {
                f = Contract.class.getField(fname);
                return f.get(conlib.getAllContractDetails().get(rowIndex).m_summary);
            }
            else if (fname.equals("Summary"))
            {
                return conlib.getSummaryDescription(
                        conlib.getAllContractDetails().get(rowIndex).m_summary);
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public Contract contractByRowIndex(int i)
    {
        return conlib.getAllContractDetails().get(i).m_summary;

    }
}

