package kyl.ib;

/*
 * 
 * First design. Keeping it simple.
 * Only one entry point and handler is supposed to check trade state change
 * 
 */

public interface MarketDataHandler 
{
	public void marketUpdate(RetailBook book, int tick);
	public void lastDone(RetailBook book, int tick);
}
