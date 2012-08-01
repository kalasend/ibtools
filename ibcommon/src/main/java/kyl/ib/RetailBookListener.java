package kyl.ib;


public interface RetailBookListener
{
    /*
     *  ticks should be a bit-or'ed to mark which tick has been updated
     */
    public void update(RetailBook book, long ticks);

    public void last(RetailBook book);
}
