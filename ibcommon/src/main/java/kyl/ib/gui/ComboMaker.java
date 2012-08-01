package kyl.ib.gui;


import com.ib.client.*;
import kyl.ib.*;

import java.awt.event.*;

public class ComboMaker
{

    ContractLibrary conLib;
    ComboMakerDialog dlg = null;
    Contract curSelCont = null;

    public ComboMaker(ContractLibrary cl)
    {
        conLib = cl;
    }

    private void init()
    {
        dlg = new ComboMakerDialog();

        dlg.getSelectButton().addActionListener(selectActionListener());
    }

    private ActionListener selectActionListener()
    {
        return new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ContractChooser chooser = new ContractChooser(conLib);
                int retval = chooser.showDialog(false);
                if (retval == ContractChooser.APPROVED && chooser.getSelectionCount() > 0)
                {
                    curSelCont = chooser.getSelected();
                    dlg.getContractField().setText(conLib.getSummaryDescription(curSelCont));
                }
            }
        };
    }

    public int showDialog()
    {
        if (dlg == null)
        {
            init();
        }

        dlg.pack();
        dlg.setVisible(true);

        return 0;
    }
}
