package kyl.ib;

import com.ib.client.*;
import kyl.ib.util.*;
import java.util.*;

public class OrderTask extends Task
{
	private Contract theContract;
	private Order theOrder;
	private long theSentTime = 0;
	private long theUpdateTime = 0;
	private String status = null;
	private long filled = 0;
	private long remaining = 0;
	private double avgFillPrice = Double.NaN;
	private double lastFillPrice = Double.NaN;
	private List<OrderStatusListener> theListeners = Collections.synchronizedList(new Vector<OrderStatusListener>());

	public static final Comparator<OrderTask> PriceTimeComparator = new Comparator<OrderTask>() {
		@Override
		public int compare(OrderTask o1, OrderTask o2)
		{
			if (DoubleMath.gt(o1.getPrice(), o2.getPrice()))
			{
				return -1;
			}
			else if (DoubleMath.lt(o1.getPrice(), o2.getPrice()))
			{
				return 1;
			}
			else if (o1.getSentTime() < o2.getSentTime())
			{
				return -1;
			}
			else if (o1.getSentTime() > o2.getSentTime())
			{
				return 1;
			}
			else
			{
				return 0;
			}
		}
	};

//	public OrderTask(IBTaskClient client, Contract cont, Order order)
//	{
//		this(cont, client);
//		theOrder = order;
//	}
	
	public OrderTask(IBTaskClient client, Contract cont)
	{
		super(client);
		theContract = cont;
		theOrder = new Order();
		theOrder.m_orderRef = "OT";
		theOrder.m_orderId = theTaskId;
	}

	public long getSentTime()
	{
		return theSentTime;
	}

	public long getUpdateTime()
	{
		return theUpdateTime;
	}
	
	public double getPrice()
	{
		return theOrder.m_lmtPrice;
	}

	@Override
	public void start()
	{
		remaining = theOrder.m_totalQuantity;
		theSentTime = System.nanoTime();
		theClient.getEClient().placeOrder(theTaskId, theContract, theOrder);
	}

    public void setOrder(Order o)
    {
        theOrder = o;
        theOrder.m_orderRef = "OT:" + theOrder.m_orderId + ";" + theOrder.m_orderRef;
    }

	public Order getOrder()
	{
        if (theOrder == null)
        {
            theOrder = new Order();
            theOrder.m_orderRef = "OT:" + theOrder.m_orderId;
//            theOrder.m_orderId = theTaskId;
        }
		return theOrder;
	}

	public void orderStatus(String status, int filled, int remaining,
			double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld)
	{
		theUpdateTime = System.nanoTime();
		notifyListeners(theUpdateTime / 1000, status, lastFillPrice, this.filled, filled, this.remaining, remaining);
		this.status = status;
		this.filled = filled;
		this.remaining = remaining;
		this.avgFillPrice = avgFillPrice;
		this.lastFillPrice = lastFillPrice;
		
		if (this.remaining <= 0)
		{
			signalCompletion();
		}
	}
	
	private void notifyListeners(long time, String status, double price, 
			long prevFilled, long curFilled, long prevRemain, long curRemain)
	{
		synchronized (theListeners)
		{
			for (OrderStatusListener lis : theListeners)
			{
				lis.update(time, status, price, prevFilled, curFilled, prevRemain, curRemain);
			}
		}
	}

	public void addOrderStatusListener(OrderStatusListener lis)
	{
		synchronized (theListeners)
		{
			theListeners.add(lis);
		}
	}
	
	public long getRemaining()
	{
		return remaining;
	}

	public void setRemaining(long rem)
	{
		remaining = rem;
	}

	public void cancel()
	{
		theClient.getEClient().cancelOrder(theTaskId);
	}

	public void change(double price, long qty)
	{
		theOrder.m_lmtPrice = price;
		theOrder.m_totalQuantity = (int) qty;
		theUpdateTime = System.nanoTime();
		theClient.getEClient().placeOrder(theTaskId, theContract, theOrder);
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(id()).append(' ').append(theContract).append(" ")
			.append(theOrder.m_action).append(' ').append(theOrder.m_totalQuantity).append('@')
			.append(theOrder.m_lmtPrice);
		return sb.toString();
	}

	public Contract getContract()
	{
		return theContract;
	}
	
	public int id()
	{
		return theOrder.m_orderId;
	}
	
	public String status()
	{
		return status;
	}

	public void markSentTime()
	{
		theSentTime = System.nanoTime();
	}
}
