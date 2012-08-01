package kyl.ib;

import com.ib.client.*;
import kyl.ib.util.*;
import org.apache.log4j.*;

import java.util.*;

import static kyl.ib.ExposureManager.ExposureType.*;

public class ExposureManager
{
	private static final boolean testing = false;
	private static final IBTaskClient testClient = new IBTaskClient("", 0, 0);

    private IBTaskClient theClient;
	private Logger theLogger = Logger.getLogger(ExposureManager.class);
	private Contract theContract;
	private SerializableOrder theOrderTemplate;
	private List<ExposureListener> theListeners = Collections.synchronizedList(new Vector<ExposureListener>());
	private TreeSet<OrderTask> theOrdersCore = new TreeSet<OrderTask>(OrderTask.PriceTimeComparator);
	private SortedSet<OrderTask> theOrders = Collections.synchronizedSortedSet(theOrdersCore);
	private SortedSet<ExposureLevel> theDesired = Collections.synchronizedSortedSet(new TreeSet<ExposureLevel>(
			new Comparator<ExposureLevel>() {
				@Override
				public int compare(ExposureLevel e1, ExposureLevel e2)
				{
					if (DoubleMath.lt(e1.price, e2.price))
						return -1;
					else if (DoubleMath.gt(e1.price, e2.price))
						return 1;
					else
						return 0;
				}
			}));

	// order reference
	private StringBuffer theOrderRef = new StringBuffer("EM");
    private boolean useOca = false;
    private String ocaGroupname = null;

    public void setOca(boolean b)
    {
        if (b)
        {
            if (!useOca)
            {
                useOca = b;
                ocaGroupname = StringId.generate().toString();
            }
        }
    }

    private class ExposureLevel
	{
		public long qty;
		public double price;
		public ExposureType type;
		
		public ExposureLevel(double p, long q, ExposureType t)
		{
			qty = q;
			price = p;
			type = t;
		}
		
		public String toString()
		{
			StringBuffer sb = newLogBuffer();
			sb.append(type).append(' ').append(qty).append('@').append(price);
			return sb.toString();
		}
	}

	private class OrderChange
	{
		OrderTask orderTask;
		double price;
		long qty;
		
		public OrderChange(OrderTask ot, double p, long q)
		{
			orderTask = ot;
			price = p;
			qty = q;
		}

		public boolean sizeDownOnly()
		{
			return (DoubleMath.eq(orderTask.getPrice(), price) && orderTask.getRemaining() > qty);
		}
		
		public String toString()
		{
			StringBuffer sb = newLogBuffer();
			sb.append("orderTask={").append(orderTask.toString()).append("} newPrice=").append(price).append(" newQty=").append(qty);
			return sb.toString();
		}
	}
	
	public enum ExposureType
	{
		BUY,
		SELL,
		SSHORT;

		public static ExposureType parseType(String m_action)
		{
			if (m_action.equalsIgnoreCase("BUY"))
				return BUY;
			else if (m_action.equalsIgnoreCase("SELL"))
				return SELL;
			else if (m_action.equalsIgnoreCase("SSHORT"))
				return SSHORT;
			else
				return null;
		}

		public static boolean sameSide(ExposureType orderExpoType,
				ExposureType type)
		{
			return ((orderExpoType == BUY && type == BUY)
					|| (orderExpoType != BUY && type != BUY));
		}
	}
	
	public class OrderListener implements OrderStatusListener
	{
		OrderTask order;
		StringBuffer sb = newLogBuffer();
		
		public OrderListener(OrderTask ot)
		{
			order = ot;
		}

