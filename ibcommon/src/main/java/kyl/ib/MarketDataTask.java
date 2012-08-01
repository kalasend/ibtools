package kyl.ib;

import com.ib.client.*;
import kyl.ib.util.*;
import org.apache.log4j.*;

import java.util.*;

public class MarketDataTask extends Task
{
/*

    Notes about IB market data:
    1. Best bid/ask data is always slower (by about 0.3 secs) from depth data.
       So when depth is enabled, top level depth data should be ignored

    2. If there's a new trade and LAST price does not change, then only LASTSIZE
       is sent.

    3. If a new trade is done and trade price is different from LAST, then two
       LASTSIZE ticks would follow, though they refer to the same trade.
 */

	private static final int MAX_DEPTH_ROWS = 10;

	private static final int HAS_LAST = 16;
	private static final int HAS_LSIZE = 32;
	private static final int INITIAL_LAST_DISPATCH_FLAG = ~(HAS_LAST | HAS_LSIZE);

	private int lastDispatchFlag = INITIAL_LAST_DISPATCH_FLAG;

	Logger theLogger = Logger.getLogger(MarketDataTask.class);

    boolean debugEnabled = false;

	Contract theContract;
    RetailBook theBook;
	String theGenericTickList = "225,233";
	int theNumRows = -1;
	int theDepthReqId = -1;
	List<MarketDataHandler> theHandlers = Collections.synchronizedList(new Vector<MarketDataHandler>());
	boolean snapshotMode = false;
    private boolean doAggregateLastDoneData = true;
    private long numTradesSinceLast = 0;
    private boolean muoba = true;   // market update on bid/ask,
                                    // ie. if bid/ask crosses, remove opposite levels
    private double mpv = 0.01;



	public MarketDataTask(Contract aContract, IBTaskClient aClient)
	{
		this(aContract, null, aClient);
	}

	public MarketDataTask(Contract aContract, String genTicks, IBTaskClient aClient)
	{
		super(aClient);
		theContract = aContract;
		theBook = new RetailBook();
		if (genTicks != null)
		{
			theGenericTickList = genTicks;
		}
	}

    public void enableDepth(int n)
	{
		theNumRows = Math.min(n, MAX_DEPTH_ROWS);
		theDepthReqId = theClient.getNextId();
		theClient.addTask(theDepthReqId, this);
		if (getTaskState() == TaskState.STARTED)
		{
			sendDepthRequest(theNumRows);
		}
	}

	@Override
	public void start() 
	{
		if (getTaskState() != TaskState.STARTED)
		{
			snapshotMode = false;
			theClient.getEClient().reqMktData(theTaskId, theContract, null, false);
			if (theNumRows >= 1)
			{
				sendDepthRequest(theNumRows);
			}
			setTaskState(TaskState.STARTED);
		}
		else
		{
			theLogger.debug("MarketDataTask: reqMktData already sent.");
		}
	}
	
	private void sendDepthRequest(int nRows)
	{
		theClient.getEClient().reqMktDepth(theDepthReqId, theContract, nRows);
	}

	public void stop()
	{
		theClient.getEClient().cancelMktData(theTaskId);
		if (theDepthReqId >= 0)
		{
			theClient.getEClient().cancelMktDepth(theDepthReqId);
		}
	}
	
