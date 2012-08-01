package kyl.ib;

import org.apache.log4j.Logger;

import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Task
{
	public static final long MIN_TASK_LIFETIME = 10;
	
	// task states
	private TaskState theTaskState;
	
	IBTaskClient theClient = null;
	int theTaskId = -1;
	long theCreateTime;
	long theUpdateTime;
    Lock condLock;
    Condition stateCond;
	TwsError theError = null;
    Logger theLogger = Logger.getLogger(Task.class);
    private static final long ONE_DAY = 24 * 3600 * 1000;

    public Task(IBTaskClient client)
	{
		theUpdateTime = theCreateTime = System.currentTimeMillis();
		theClient = client;
		allocateTaskId();
		theTaskState = TaskState.CREATED;
        condLock = new ReentrantLock();
        stateCond = condLock.newCondition();
	}
	

	protected void allocateTaskId()
	{
		if (theTaskId != -1)
		{
			theClient.removeTask(theTaskId);
			theClient.freeId(theTaskId);
		}
		theTaskId = theClient.getNextId();
		theClient.addTask(theTaskId, this);
	}
	
	public boolean waitForState(TaskState aState, long timeout) 
		throws InterruptedException
	{
        theLogger.trace("Task.waitForState: start waiting for " + aState);
        Date endTime = new Date(
                System.currentTimeMillis() + (timeout <= 0 ? Long.MAX_VALUE : timeout));
        boolean notended = getTaskState() != aState;
        boolean notyet = System.currentTimeMillis() < endTime.getTime();
        theLogger.trace("Task.waitForState: notended=" + notended + " notyet=" + notyet);
        while (notended && notyet)
        {
            condLock.lock();
            try
            {
                stateCond.awaitUntil(endTime);
                notended = getTaskState() != aState;
                notyet = System.currentTimeMillis() < endTime.getTime();
                theLogger.trace("Task.waitForState: notended=" + notended + " notyet=" + notyet);
            }
            finally
            {
                condLock.unlock();
            }
        }
        theLogger.trace("Task.waitForState: Waiting ended. Current state=" + getTaskState());
		return getTaskState() == aState;
	}
	
	public void setTaskState(TaskState aState)
	{
        condLock.lock();
        try
        {
            theLogger.trace("Task.setTaskState: setting to " + aState);
            theTaskState = aState;
            stateCond.signalAll();
        }
        finally
        {
            condLock.unlock();
        }
	}
	
	public void waitForCompletion() 
		throws InterruptedException
	{
		waitForCompletion(ONE_DAY);
	}
	
	public void waitForCompletion(long timeout) 
		throws InterruptedException
	{
		waitForState(TaskState.ENDED, timeout);
	}

	public void signalCompletion()
	{
		setTaskState(TaskState.ENDED);
	}

	public TaskState getTaskState()
	{
		return theTaskState;
	}
	
	public boolean isComplete()
	{
		return theTaskState == TaskState.ENDED;
	}
	
	public abstract void start() throws Exception;
	
	public boolean hasError()
	{
		return theError != null;
	}

	public void error(TwsError err)
	{
		theError = err;
		signalCompletion();
	}
	
	public TwsError error()
	{
		return theError;
	}
}
