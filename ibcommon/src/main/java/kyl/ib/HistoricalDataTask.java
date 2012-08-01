package kyl.ib;

import java.util.*;

import org.apache.log4j.Logger;

import com.ib.client.Contract;

public final class HistoricalDataTask extends Task
{
	
	private static final long CODE_162_DELAY = 61 * 1000;
	private static final int CODE_162_MAX_RETRIES = 10;

	private static Timer timer = null;

	private Contract theContract;
	private String endDateTime;
	private String durationString;
	private String barSizeSetting;
	private String whatToShow;
	private int useRTH;
	private int formatDate;
	private int numRetries = 0;
	private long retryDelay = 0;


	
	public final class HistoricalDataBlock 
	{
		public String date;
		public double open;
		public double high;
		public double low;
		public double close;
		public long volume;
		public int count;
		public double wap;
		public boolean hasGaps;
	}
	
	private Vector<HistoricalDataBlock> vblocks;
	public HistoricalDataTask(IBTaskClient client, Contract contract,
            String endDateTime, String durationString,
            String barSizeSetting, String whatToShow,
            int useRTH, int formatDate) 
	{
		super(client);
		
		this.theContract = contract;
		this.endDateTime = endDateTime;
		this.durationString = durationString;
		this.barSizeSetting = barSizeSetting;
		this.whatToShow = whatToShow;
		this.useRTH = useRTH;
		this.formatDate = formatDate;

		vblocks = new Vector<HistoricalDataBlock>(250);
	}
	
	public void addData(String date, double open,
			double high, double low, double close, int volume, int count,
			double WAP, boolean hasGaps) 
	{
		if (date.startsWith("finished")) 
		{
			// no need to cancel the request if we get "finished" block
//			theClient.getEClient().cancelHistoricalData(theReqId);
			signalCompletion();
		}
		else
		{
			this.theUpdateTime = System.currentTimeMillis();
			HistoricalDataBlock blk = new HistoricalDataBlock();
			blk.date = date;
			blk.open = open;
			blk.high = high;
			blk.low = low;
			blk.close = close;
			blk.volume = volume;
			blk.count = count;
			blk.wap = WAP;
			blk.hasGaps = hasGaps;
			this.vblocks.add(blk);
		}
	}
	
	public Vector<HistoricalDataBlock> getData() 
	{
		return vblocks;
	}

	@Override
	public boolean hasError() 
	{
		return super.hasError();
	}

//	@Override
//	public boolean isComplete() 
//	{
//		return getTaskState() == TaskState.ENDED;
//	}

	@Override
	public void error(TwsError err) 
	{
		if (isErrorRecoverable(err))
		{
			if (numRetries > 0)
			{
				// retry after a while
				StringBuffer sb = new StringBuffer();
				sb.append(theContract.m_symbol).append(": Recoverable error received. Will retry in ")
				.append(retryDelay).append(" millis");
				Logger.getRootLogger().warn(sb.toString());

				if (timer == null)
				{
					timer = new Timer("HistoricalDataTask", true);
				}

				timer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						allocateTaskId();
						theClient.getEClient().reqHistoricalData(theTaskId, theContract, 
								endDateTime, durationString, barSizeSetting, whatToShow, useRTH, formatDate);
					}
				},
				retryDelay);

				numRetries--;
				return;
			}
		}
		else if (isErrorIgnorable(err))
		{
			// ignore this code
			return;
		}

		super.error(err);
	}

	private boolean isErrorIgnorable(TwsError err)
	{
		return err.getErrorCode() == 165;
	}

	private boolean isErrorRecoverable(TwsError err)
	{
		if (
				err.getErrorCode() == 162
				&& 
				(
						err.getMessage().contains("pacing violation") 
						||
						err.getMessage().contains("BEST queries are not supported")
				)
		)
		{
			numRetries = CODE_162_MAX_RETRIES;
			retryDelay = CODE_162_DELAY;
			return true;
		}
		
		return false;
	}

	@Override
	public void start() 
	{
		theClient.getHistoricalDataLimitation().waitInLine(
				theContract.m_conId, 
				theContract.m_exchange, 
				barSizeSetting, 
				durationString, 
				endDateTime);
		theClient.getEClient().reqHistoricalData(theTaskId, theContract, 
				endDateTime, durationString, barSizeSetting, whatToShow, useRTH, formatDate);
	}
}