	protected void parseHalted(double value)
	{
		if (value < 1.0d)
		{
			theBook.setState(RetailBookState.HALTED);
		}
		else if (value >= 1.0d)
		{
			theBook.setState(RetailBookState.NORMAL);
		}
	}

	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) 
	{
		// TODO 
		theLogger.info("tickEFP: id=" + tickerId + " type=" + TickType.getField(tickType) + " more...");
	}

	public void tickGeneric(int tickerId, int tickType, double value) 
	{
		switch (tickType)
		{
		case TickType.HALTED:
			parseHalted(value);
			break;
		default:
			theLogger.info("tickGeneric: id=" + tickerId + " type=" + TickType.getField(tickType) + " value=" + value);
		}
	}

	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double modelPrice,
			double pvDividend) 
	{
		// TODO 
		theLogger.debug("tickOptionComputation: id=" + tickerId + " field=" + field);
	}

	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) 
	{
		if (debugEnabled)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("{").append(theContract).append("} tickPrice: field=").append(TickType.getField(field))
				.append(" price=").append(price);
            theLogger.debug(sb);
		}
		
		RetailBook ts = theBook;
        long t = System.currentTimeMillis();
		switch (field)
		{
		case TickType.BID:
            ts.setBid(price, t);
			if (muoba)
            {
                boolean removed = false;
                while (!Double.isNaN(ts.ask()) && DoubleMath.ge(price, ts.ask()))
                {
                    theLogger.debug("Bid >= ask. Artificially remove ask.");
                    ts.removeAsk(0, t);
                    removed = true;
                }
                if (removed && Double.isNaN(ts.ask()))
                {
                    theLogger.debug("Adding synthetic ask");
                    ts.setAsk(ts.bid() + mpv, t);
                    ts.setAskSize(1, t);
                }
            }
			callHandlersMarket(TickType.BID);
			break;
		case TickType.ASK:
			ts.setAsk(price, t);
            if (muoba)
            {
                boolean removed = false;
                while (!Double.isNaN(ts.bid()) && DoubleMath.le(price, ts.bid()))
                {
                    theLogger.debug("Ask >= bid. Artificially remove bid.");
                    ts.removeBid(0, t);
                    removed = true;
                }
                if (removed && Double.isNaN(ts.bid()))
                {
                    theLogger.debug("Adding synthetic bid");
                    ts.setBid(ts.ask() - mpv, t);
                    ts.setBidSize(1, t);
                }
            }
			callHandlersMarket(TickType.ASK);
			break;
		case TickType.LAST:
            numTradesSinceLast = 0;
			ts.setLast(price, t);
			lastDispatchFlag |= HAS_LAST;
			callHandlersLastdone(TickType.LAST);
			break;
		}
	}

	public void tickSize(int tickerId, int field, int size) 
	{
		if (debugEnabled)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("{").append(theContract).append("} tickSize: field=").append(TickType.getField(field))
				.append(" size=").append(size);
            System.err.println(sb);
		}

		RetailBook ts = theBook;
        long t = System.currentTimeMillis();
		switch (field)
		{
		case TickType.BID_SIZE:
			ts.setBidSize(size, t);
			callHandlersMarket(TickType.BID_SIZE);
			break;
		case TickType.ASK_SIZE:
			ts.setAskSize(size, t);
			callHandlersMarket(TickType.ASK_SIZE);
			break;
		case TickType.LAST_SIZE:
            if (numTradesSinceLast != 1)    // refer to Note 2,3
            {
                lastDispatchFlag |= HAS_LSIZE;
                ts.setLastSize(size, t);
                callHandlersLastdone(TickType.LAST_SIZE);
            }
            numTradesSinceLast++;
			break;
		case TickType.VOLUME:
			ts.setVolume(size, t);
			break;
		}
	}

	public void tickString(int tickerId, int tickType, String value) 
	{
		if (debugEnabled)
		{
			StringBuffer sb = new StringBuffer();
			sb.append(theContract).append(" tickString: field=").append(TickType.getField(tickType))
				.append(" value=").append(value);
			System.err.println(sb);
		}

        long t = System.currentTimeMillis();
		switch (tickType)
		{
		case TickType.RT_VOLUME:
			String[] tokens = value.split(";");
			double price = Double.parseDouble(tokens[0]);
			long size = Long.parseLong(tokens[1]);
			long timestamp = Long.parseLong(tokens[2]);
			long volume = Long.parseLong(tokens[3]);
			
			// *** NOTE: skipping the VWAP price and block trade boolean
			
			theBook.setLast(price, t);
			lastDispatchFlag |= HAS_LAST;
			theBook.setLastSize(size, t);
			lastDispatchFlag |= HAS_LSIZE;
			theBook.setVolume(volume, t);
//			theBook.setTimestamp(timestamp);

			callHandlersLastdone(TickType.VOLUME);
			break;
			
		case TickType.LAST_TIMESTAMP:
			// TWS last timestamp is in seconds and is of little use to be kept. So ignore.  
			break;
			
		default:
			theLogger.info("tickString: id=" + tickerId + " type=" + TickType.getField(tickType) + " value=" + value);
		}
	}
	
	private void callHandlersMarket(int type)
	{
		if (!snapshotMode)
		{
			if (debugEnabled)
			{
				System.err.println("{" + theContract + "} Calling handlers market-update");
			}

			for (MarketDataHandler handler : theHandlers)
			{
				handler.marketUpdate(theBook, type);
			}
		}
	}

	private void callHandlersLastdone(int type)
	{
		if (!snapshotMode)
		{
			if (debugEnabled)
			{
				System.err.println("{" + theContract + "} Calling handlers last-done");
			}
			
			if (!doAggregateLastDoneData || isLastDispatchReady())
			{
				for (MarketDataHandler handler : theHandlers)
				{
					handler.lastDone(theBook, type);
				}
				lastDispatchFlag = INITIAL_LAST_DISPATCH_FLAG;
			}
		}
	}
	
	private boolean isLastDispatchReady()
	{
		return lastDispatchFlag == -1;
	}

	public void addHandler(MarketDataHandler aHandler)
	{
		if (!theHandlers.contains(aHandler))
		{
			theHandlers.add(aHandler);
		}
	}

	public void removeHandler(MarketDataHandler aHandler)
	{
		synchronized (theHandlers)
		{
			theHandlers.remove(aHandler);
		}
	}

	public void tickSnapshotEnd(int reqId) 
	{
		if (snapshotMode)
		{
			snapshotMode = false;
			callHandlersMarket(TickType.SNAPSHOT);
			theClient.getEClient().reqMktData(theTaskId, theContract, theGenericTickList, false);
		}
	}
	
	public void setGenericTickList(String genticks)
	{
		theGenericTickList = genticks;
	}

	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) 
	{
        if (position > 0)   // refer to Note 1
        {
            long t = System.currentTimeMillis();
            switch (operation)
            {
            case 0:
                if (side == 0)
                {
                    theBook.insertAsk(position, price, size, t);
                    callHandlersMarket(TickType.ASK_INSERT);
                }
                else
                {
                    theBook.insertBid(position, price, size, t);
                    callHandlersMarket(TickType.BID_INSERT);
                }
                break;
            case 1:
                if (side == 0)
                {
                    theBook.updateAsk(position, price, size, t);
                    callHandlersMarket(TickType.ASK_UPDATE);
                }
                else
                {
                    theBook.updateBid(position, price, size, t);
                    callHandlersMarket(TickType.BID_UPDATE);
                }
                break;
            case 2:
                if (side == 0)
                {
                    theBook.removeAsk(position, t);
                    callHandlersMarket(TickType.ASK_REMOVE);
                }
                else
                {
                    theBook.removeBid(position, t);
                    callHandlersMarket(TickType.BID_REMOVE);
                }
                break;
            }
        }
	}

	public Contract getContract()
	{
		return theContract;
	}
	
	public void setAggreateLastDoneData(boolean b)
	{
		doAggregateLastDoneData = b;
	}

    public boolean isMuoba()
    {
        return muoba;
    }

    public void setMuoba(boolean muoba, double mpv)
    {
        this.muoba = muoba;
        this.mpv = mpv;
    }
}
