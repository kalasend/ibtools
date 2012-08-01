package kyl.ib.app;

import gnu.getopt.Getopt;
import kyl.ib.ContractLibrary;
import kyl.ib.SerializableContractDetails;
import org.apache.log4j.BasicConfigurator;

import javax.swing.*;
import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

public class ContractLibraryServer
{
    public static final String SERVICE_NAME = "Contract_Server";

    private static ContractLibraryServer _instance = null;

    private JFrame mainFrame;
    private String dataFile;
    private ContractLibrary contractLibrary;

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.err.println("Usage: ContractLibraryServer <data_file>");
            System.exit(1);
        }

        ContractLibraryServer cs = ContractLibraryServer.getInstance();
        Getopt g = new Getopt("ContractLibraryServer", args, "");

        int c;
        while ((c = g.getopt()) != -1)
        {
            switch (c)
            {
            }
        }

        cs.setDataFile(args[g.getOptind()]);
        cs.start();
    }

    public ContractLibraryServer()
    {
        BasicConfigurator.configure();
        mainFrame = new JFrame("Contract Server");
    }

    private static ContractLibraryServer getInstance()
    {
        if (_instance == null)
        {
            _instance = new ContractLibraryServer();
        }
        return _instance;
    }

    private void start() throws Exception
    {
        contractLibrary = new ContractLibrary();
        contractLibrary.initFromFile(new File(dataFile));

        // TODO: establish RMI interface
        RmiImplementation rimpl = new RmiImplementation();
        ContractLibraryInterface stub = (ContractLibraryInterface) UnicastRemoteObject.exportObject(rimpl, 0);
        Naming.rebind(SERVICE_NAME, stub);

    }

    public void setDataFile(String path)
    {
        dataFile = path;
    }


    // RMI implementation
    public class RmiImplementation implements ContractLibraryInterface
    {

        /*
        @Override
        public Collection<SerializableContract> popup(boolean allowMultiple) throws RemoteException
        {
            ContractChooser cch = new ContractChooser(contractLibrary);
            cch.setParentFrame(mainFrame);
            int retval = cch.showDialog(allowMultiple);
            if (retval == ContractChooser.APPROVED)
            {
                SerializableContract con = new SerializableContract(cch.getSelected());
                Collection<SerializableContract> coll = new Vector<SerializableContract>();
                coll.add(con);
                return coll;
            }
            return null;
        }
        */

        @Override
        public Collection<SerializableContractDetails> getAll() throws RemoteException
        {
            return contractLibrary.getSerContractDetailsList();
        }
    }
}
