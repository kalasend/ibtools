package kyl.ib;

import com.ib.client.Contract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class MarketDataLogTask extends MarketDataTask
{
    File sinkfile = null;
    boolean testing = false;
    boolean stopped = false;
    PrintWriter sink = null;
    boolean doAppend = false;

    public MarketDataLogTask(File file, Contract aContract, IBTaskClient aClient,
                             boolean append)
    {
        super(aContract, null, aClient);
        sinkfile = file;
        doAppend = append;
    }

    public MarketDataLogTask(File file, Contract aContract, IBTaskClient aClient)
    {
        this(file, aContract, aClient, false);
    }

    public void init() throws Exception
    {
        sink = new PrintWriter(new BufferedWriter(new FileWriter(sinkfile, doAppend), 4096), true);
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run()
                    {
                        if (sink != null)
                        {
                            sink.flush();
                            sink.close();
                        }
                    }
                }
        );
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute)
    {
        sink.print(System.currentTimeMillis());
        sink.print(",P,");
        sink.print(field);
        sink.print(',');
        sink.println(price);
    }

    @Override
    public void tickSize(int tickerId, int field, int size)
    {
        sink.print(System.currentTimeMillis());
        sink.print(",S,");
        sink.print(field);
        sink.print(',');
        sink.println(size);
    }

    @Override
    public void tickString(int tickerId, int tickType, String value)
    {
        sink.print(System.currentTimeMillis());
        sink.print(",Z,");
        sink.print(tickType);
        sink.print(',');
        sink.println(value);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value)
    {
        sink.print(System.currentTimeMillis());
        sink.print(",G,");
        sink.print(tickType);
        sink.print(',');
        sink.println(value);
    }

    @Override
    public void addHandler(MarketDataHandler aHandler)
    {
        // do nothing
    }

    @Override
    public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size)
    {
        sink.print(System.currentTimeMillis());
        sink.print(",D,");
        sink.print(position);
        sink.print(',');
        sink.print(operation);
        sink.print(',');
        sink.print(side);
        sink.print(',');
        sink.print(price);
        sink.print(',');
        sink.println(size);
    }

    @Override
    public void stop()
    {
        try
        {
            stopped = true;
            if (!testing)
            {
                super.stop();
            }
            sink.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


}