		@Override
		public void update(long time, String status, double lastPrice,
				long prevFilled, long curFilled, long prevRemain, long curRemain)
		{
			sb.delete(0, sb.length());
			sb.append("{").append(order).append("} status=").append(status).append(" filled=").append(curFilled)
				.append(" rem=").append(curRemain);
			
			if (status.equalsIgnoreCase("Filled") || status.equalsIgnoreCase("Cancelled"))
			{
				sb.append("\n status=").append(status).append(" order will be removed");
				synchronized (theOrders)
				{
					theOrders.remove(order);
				}
			}

			if (curFilled > prevFilled)
			{
				sb.append("\n prevFilled=").append(prevFilled).append(" curFilled=").append(curFilled);
				notifyTrade(ExposureType.parseType(order.getOrder().m_action), curFilled - prevFilled, lastPrice, time);
			}
			theLogger.info(sb);
		}
	}

	public ExposureManager(IBTaskClient client, Contract cont, SerializableOrder temp)
	{
        theClient = client;
		theContract = cont;
		theOrderTemplate = temp;
	}
	
	public void notifyTrade(ExposureType etype, long vol, double price, long time)
	{
		for (ExposureListener lis : theListeners)
		{
			lis.traded(etype == BUY ? TradeType.BOUGHT : TradeType.SOLD,
					vol, price, time);
		}
	}

	public void addListener(ExposureListener lis)
	{
		synchronized (theListeners)
		{
			theListeners.add(lis);
		}
	}
	
	public void clearAll()
	{
		synchronized (theDesired)
		{
			theDesired.clear();
		}
	}
	
	public void clearSide(ExposureType clearType)
	{
		synchronized (theDesired)
		{
			Iterator<ExposureLevel> iter = theDesired.iterator();
			while (iter.hasNext())
			{
				ExposureLevel el = iter.next();
				if (ExposureType.sameSide(el.type, clearType))
				{
					iter.remove();
				}
			}
		}
	}
	
	public void addExposure(double price, long qty, ExposureType type)
	{
        if (Double.isInfinite(price) || Double.isNaN(price) || qty <= 0)
        {
            theLogger.debug("Ignoring invalid price or size: " + price + " " + qty);
            return;
        }

		synchronized (theDesired)
		{
			for (ExposureLevel el : theDesired) 
			{
				if (DoubleMath.eq(el.price, price) && el.type == type)
				{
					el.qty += qty;
					return;
				}
			}
			theDesired.add(new ExposureLevel(price, qty, type));
		}
	}

	public void reconcile(boolean actOnPlan) throws Exception
	{
		synchronized (theOrders)
		{
			StringBuffer sb = newLogBuffer().append("desired exposures\n").append(dumpDesired());
			theLogger.debug(sb);
			
			// scan for current orders that should be canceled; save results(cancels)
			Vector<OrderTask> cancels = new Vector<OrderTask>();
			scanForCancels(cancels);
			
			// compare desired levels against current orders; save results(creates and modifys)
			SortedSet<OrderChange> modifys = new TreeSet<OrderChange>(getOrderChangeComparator());
			Vector<OrderTask> creates = new Vector<OrderTask>();
			processDesired(creates, cancels, modifys);

			theLogger.debug(resetLogBuffer(sb).append("action plan:\n").append(dumpPlans(creates, cancels, modifys)));
			if (actOnPlan)
			{
				takeActions(creates, cancels, modifys);
			}
			else
			{
				takeFakeActions(creates, cancels, modifys);
			}
		}
	}

	private String dumpDesired()
	{
		StringBuffer sb = new StringBuffer();
		
		for (ExposureLevel el : theDesired)
		{
			sb.append(el).append("\n");
		}
		return sb.toString();
	}

