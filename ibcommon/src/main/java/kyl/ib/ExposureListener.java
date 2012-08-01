package kyl.ib;

public interface ExposureListener
{
//	public void exposureUpdate();
	public void traded(TradeType ttype, long vol, double price, long time);
}
