package kyl.ib;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class HistoricalDataLimitation
{
	private static final int MAX_REQ_IN_TEN_MINS = 60;
	private static final long TEN_MINS_IN_MILLIS = 600 * 1000;
	private static final long MIN_INTERVAL_IN_MILLIS = 50;
	
	ArrayDeque<RequestRecord> reqsInTenMin = new ArrayDeque<RequestRecord>();
	ReentrantLock lock = new ReentrantLock();
	
	private class RequestRecord
	{
		public long timestamp;
		public int conId;
		public String exchange;
		public String tickType;
		public String duration;
		public String endTime;
	}

	public HistoricalDataLimitation()
	{
		
	}
	
	public void waitInLine(int conId, String exchange, String tickType, String duration, String endTime)
	{
		lock.lock();
		try
		{
			removeOldRecords();
			
			RequestRecord rr = new RequestRecord();
			rr.conId = conId;
			rr.exchange = exchange;
			rr.tickType = tickType;
			rr.duration = duration;
			rr.endTime = endTime;

			long sleepTime = MIN_INTERVAL_IN_MILLIS + Math.max(
					identicalReqWait(rr),
					Math.max(
							similarReqWait(rr), 
							maxInTenMinsWait()));
			if (sleepTime > MIN_INTERVAL_IN_MILLIS)
			{
//				Logger.getRootLogger().info("HistoricalDataLimitation: sleeping for " + sleepTime + " due to retrieval limitation");
			}
			Thread.sleep(sleepTime);
			
			rr.timestamp = System.currentTimeMillis();
			reqsInTenMin.addLast(rr);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace(System.err);
		}
		finally
		{
			lock.unlock();
		}
	}

	private void removeOldRecords()
	{
		long tenMinsAgo = System.currentTimeMillis() - (600 * 1000);
		boolean done = false;
		while (!done && reqsInTenMin.size() > 0)
		{
			RequestRecord req = reqsInTenMin.peekFirst();
			if (req.timestamp < tenMinsAgo)
			{
				reqsInTenMin.removeFirst();
			}
			else
			{
				done = true;
			}
		}
	}

	private long maxInTenMinsWait()
	{
		// The following conditions can cause a pacing violation:
		// * Making more than 60 historical data requests in any ten-minute period
		
		if (reqsInTenMin.size() < MAX_REQ_IN_TEN_MINS)
		{
			return 0;
		}
		else
		{
			return Math.max(0, 
					(TEN_MINS_IN_MILLIS) - (reqsInTenMin.getLast().timestamp - reqsInTenMin.getFirst().timestamp)); 
		}
	}

	private long similarReqWait(RequestRecord rr)
	{
		// The following conditions can cause a pacing violation:
		// * Making six or more historical data requests for the same Contract, Exchange and Tick Type within two seconds

		// TODO
		return 0L;
	}

	private long identicalReqWait(RequestRecord rr)
	{
		// The following conditions can cause a pacing violation:
		// * Making identical historical data request within 15 seconds
		
		Iterator<RequestRecord> iter = reqsInTenMin.descendingIterator();
		while (iter.hasNext())
		{
			RequestRecord req = iter.next();
			if (rr.conId == req.conId
					&& rr.exchange.equals(req.exchange)
					&& rr.tickType.equals(req.tickType)
					&& rr.duration.equals(req.duration)
					&& rr.endTime.equals(req.endTime))
			{
				return Math.max(0, 
						(15 * 1000) - (System.currentTimeMillis() - req.timestamp));
			}
		}
		return 0;
	}
}
