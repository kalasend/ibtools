package kyl.ib;

import java.util.*;

public class MarketDataVisitor extends Thread
{
	private List<MarketDataTask> tasks = Collections.synchronizedList(new ArrayList<MarketDataTask>());
	
	private MarketDataVisitor()
	{
	}
	
	@Override
	public void run()
	{
		Thread.yield();
		
		// TODO
//		for (MarketDataTask task : tasks)
//		{
//		}
	}

	public void addTask(MarketDataTask myTask)
	{
        synchronized (tasks)
        {
		    tasks.add(myTask);
        }
	}

	public void removeTask(MarketDataTask task)
	{
        synchronized (tasks)
        {
    		tasks.remove(task);
        }
	}
}