	private Comparator<? super OrderChange> getOrderChangeComparator()
	{
		return new Comparator<OrderChange>() {
			@Override
			public int compare(OrderChange o1, OrderChange o2)
			{
				theLogger.debug("comparing: o1={" + o1 + "} o2={" + o2 + "}");
				
				// most important of all is to detect "crossable" orders after modifying(
				// e.g. buy 11 and sell 10
				ExposureType et1 = ExposureType.parseType(o1.orderTask.getOrder().m_action);
				ExposureType et2 = ExposureType.parseType(o2.orderTask.getOrder().m_action);
				
				if (!isOpposing(et1, et2))
				{
					theLogger.debug("comparing: same side, no change");
					return -1;
				}
				else if (et1 == BUY && wouldCross(o1.price, o2.orderTask.getPrice()))
				{
					theLogger.debug("comparing: o1 is buying and would cross: -1");
					return 1;
				}
				else if (et1 != BUY && wouldCross(o2.orderTask.getPrice(), o1.price))
				{
					theLogger.debug("comparing: o2 is buying and would cross: 1");
					return 1;
				}
				else
				{
					theLogger.debug("comparing: no crossing, no change");
					return -1;
				}
			}

			private boolean wouldCross(double buyPrice, double sellPrice)
			{
				boolean retval = DoubleMath.ge(buyPrice, sellPrice);
				return retval;
			}

			private boolean isOpposing(ExposureType et1, ExposureType et2)
			{
				return 
				(et1 == BUY && et2 != BUY)
					|| (et2 == BUY && et1 != BUY);
			}
		};
	}

	private void sortOrders()
	{
		Vector<OrderTask> temp = new Vector<OrderTask>();
		for (OrderTask ot : theOrders)
		{
			temp.add(ot);
		}
		theOrders.clear();
		theOrders.addAll(temp);
	}

	private void processDesired(List<OrderTask> creates, List<OrderTask> cancels, SortedSet<OrderChange> modifys) throws Exception
	{
		StringBuffer sb = newLogBuffer();
		for (ExposureLevel el : theDesired)
		{
			if (testing)
				sb.append("processDesired: ExposureLevel ").append(el).append("\n");
			
			long qtyToFill = el.qty;
			
			// scan current orders to see how much of desired qty they can fulfill
			for (OrderTask ot : theOrders)
			{
				if (!cancels.contains(ot) 
						&& orderMatchesExposureLevel(ot, el)
						&& !toBeModified(ot, modifys))
				{
					if (testing)
						sb.append("processDesired: looking at OrderTask ").append(ot).append("\n");
					
					if (qtyToFill <= 0)
					{
						if (testing)
							sb.append("processDesired: will cancel OrderTask ").append(ot).append("\n");
						cancels.add(ot);
					}
					else if (ot.getRemaining() > qtyToFill)
					{
						OrderChange oc = new OrderChange(ot, ot.getPrice(), qtyToFill); 
						modifys.add(oc);
						qtyToFill = 0;
						if (testing)
							sb.append("processDesired: will modify ").append(oc).append("\n");
					}
					else
					{
						qtyToFill -= ot.getRemaining();
					}
					if (testing)
						sb.append("processDesired: qty2Fill=").append(qtyToFill).append("\n");
				}
			}
			if (testing)
				sb.append("processDesired: Done scanning orders, qty2Fill=").append(qtyToFill).append("\n");
			
			// if all current orders(at price) cannot fulfill, we want to size up
			if (qtyToFill > 0)
			{
				OrderTask ot = selectFromCancels(cancels, el.type);
				if (ot != null)
				{
					cancels.remove(ot);
					OrderChange oc = new OrderChange(ot, el.price, qtyToFill); 
					modifys.add(oc);
					if (testing)
						sb.append("processDesired: will modify ").append(oc).append("\n");
				}
				// TODO : there needs to be a limit on max number of orders per price level
				// i.e.:
				// if (numOrdersAtPrice(el.price) > getMaxOrdersPerLevel()) { ... }
				else
				{
                    ot = createOrder(el, qtyToFill);
                    creates.add(ot);
					if (testing)
						sb.append("processDesired: will create ").append(ot).append("\n");
				}
			}
		}
		if (sb.length() > 0)
		{
			theLogger.debug(sb);
		}
	}

