package kyl.ib;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import org.apache.log4j.Logger;

import java.util.Vector;

public final class ContractDetailsTask extends Task
{
	
	private static Logger theLogger = Logger.getLogger(ContractDetailsTask.class);
	
	public Vector<ContractDetails> details = null;
	protected Contract scanContract = null;
	private String errMsg = null;
	
	// temp solution: detailsEnd called before receiving details
	private boolean isEarlyEnd = false;
	private TwsError termError = null;
	
	
	
	public ContractDetailsTask(IBTaskClient client, Contract contract)
	{
		super(client);
		details = new Vector<ContractDetails>();
		scanContract = contract;
	}


	@Override
	public void start() 
	{
		this.setTaskState(TaskState.STARTED);
        theLogger.trace("ContractDetailsTask: sending request...");
		theClient.getEClient().reqContractDetails(theTaskId, scanContract);
	}

	public void addDetails(ContractDetails contractDetails) 
	{
		theLogger.trace("ContractDetailsTask: " + System.currentTimeMillis() + 
				" got contract details for reqId=" + theTaskId + "  conId=" + contractDetails.m_summary.m_conId);
		details.add(contractDetails);
		if (isEarlyEnd)
		{
            setTaskState(TaskState.ENDED);
		}
	}
	
	public void detailsEnd() 
	{
		theLogger.trace("ContractDetailsTask: " + System.currentTimeMillis() + " contract details ended for reqId=" + theTaskId);
		
		if (details.size() > 0)
		{
			// something was returned before, end task gracefully
            setTaskState(TaskState.ENDED);
		}
		else
		{
			isEarlyEnd = true;
			theLogger.trace("ContractDetailsTask: reqId=" + theTaskId + " isEarlyEnd=" + isEarlyEnd);
		}
	}
	
	@Override
	public boolean hasError() 
	{
		return termError == null;
	}

//	@Override
//	public boolean isComplete() 
//	{
//		return getTaskState() == TaskState.ENDED;
//	}

	@Override
	public void error(TwsError err) 
	{
		termError = err;
		signalCompletion();
	}

}
