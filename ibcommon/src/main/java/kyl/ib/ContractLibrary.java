package kyl.ib;


import com.google.gson.*;
import com.ib.client.*;
import kyl.ib.app.ContractLibraryInterface;
import kyl.ib.app.ContractLibraryServer;
import org.apache.log4j.Logger;

import java.io.*;
import java.rmi.Naming;
import java.util.*;

public class ContractLibrary
{
    private static ContractLibrary _instance = null;

    Logger theLogger = Logger.getLogger(ContractLibrary.class);

    File contractFile;

    List<ContractDetails> contractDetailsList;
    List<SerializableContractDetails> serContractDetailsList;
    Hashtable<Contract, String> contractSummaries;



    public ContractLibrary()
    {
        contractDetailsList = new Vector<ContractDetails>();
        serContractDetailsList = new Vector<SerializableContractDetails>();
        contractSummaries = new Hashtable<Contract, String>();
    }

    public void initFromFile(File conf) throws Exception
    {
        contractFile = conf;
        if (!contractFile.canRead())
        {
            throw new Exception("Unable to read contract/combo files.");
        }

        theLogger.info("Start loading...");

        // load contracts
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .serializeSpecialFloatingPointValues()
                .setPrettyPrinting()
                .create();
        SerializableContractDetails scds[] =
                gson.fromJson(new FileReader(contractFile), SerializableContractDetails[].class);
        theLogger.info("Loaded " + scds.length + " contract details from " + contractFile.getPath());
        Collections.addAll(serContractDetailsList, scds);
        procSerConDets();
    }

    public void initFromSerConDetails(Collection<SerializableContractDetails> scd_coll)
    {
        serContractDetailsList.addAll(scd_coll);
        procSerConDets();
    }

    private void procSerConDets()
    {
        for (SerializableContractDetails scd : serContractDetailsList)
        {
            ContractDetails cd = new ContractDetails();
            scd.mirrorTo(cd);
            contractDetailsList.add(cd);
        }
    }



    public Collection<SerializableContractDetails> getSerContractDetailsList()
    {
        return serContractDetailsList;
    }

    public List<ContractDetails> getAllContractDetails()
    {
        return contractDetailsList;
    }

    public static ContractLibrary getContractLibrary(String conpath)
    {
        try
        {
            if (_instance == null)
            {
                File confile = new File(conpath);
                _instance = new ContractLibrary();
                _instance.initFromFile(confile);
            }
        }
        catch (Exception e)
        {
            Logger.getRootLogger().error("Failed to load file into contract library.", e);
            _instance = null;
        }
        return _instance;
    }

    public static ContractLibrary getContractLibrary()
    {
        try
        {
            if (_instance == null)
            {
                ContractLibraryInterface conlibif = (ContractLibraryInterface) Naming.lookup(ContractLibraryServer.SERVICE_NAME);
                _instance = new ContractLibrary();
                _instance.initFromSerConDetails(conlibif.getAll());
            }
        }
        catch (Exception e)
        {
            Logger.getRootLogger().error("Failed to obtain contract details from contract library.", e);
            _instance = null;
        }
        return _instance;
    }


    public String getSummaryDescription(Contract contract)
    {
        String summary = contractSummaries.get(contract);
        if (summary == null)
        {
            return generateSummary(contract);
        }
        else
        {
            return summary;
        }
    }

    private String generateSummary(Contract c)
    {
        final StringBuffer sb = new StringBuffer();
        sb.delete(0, sb.length());
        String sum = summarizeContract(sb, c).toString();
        contractSummaries.put(c, sum);
        return sum;
    }

    public static StringBuffer summarizeContract(StringBuffer sb, Contract c)
    {
        if (c.m_secType.equals("STK") || c.m_secType.equals("IND"))
        {
            sb.append(c.m_symbol);
        }
        else if (c.m_secType.equals("FUT"))
        {
            sb.append(c.m_localSymbol).append(' ').append(c.m_symbol);
            sb.append(' ').append(c.m_multiplier).append(" FUT ");
            sb.append(c.m_expiry.substring(0, 4)).append('-');
            sb.append(c.m_expiry.substring(4, 6)).append('-');
            sb.append(c.m_expiry.substring(6));
        }
        else if (c.m_secType.equals("OPT") || c.m_secType.equals("FOP"))
        {
            sb.append(c.m_symbol);
            sb.append(' ').append(c.m_multiplier);
            sb.append(' ').append(c.m_right.charAt(0)).append(' ').append(String.format("%.2f", c.m_strike));
            sb.append(' ').append(c.m_expiry.substring(0, 4)).append('-')
                .append(c.m_expiry.substring(4, 6)).append('-')
                .append(c.m_expiry.substring(6));
        }
        else if (c.m_secType.equals("BAG"))
        {
            return sb.append(c.m_comboLegsDescrip);
        }
        sb.append(" (").append(c.m_exchange).append(",")
            .append(c.m_currency).append(')');
        return sb;
    }

    public Contract getContract(int conId, String exchange)
    {
        for (ContractDetails cd : contractDetailsList)
        {
            if (cd.m_summary.m_conId == conId
                    && cd.m_summary.m_exchange.equals(exchange))
            {
                return cd.m_summary;
            }
        }
        return null;
    }

    public Contract getContract(Contract cont)
    {
        for (ContractDetails cd : contractDetailsList)
        {
            if (cd.m_summary.equals(cont))
            {
                return cd.m_summary;
            }
        }
        return getContract(cont.m_conId, cont.m_exchange);
    }


    public ContractDetails getContractDetails(Contract con)
    {
        Contract checkCon = con;
        for (ContractDetails cd : contractDetailsList)
        {
            Contract cd_con = cd.m_summary;
            if (cd_con.equals(checkCon))
            {
                return cd;
            }
        }

        return null;
    }
}