    private OrderTask createOrder(ExposureLevel el, long qtyToFill)
    {
        OrderTask ot = null;
        if (testing)
        {
            ot = new OrderTask(testClient, theContract);
        }
        else
        {
            ot = new OrderTask(theClient, theContract);
            ot.addOrderStatusListener(new OrderListener(ot));
            if (theOrderTemplate != null)
            {
                theOrderTemplate.mirrorTo(ot.getOrder());
            }
        }
        StringBuffer oref = new StringBuffer(
                ot.getOrder().m_orderRef != null ?
                        ot.getOrder().m_orderRef :
                        "");
        if (oref.length() > 0)
        {
            oref.append(';');
        }
        oref.append(getOrderRef());

        if (useOca && ocaGroupname != null)
        {
            ot.getOrder().m_ocaGroup = ocaGroupname;
            if (theContract.m_secType.equals("BAG"))
            {
                ot.getOrder().m_ocaType = 3;
            }
            else
            {
                ot.getOrder().m_ocaType = 1;
            }
        }

        ot.getOrder().m_orderRef.concat(oref.toString());
        ot.getOrder().m_orderType = "LMT";
        ot.getOrder().m_totalQuantity = (int) qtyToFill;
        ot.getOrder().m_lmtPrice = el.price;
        ot.getOrder().m_action = el.type.name();
        return ot;
    }

    private String getOrderRef()
	{
		return theOrderRef.toString();
	}
	
	public void addToOrderRef(String oref)
	{
		if (theOrderRef.length() > 0)
		{
			theOrderRef.append(';');
		}
		theOrderRef.append(oref);
	}

	private boolean toBeModified(OrderTask ot, SortedSet<OrderChange> modifys)
	{
		for (OrderChange oc : modifys)
		{
			if (ot.equals(oc.orderTask))
			{
				return true;
			}
		}
		return false;
	}

	private OrderTask selectFromCancels(List<OrderTask> cancels, ExposureType type)
	{
		for (OrderTask ot : cancels)
		{
			if (ExposureType.parseType(ot.getOrder().m_action) == type)
			{
				return ot;
			}
		}
		return null;
	}

	private void scanForCancels(Vector<OrderTask> cancels)
	{
		for (OrderTask ot : theOrders)
		{
			ExposureLevel el = findByPrice(ot.getPrice(), theDesired);
			if (el == null)
			{
				cancels.add(ot);
			}
			else if (!orderMatchesExposureLevel(ot, el))
			{
				cancels.add(ot);
			}
		}
	}

	private boolean orderMatchesExposureLevel(OrderTask ot, ExposureLevel el)
	{
		ExposureType orderExpoType = ExposureType.parseType(ot.getOrder().m_action);
		if (DoubleMath.eq(ot.getPrice(), el.price)
				&& ExposureType.sameSide(orderExpoType, el.type))
		{
			return true;
		}
		return false;
	}

	private ExposureLevel findByPrice(double price, SortedSet<ExposureLevel> levels)
	{
		for (ExposureLevel el : levels)
		{
			if (DoubleMath.eq(price, el.price))
				return el;
		}
		return null;
	}
	
	public void takeActions(Vector<OrderTask> creates, Vector<OrderTask> cancels, SortedSet<OrderChange> modifys)
	{
		if (testing)
		{
			takeFakeActions(creates, cancels, modifys);
		}
		else
		{
			for (OrderTask ot : cancels)
			{
				ot.cancel();
			}
			for (OrderChange oc : modifys)
			{
				long qtydiff = oc.orderTask.getRemaining() - oc.qty;
				oc.orderTask.change(
						oc.price, 
						oc.orderTask.getOrder().m_totalQuantity - qtydiff);
			}
			for (OrderTask ot : creates)
			{
				theOrders.add(ot);
				ot.start();
			}
		}
	}

