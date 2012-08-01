package kyl.ib;


import com.ib.client.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

import static java.lang.Integer.*;

public class IBClient implements EWrapper
{
	
	private static final int MAX_NUM_REQ_IDS = 100000;
    private static final int CONNECTION_WAIT_SECS = 10;
    private static final int MaxConcurrentQueries = 100;

    private Logger logger = Logger.getLogger(IBClient.class);
    
	protected String twsHost = null;
	protected int twsPort = -1;
    protected int twsClientId = -1;
    protected EClientSocket eClient = null;
    protected int nextId = -1;
    private int[] usableClientIds = null;

    private int lastErrorCode = -1;


//	protected Semaphore queryThrottleSem = new Semaphore(MaxConcurrentQueries, true);
	protected BitSet idAllocBits = new BitSet(MAX_NUM_REQ_IDS);

	protected long serverTimestamp;
    protected Lock conditionLock;
    protected Condition connectionCondition;
//    protected Condition disconnectedCondition;

    public IBClient(String url) throws Exception
    {
        Pattern p = Pattern.compile("tws://([^:]+):(\\d+)/(\\d+)(-\\d+)?");
        Matcher m = p.matcher(url);
        if (m.matches())
        {
            twsHost = m.group(1);
            twsPort = parseInt(m.group(2));
            int x = parseInt(m.group(3));
            int y = x;
            if (!m.group(4).equals(""))
            {
                y = parseInt(m.group(4).substring(1));
            }
            usableClientIds = new int[y - x + 1];
            for (int i = x; i <= y; i++)
            {
                usableClientIds[i - x] = i;
            }
        }
        else
        {
            throw new Exception("Incorrect URL format");
        }

        conditionLock = new ReentrantLock();
        connectionCondition = conditionLock.newCondition();
//        disconnectedCondition = conditionLock.newCondition();
        eClient = new EClientSocket(this);
    }

	public IBClient(String host, int port, int cid)
	{
		twsHost = host;
		twsPort = port;
        usableClientIds = new int[] {cid};

        conditionLock = new ReentrantLock();
        connectionCondition = conditionLock.newCondition();
//        disconnectedCondition = conditionLock.newCondition();
        eClient = new EClientSocket(this);
	}

    public IBClient(String host, int port, int[] cids)
    {
        twsHost = host;
        twsPort = port;
        usableClientIds = cids;

        conditionLock = new ReentrantLock();
        connectionCondition = conditionLock.newCondition();
//        disconnectedCondition = conditionLock.newCondition();
        eClient = new EClientSocket(this);
    }


	public EClientSocket getEClient() 
	{
		return eClient;
	}
	
	public boolean connect() throws Exception
    {
//		logger.info("BaseTwsClient: Connecting to %s:%d with ID %d\n", twsHost, twsPort, twsClientId);

        try
        {
            for (int i = 0; i < usableClientIds.length && !isReady(); i++)
            {
                twsClientId = usableClientIds[i];
                conditionLock.lock();
                if (eClient.isConnected())
                {
                    logger.info("Disconnecting...");
                    eClient.eDisconnect();
                    Thread.sleep(1000);
                }
                logger.info("Attempting to connect to " + twsHost + " at port " + twsPort + " as client " + twsClientId);
                eClient.eConnect(twsHost, twsPort, twsClientId);
                connectionCondition.await(CONNECTION_WAIT_SECS, TimeUnit.SECONDS);
                conditionLock.unlock();
            }
        }
        catch (InterruptedException e)
        {
            if (!isReady())
                throw e;      // if we are connected, don't care
        }

		return isReady();
	}

    private boolean isReady()
    {
        return eClient.isConnected() && nextId != -1;
    }

    public void disconnect() throws InterruptedException
    {
		if (!eClient.isConnected())
			return;

        try
        {
            conditionLock.lock();
            eClient.eDisconnect();
            if (eClient.isConnected())
            {
                connectionCondition.await(CONNECTION_WAIT_SECS, TimeUnit.SECONDS);
            }
            conditionLock.unlock();
        }
        catch (InterruptedException e)
        {
            if (eClient.isConnected())
                throw e;      // if we are disconnected, don't care
        }
    }

