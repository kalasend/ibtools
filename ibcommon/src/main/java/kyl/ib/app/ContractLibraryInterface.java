package kyl.ib.app;

import kyl.ib.SerializableContract;
import kyl.ib.SerializableContractDetails;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface ContractLibraryInterface extends Remote
{
//    public Collection<SerializableContract> popup(boolean allowMultiple) throws RemoteException;
    public Collection<SerializableContractDetails> getAll() throws RemoteException;
}