	private void takeFakeActions(Vector<OrderTask> creates,
			Vector<OrderTask> cancels, SortedSet<OrderChange> modifys)
	{
		for (OrderTask ot : cancels)
		{
			theOrders.remove(ot);
		}
		for (OrderChange oc : modifys)
		{
			theOrders.remove(oc.orderTask);
			if (!oc.sizeDownOnly())
			{
				oc.orderTask.markSentTime();
			}
			oc.orderTask.getOrder().m_totalQuantity = (int) oc.qty;
			oc.orderTask.getOrder().m_lmtPrice = oc.price;
			theOrders.add(oc.orderTask);
		}
		for (OrderTask ot : creates)
		{
			ot.setRemaining(ot.getOrder().m_totalQuantity);
			ot.markSentTime();
			theOrders.add(ot);
		}
	}

	public void dumpOrders()
	{
		System.out.println("Dumping " + theOrders.size() + " orders:");
		for (OrderTask ot : theOrders)
		{
			System.out.println(ot);
		}
	}
	
	public String dumpPlans(Vector<OrderTask> creates, Vector<OrderTask> cancels, SortedSet<OrderChange> modifys)
	{
		StringBuffer sb = newLogBuffer().append("\n");
		for (OrderTask ot : cancels)
		{
			sb.append("cancel: ").append(ot).append("\n");
		}
		for (OrderChange oc : modifys)
		{
			sb.append("modify: ").append(oc).append("\n");
		}
		for (OrderTask ot : creates)
		{
			sb.append("create: ").append(ot).append("\n");
		}
		return sb.toString();
	}

	public long getTotalExposureAt(double price, ExposureType type)
	{
		synchronized (theOrders)
		{
			long totalExposure = 0;
			for (OrderTask ot : theOrders)
			{
				if (DoubleMath.eq(ot.getPrice(), price)
						&& ExposureType.sameSide(ExposureType.parseType(ot.getOrder().m_action), type))
				{
					totalExposure += ot.getRemaining();
				}
			}
			return totalExposure;
		}
	}

	public boolean hasBuyExposure()
	{
		synchronized (theOrders)
		{
			for (OrderTask ot : theOrders)
			{
				if (ExposureType.sameSide(ExposureType.parseType(ot.getOrder().m_action), BUY))
				{
					return true;
				}
			}
			return false;
		}
	}

	public boolean hasSellExposure()
	{
		synchronized (theOrders)
		{
			for (OrderTask ot : theOrders)
			{
				if (ExposureType.sameSide(ExposureType.parseType(ot.getOrder().m_action), SELL))
				{
					return true;
				}
			}
			return false;
		}
	}

	public double getBestBid()
	{
		synchronized (theOrders)
		{
			Iterator<OrderTask> iter = theOrdersCore.descendingIterator();
			while (iter.hasNext())
			{
				OrderTask ot = iter.next();
				if (ExposureType.sameSide(
                        ExposureType.parseType(ot.getOrder().m_action),
                        BUY))
				{
					return ot.getPrice();
				}
			}
			return Double.NaN;
		}
	}

	public double getBestOffer()
	{
		synchronized (theOrders)
		{
			Iterator<OrderTask> iter = theOrdersCore.iterator();
			while (iter.hasNext())
			{
				OrderTask ot = iter.next();
				if (ExposureType.sameSide(
                        ExposureType.parseType(ot.getOrder().m_action),
                        SELL))
				{
					return ot.getPrice();
				}
			}
			return Double.NaN;
		}
	}

	public void manage(OrderTask ot)
	{
		theOrders.add(ot);
	}
	
	public StringBuffer newLogBuffer()
	{
		StringBuffer sb = new StringBuffer("EM{");
        ContractLibrary.summarizeContract(sb, theContract);
        return sb.append("}: ");
	}
	
	public StringBuffer resetLogBuffer(StringBuffer sb)
	{
		sb.delete(0, sb.length());
        sb.append("EM{");
        ContractLibrary.summarizeContract(sb, theContract);
        return sb.append("}: ");
	}
}