	protected synchronized int getNextId()
	{
		int id = idAllocBits.nextClearBit(0);
		idAllocBits.set(id);
		return nextId + id;
	}

	public synchronized boolean useId(int i)
	{
		if (idAllocBits.get(i))
		{
			return false;
		}
		idAllocBits.set(i);
		return true;
	}
	
	protected synchronized void freeId(int id)
	{
		int offset = id - nextId;
		assert (offset >= 0) && idAllocBits.get(offset);
		idAllocBits.clear(offset);
	}
	
	@Override
	public void accountDownloadEnd(String accountName) 
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
	}

	@Override
	public void contractDetailsEnd(int reqId) {
	}

	@Override
	public void currentTime(long time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deltaNeutralValidation(int reqId, UnderComp underComp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetailsEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fundamentalData(int reqId, String data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalData(int reqId, String date, double open,
			double high, double low, double close, int volume, int count,
			double WAP, boolean hasGaps) {
	}

	@Override
	public void managedAccounts(String accountsList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nextValidId(int orderId) 
	{
		nextId = orderId;
        logger.info("Next valid id=" + orderId);
        conditionLock.lock();
        connectionCondition.signalAll();
        conditionLock.unlock();
	}

//	@Override
//	public void openOrder(int orderId, Contract contract, Order order,
//			OrderState orderState) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void openOrderEnd() {
//		// TODO Auto-generated method stub
//
//	}

	@Override
	public void orderStatus(int orderId, String status, int filled,
			int remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld) {
		// TODO Auto-generated method stub

	}

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void openOrderEnd()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateAccountValue(String key, String value, String currency, String accountName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateAccountTime(String timeStamp)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public void realtimeBar(int reqId, long time, double open, double high,
			double low, double close, long volume, double wap, int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveFA(int faDataType, String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerData(int reqId, int rank,
			ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerDataEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerParameters(String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickOptionComputation( int tickerId, int field, double impliedVol,
    		double delta, double optPrice, double pvDividend,
    		double gamma, double vega, double theta, double undPrice)
    {
    	
    }

	@Override
	public void tickPrice(int tickerId, int field, double price,
			int canAutoExecute) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void tickSize(int tickerId, int field, int size) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void tickSnapshotEnd(int reqId) 
	{
		// TODO Auto-generated method stub
	}

    public void marketDataType(int reqId, int marketDataType)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public void tickString(int tickerId, int tickType, String value) {
		// TODO Auto-generated method stub
	}

//	@Override
//	public void updateAccountTime(String timeStamp) 
//	{
//	}

	public long getDelay()
	{
		return System.currentTimeMillis() - serverTimestamp;
	}

	/*
	@Override
	public void updateAccountValue(String key, String value, String currency,
			String accountName) 
	{
	}
	*/

	@Override
	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) 
	{
	}

	@Override
	public void updateMktDepthL2(int tickerId, int position,
			String marketMaker, int operation, int side, double price, int size) 
	{
	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) 
	{
		// TODO Auto-generated method stub

	}

	/*
	@Override
	public void updatePortfolio(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) 
	{
	}
	*/

	@Override
	public void connectionClosed()
    {
        conditionLock.lock();
        connectionCondition.signalAll();
        conditionLock.unlock();
		logger.info("Connection closed for client_id " + twsClientId);
	}

	@Override
	public void error(Exception e)
    {
        logger.error("Exception: ", e);
	}

	@Override
	public void error(String str)
    {
		logger.error("TWS[E]: " + str);
	}

	@Override
	public void error(int id, int errorCode, String errorMsg) 
	{
        assert(id == -1);
        lastErrorCode = errorCode;
        switch (errorCode)
        {
        case 326:
            conditionLock.lock();
            logger.warn(String.format("TWS[E]: id=%d  code=%d  msg=\"%s\"", id, errorCode, errorMsg));
            connectionCondition.signalAll();
            conditionLock.unlock();
            break;
        case 502:
            conditionLock.lock();
            logger.warn(String.format("TWS[E]: id=%d  code=%d  msg=\"%s\"", id, errorCode, errorMsg));
            connectionCondition.signalAll();
            conditionLock.unlock();
            break;
        default:
            logger.info(String.format("TWS[E]: id=%d  code=%d  msg=\"%s\"", id, errorCode, errorMsg));
        }
	}
}
