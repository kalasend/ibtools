package kyl.ib;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.OrderState;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class IBTaskClient extends IBClient
{
	private static final int MAX_TASKS = 100000;

    private Logger logger = Logger.getLogger(IBTaskClient.class);
	protected Task[] tasks = new Task[MAX_TASKS];

	private HistoricalDataLimitation theHistoricalDataLImitation = new HistoricalDataLimitation();
    private List<OrderTask> orderTasks = Collections.synchronizedList(new Vector<OrderTask>());
    private List<MarketDataTask> marketDataTasks = Collections.synchronizedList(new Vector<MarketDataTask>());

    public IBTaskClient(String tws_url) throws Exception
    {
        super(tws_url);
    }


    @Override
	public void error(Exception e) 
	{
		super.error(e);
	}
	
	@Override
	public void error(int id, int errorCode, String errorMsg) 
	{
		super.error(id, errorCode, errorMsg);
		if (id > 0 && tasks[id] != null) 
		{
			TwsError te = new TwsError(id, errorCode, errorMsg);
			tasks[id].error(te);
		}
	}
	
	@Override
	public void error(String str) 
	{
		super.error(str);
	}

	public IBTaskClient(String host, int port, int cid) {
		super(host, port, cid);
		init();
	}

	private void init() {
		Arrays.fill(tasks, null);
	}

	@Override
	public boolean connect() throws Exception
    {
		boolean retval = super.connect();
		if (retval)
		{
            logger.info("Requesting all open orders...");
			eClient.reqAllOpenOrders();
		}
		return retval;
	}
	
	@Override
	public void historicalData(int reqId, String date, double open,
			double high, double low, double close, int volume, int count,
			double WAP, boolean hasGaps) 
	{
		HistoricalDataTask hdt = (HistoricalDataTask) this.tasks[reqId];
		assert hdt != null;
		hdt.addData(date, open, high, low, close, volume, count, WAP, hasGaps);
	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) 
	{
		ContractDetailsTask cdt = (ContractDetailsTask) this.tasks[reqId];
		cdt.addDetails(contractDetails);
	}

	@Override
	public void contractDetailsEnd(int reqId) 
	{
		ContractDetailsTask cdt = (ContractDetailsTask) this.tasks[reqId];
		assert cdt != null;
		cdt.detailsEnd();
	}

	public synchronized void addTask(int id, Task aTask)
	{
		assert id < MAX_TASKS;
		assert tasks[id] == null;
		this.tasks[id] = aTask;

        if (aTask instanceof OrderTask)
        {
            orderTasks.add((OrderTask) aTask);
        }
        else if (aTask instanceof MarketDataTask)
        {
            marketDataTasks.add((MarketDataTask) aTask);
        }
	}

	public void removeTask(int id)
	{
		assert id < MAX_TASKS;
		assert tasks[id] != null;
		this.tasks[id] = null;
	}
	
	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickEFP(tickerId, tickType, basisPoints, formattedBasisPoints, impliedFuture, holdDays,
					futureExpiry, dividendImpact, dividendsToExpiry);
		}
	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickGeneric(0, tickType, value);
		}
	}

	@Override
	public void tickPrice(int tickerId, int field, double price,
			int canAutoExecute) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickPrice(0, field, price, canAutoExecute);
		}
	}

	@Override
	public void tickSize(int tickerId, int field, int size) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickSize(0, field, size);
		}
	}

	@Override
	public void tickSnapshotEnd(int tickerId) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[reqId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickSnapshotEnd(tickerId);
		}
	}

	@Override
	public void tickString(int tickerId, int tickType, String value) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).tickString(0, tickType, value);
		}
	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) 
	{
//		MarketDataTask mdt = (MarketDataTask) tasks[tickerId];
		if (tasks[tickerId] != null)
		{
			((MarketDataTask) tasks[tickerId]).updateMktDepth(tickerId, position, operation, side, price, size);
		}
	}

	@Override
	public void updateMktDepthL2(int tickerId, int position,
			String marketMaker, int operation, int side, double price, int size) 
	{
		if (tasks[tickerId] != null)
		{
			this.updateMktDepth(tickerId, position, operation, side, price, size);
		}
	}

	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName)
	{
//		theLogger.trace("updateAccountValue: " + key + "=" + value);
//		AccountData ad = account.new AccountData();
//		ad.name = key;
//		ad.value = value;
//		ad.currency = currency;
//		ad.owner = accountName;
//		account.addData(ad);
	}

	@Override
	public void updateAccountTime(String timeStamp)
	{
//		account.setTime(timeStamp);
	}

	public boolean isConnected()
	{
		return eClient.isConnected();
	}

	@Override
	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta,
			double undPrice)
	{
		// TODO Auto-generated method stub
		if (tasks[tickerId] != null)
		{
		}
	}

	@Override
	public void orderStatus(int orderId, String status, int filled,
			int remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld)
	{
		if (tasks[orderId] != null)
		{
			OrderTask ot = (OrderTask) tasks[orderId];
			ot.orderStatus(status, filled, remaining, avgFillPrice, permId,
					parentId, lastFillPrice, clientId, whyHeld);
		}
		else
		{
			System.err.println("Orphan orderId=" + orderId);
		}
	}
	
	public HistoricalDataLimitation getHistoricalDataLimitation()
	{
		return theHistoricalDataLImitation ;
	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order,
			OrderState orderState)
	{
		try
		{
            // TODO
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}
	
	@Override
	public void openOrderEnd()
	{
		// TODO Auto-generated method stub
	}
}
