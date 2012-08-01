package kyl.ib.gui;


import kyl.ib.IBClient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConnectionSetup
{
    public static final int APPROVED = 1;
    public static final int CANCELED = 0;

    private boolean enableTest = true;
    private boolean connected = false;
    private IBClient client;
    private ConnectionDialog conndlg;
    private int dlgCloseState;

    public ConnectionSetup(boolean enableTest)
    {
        this.enableTest = enableTest;
        client = null;
        conndlg = new ConnectionDialog();
        initDialog();
    }

    private void initDialog()
    {
        if (!enableTest)
        {
            conndlg.getTestButton().setEnabled(false);
        }
        else
        {
            conndlg.getButtonOK().setEnabled(false);
            conndlg.getTestButton().addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    conndlg.getTestButton().setEnabled(false);
                    Thread tester = new Thread(new ConnTester());
                    tester.start();
                }
            });
        }

        conndlg.getButtonOK().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dlgCloseState = APPROVED;
            }
        });

        conndlg.getButtonCancel().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dlgCloseState = CANCELED;
            }
        });
    }

    public int showSetupDialog()
    {
        conndlg.pack();
        conndlg.setVisible(true);

        return dlgCloseState;
    }

    private class ConnTester implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                conndlg.getStatusLabel().setText("");

                client = new IBClient(conndlg.getHostField().getText(),
                        Integer.parseInt(conndlg.getPortField().getText()),
                        Integer.parseInt(conndlg.getClientIdField().getText()));
                if (client.connect())
                {
                    conndlg.getStatusLabel().setText("Connected!");
                    conndlg.getButtonOK().setEnabled(true);
                    conndlg.getTestButton().setEnabled(true);
                    connected = true;
                    client.disconnect();
                }
                else
                {
                    conndlg.getStatusLabel().setText("Failed to connect");
                    conndlg.getButtonOK().setEnabled(false);
                    conndlg.getTestButton().setEnabled(true);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                conndlg.getStatusLabel().setText("Exception: " + e.getMessage());
                conndlg.getButtonOK().setEnabled(false);
                conndlg.getTestButton().setEnabled(true);
            }
        }
    }
}
