package kyl.ib.util;


import com.google.gson.*;
import com.ib.client.*;
import kyl.ib.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ContractScan
{
    private Vector<ContractDetails> condets = new Vector<ContractDetails>();
    private File conFile;
    private IBTaskClient client;

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java kyl.ib.util.ContractScan <contracts file> ...");
            System.exit(1);
        }

        ContractScan cs = new ContractScan();
        cs.setConFile(args[0]);

        // TODO: Use Getopt for more advanced option parsing
//        Getopt getopt = new Getopt("contractScan", args, "s:i:f:v");

        cs.start();
    }

    private void start() throws Exception
    {
        client = new IBTaskClient("localhost", 7496, 11);
        client.connect();

        String stock_syms[] = {
                "SPY", "QQQ",
                "SDS",
                "XLF", "SKF",
                "GLD", "PTM", "SLV", "JJC",
                "PPLT", "PHYS", "PSLV",
                "GDX", "SIL",
                "USO", "DTO", "GAZ",
                "TLT", "EEM", "UUP",
                "VXX", "VXZ", "XIV", "TVIX",
                "FXI", "FXP"};
        for (String sym : stock_syms)
        {
            //condets.addAll(scanStock(sym, "USD"));
        }

        String index_syms[] = {
                "SPX", "INDU", "NDX",
                "VIX", "VXO", "VXV",
                "DX", "TICK-NYSE", "TICK-NASD"
        };
        for (String sym : index_syms)
        {
//            condets.addAll(scanIndex(sym));
        }

        /*
         *      VIX futures, futures spreads and options
         */
        Collection<ContractDetails> vxfuts = scanFutures("VIX", "CFE");
        condets.addAll(vxfuts);

        for (int numMonths = 1; numMonths <= 7; numMonths++)
        {
            Collection<? extends ContractDetails> mySpreads = futureSpreads(vxfuts, numMonths, "VIX", "CFE");
            for (ContractDetails cd : mySpreads)
            {
                // must manually set m_minTick for future spreads
                cd.m_minTick = 0.01;
            }
            condets.addAll(mySpreads);
        }

        String[] vxMonths = collectMonths(vxfuts);
        for (String month : vxMonths)
        {
            for (String callput : new String[] {"C", "P"})
            {
                System.out.println("Scan options for VIX at CBOE for " + month + " " + callput);
                condets.addAll(scanOptions("VIX", "CBOE", month, callput, "USD"));
                Thread.sleep(1000);
            }
        }

//        condets.addAll(scanFutures("ES", "GLOBEX"));
//        condets.addAll(scanFutures("NQ", "GLOBEX"));
//        condets.addAll(scanFutures("GC", "NYMEX"));
//        condets.addAll(scanFutures("PL", "NYMEX"));
//        condets.addAll(scanFutures("SI", "NYMEX"));
//        condets.addAll(scanFutures("CL", "NYMEX"));

        client.disconnect();

        writeContractsToGson();
    }

    private String[] collectMonths(Collection<ContractDetails> futs) throws InterruptedException
    {
        Vector<String> mos = new Vector<String>();
        for (ContractDetails cd : futs)
        {
            mos.add(cd.m_contractMonth);
        }
        return mos.toArray(new String[0]);
    }

    private Collection<? extends ContractDetails> futureSpreads(
            Collection<ContractDetails> futs, int n, String sym, String exch)
    {
        //
        // The important assumption is that all futures in futs are of the same underlying
        //

        Vector<ContractDetails> combos = new Vector<ContractDetails>();

        for (ContractDetails cd1 : futs)
        {
            int year = Integer.parseInt(cd1.m_contractMonth.substring(0, 4));
            int month = Integer.parseInt(cd1.m_contractMonth.substring(4)) + n;
            if (month > 12)
            {
                year++;
                month = month - 12;
            }
            String otherMonth = String.format("%4d%02d", year, month);

            for (ContractDetails cd2 : futs)
            {
                if (cd2.m_contractMonth.equals(otherMonth))
                {
                    Contract comboCont = new Contract();
//                    comboCont.m_symbol = "USD";   // API doc says "USD" should be used for combo's
                                                    // However, I found that to get the complex book data,
                                                    // m_symbol must be same as leg's m_symbol, AND
                                                    // m_exchange must be the exchange for complex books(likely
                                                    // the same as leg's m_exchange)
//                    comboCont.m_exchange = "SMART";
                    comboCont.m_symbol = sym;
                    comboCont.m_exchange = exch;
                    comboCont.m_secType = "BAG";
                    comboCont.m_currency = cd1.m_summary.m_currency;
                    comboCont.m_comboLegs.add(
                            new ComboLeg(cd1.m_summary.m_conId, 1, "SELL", exch, 0)
                    );
                    comboCont.m_comboLegs.add(
                            new ComboLeg(cd2.m_summary.m_conId, 1, "BUY", exch, 0)
                    );
                    comboCont.m_comboLegsDescrip = genComboDescrip(comboCont, cd1, cd2);

                    // for BAG, we clone the contract details from leg #1
                    // but changes m_summary to the combo contract
                    ContractDetails comboCd = new ContractDetails(
                            comboCont,
                            cd1.m_marketName,
                            cd1.m_tradingClass,
                            cd1.m_minTick,
                            cd1.m_orderTypes,
                            cd1.m_validExchanges,
                            cd1.m_underConId,
                            cd1.m_longName,
                            cd1.m_contractMonth,
                            cd1.m_industry,
                            cd1.m_category,
                            cd1.m_subcategory,
                            cd1.m_timeZoneId,
                            cd1.m_tradingHours,
                            cd1.m_liquidHours);


                    combos.add(comboCd);
                }
            }
        }
        return combos;  //To change body of created methods use File | Settings | File Templates.
    }

    private String genComboDescrip(Contract cbo, ContractDetails cd1, ContractDetails cd2)
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            ComboLeg leg1 = (ComboLeg) cbo.m_comboLegs.get(0);
            ComboLeg leg2 = (ComboLeg) cbo.m_comboLegs.get(1);
            sb.append(cd1.m_summary.m_symbol).append(' ')
                    .append(cd1.m_summary.m_secType).append(" Cmb ");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Calendar leg1Exp = Calendar.getInstance();
            leg1Exp.setTime(sdf.parse(cd1.m_summary.m_expiry));
            Calendar leg2Exp = Calendar.getInstance();
            leg2Exp.setTime(sdf.parse(cd2.m_summary.m_expiry));
            sb.append(leg1Exp.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()))
                    .append('-').append(leg1Exp.get(Calendar.YEAR) % 100);
            sb.append(' ');
            sb.append(leg2Exp.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()))
                    .append('-').append(leg2Exp.get(Calendar.YEAR) % 100);
            sb.append(" (").append(cd2.m_summary.m_exchange).append(',').append(cd1.m_summary.m_currency).append(')');
            return sb.toString();
        }
        catch (Exception e)
        {
            org.apache.log4j.Logger.getRootLogger().error("Failed to form combo description: ", e);
            return null;
        }
    }

    private String genComboDescrip_old(Contract cbo, ContractDetails cd1, ContractDetails cd2)
    {
        // assuming 2-leg combos only AND that all legs' contracts exist in this library

        StringBuffer sb = new StringBuffer();
        ComboLeg leg1 = (ComboLeg) cbo.m_comboLegs.get(0);
        ComboLeg leg2 = (ComboLeg) cbo.m_comboLegs.get(1);
        sb.append(leg1.m_action.equalsIgnoreCase("BUY")? '+' : '-').append(cd1.m_summary.m_localSymbol)
                .append(' ')
                .append(leg2.m_action.equalsIgnoreCase("BUY")? '+' : '-').append(cd2.m_summary.m_localSymbol);
        sb.append(' ').append(leg1.m_action.equalsIgnoreCase("BUY")? '+' : '-')
                .append(cd1.m_summary.m_expiry.substring(0, 4)).append('-')
                .append(cd1.m_summary.m_expiry.substring(4, 6)).append('-')
                .append(cd1.m_summary.m_expiry.substring(6))
                .append(' ')
                .append(leg2.m_action.equalsIgnoreCase("BUY")? '+' : '-')
                .append(cd1.m_summary.m_expiry.substring(0, 4)).append('-')
                .append(cd2.m_summary.m_expiry.substring(4, 6)).append('-')
                .append(cd2.m_summary.m_expiry.substring(6));
        sb.append(" (").append(cd2.m_summary.m_exchange).append(',').append(cd1.m_summary.m_currency).append(')');
        return sb.toString();
    }

    // serialize contract data to file via Java serialization
    private void writeContractsToJSer() throws IOException
    {
        File serfile = new File(conFile.getPath() + ".ser");
        ContractDetails[] cds = condets.toArray(new ContractDetails[0]);
        SerializableContractDetails[] scds = new SerializableContractDetails[cds.length];
        for (int i = 0; i < cds.length; i++)
        {
            scds[i] = new SerializableContractDetails();
            scds[i].mirrorFrom(cds[i]);
        }
        System.out.println("Writing " + scds.length + " contracts to " + serfile.getPath());
        FileOutputStream confos = new FileOutputStream(serfile);
        ObjectOutputStream conoos = new ObjectOutputStream(confos);
        conoos.writeObject(scds);
        conoos.close();
        confos.close();
    }

    // serialize contract data to file via GSON
    private void writeContractsToGson() throws IOException
    {
        ContractDetails[] cds = condets.toArray(new ContractDetails[0]);
        SerializableContractDetails[] scds = new SerializableContractDetails[cds.length];
        for (int i = 0; i < cds.length; i++)
        {
            scds[i] = new SerializableContractDetails();
            scds[i].mirrorFrom(cds[i]);
        }
        System.out.println("Writing " + scds.length + " contracts to " + conFile.getPath());
        FileWriter confw = new FileWriter(conFile);
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .serializeSpecialFloatingPointValues()
                .setPrettyPrinting()
                .create();
        gson.toJson(scds, confw);
        confw.close();
    }

    private Collection<? extends ContractDetails> scanOptions(String sym, String exch, String month, String callput, String cur) throws InterruptedException
    {
        Contract scancon = new Contract();
        scancon.m_symbol = sym;
        scancon.m_exchange = exch;
        scancon.m_expiry = month;
        scancon.m_secType = "OPT";
        scancon.m_currency = cur;
        ContractDetailsTask cdt = new ContractDetailsTask(client, scancon);
        cdt.start();
        cdt.waitForCompletion();
        return cdt.details;
    }

    private Collection<? extends ContractDetails> scanStock(String sym, String cur) throws InterruptedException
    {
        Contract scancon = new Contract();
        scancon.m_symbol = sym;
        scancon.m_currency = cur;
        scancon.m_secType = "STK";
        ContractDetailsTask cdt = new ContractDetailsTask(client, scancon);
        cdt.start();
        cdt.waitForCompletion();

        return cdt.details;
    }

    private Collection<ContractDetails> scanFutures(String sym, String exch) throws InterruptedException
    {
        Contract scancon = new Contract();
        scancon.m_symbol = sym;
        scancon.m_exchange = exch;
        scancon.m_secType = "FUT";
        ContractDetailsTask cdt = new ContractDetailsTask(client, scancon);
        cdt.start();
        cdt.waitForCompletion();
        return cdt.details;
    }

    private Collection<? extends ContractDetails> scanIndex(String sym) throws InterruptedException
    {
        Contract scancon = new Contract();
        scancon.m_symbol = sym;
        scancon.m_secType = "IND";
        ContractDetailsTask cdt = new ContractDetailsTask(client, scancon);
        cdt.start();
        cdt.waitForCompletion();
        return cdt.details;
    }

    public void setConFile(String path)
    {
        this.conFile = new File(path);
    }
}
