package kyl.ib;

public interface OrderStatusListener
{
	public void update(long time, String status, double lastPrice, long prevFilled, long curFilled, long prevRemain, long curRemain);
}
