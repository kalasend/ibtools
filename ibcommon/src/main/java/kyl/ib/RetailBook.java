package kyl.ib;


import java.text.*;
import java.util.*;

import static kyl.ib.RetailBook.DATA_SLOT.*;

public class RetailBook
{
    protected static final int NUM_LEVELS = 10;

    protected static enum DATA_SLOT {
        BID1, BID2, BID3, BID4, BID5, BID6, BID7, BID8, BID9, BID10,
        ASK1, ASK2, ASK3, ASK4, ASK5, ASK6, ASK7, ASK8, ASK9, ASK10,
        LAST, OPEN, HIGH, LOW, CLOSE, MPV, VOLUME, LASTTIME, TIMESTAMP, UPDATE_COUNT
    };

    private double doubles[];
    private long longs[];
    private RetailBookState retailState = RetailBookState.NORMAL;
    private List<RetailBookListener> listeners = null;

    public RetailBook()
    {
        this(0.01);
    }

    public RetailBook(double mpv)
    {
        doubles = new double[values().length];
        Arrays.fill(doubles, Double.NaN);
        longs = new long[values().length];
        Arrays.fill(longs, -1);
        longs[UPDATE_COUNT.ordinal()] = 0;
        setMpv(mpv);
    }

    public double[] getDoubles()
    {
        return doubles;
    }

    public long[] getLongs()
    {
        return longs;
    }

    public double bid()
    {
        return doubles[BID1.ordinal()];
    }

    public double bid(int i)
    {
        return doubles[BID1.ordinal() + i];
    }

    public synchronized void setBid(double x, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        doubles[BID1.ordinal()] = x;
    }

    public synchronized void setBid(int i , double x, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        doubles[BID1.ordinal() + i] = x;
    }

    public double ask()
    {
        return doubles[ASK1.ordinal()];
    }

    public double ask(int i)
    {
        return doubles[ASK1.ordinal() + i];
    }

    public synchronized void setAsk(double x, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        doubles[ASK1.ordinal()] = x;

    }

    public synchronized void setAsk(int i, double x, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        doubles[ASK1.ordinal() + i] = x;
    }

    public long bidSize()
    {
        return longs[BID1.ordinal()];
    }

    public long bidSize(int i)
    {
        return longs[BID1.ordinal() + i];
    }

    public synchronized void setBidSize(long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        longs[BID1.ordinal()] = n;
    }

    public synchronized void setBidSize(int i, long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        longs[BID1.ordinal() + i] = n;
    }

    public long askSize()
    {
        return longs[ASK1.ordinal()];
    }

    public long askSize(int i)
    {
        return longs[ASK1.ordinal() + i];
    }

    public synchronized void setAskSize(long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        longs[ASK1.ordinal()] = n;
    }

    public synchronized void setAskSize(int i, long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        longs[ASK1.ordinal() + i] = n;
    }

    public long timestamp()
    {
        return longs[TIMESTAMP.ordinal()];
    }

    public synchronized void setTimestamp(long t)
    {
        longs[TIMESTAMP.ordinal()] = t;
    }

    public long lastTime()
    {
        return longs[LASTTIME.ordinal()];
    }

    public double last()
    {
        return doubles[LAST.ordinal()];
    }

    public synchronized void setLast(double x, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[LASTTIME.ordinal()] = t;
        doubles[LAST.ordinal()] = x;
    }

    public long lastSize()
    {
        return longs[LAST.ordinal()];
    }

    public synchronized void setLastSize(long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[LASTTIME.ordinal()] = t;
        longs[LAST.ordinal()] = n;
    }

    public long volume()
    {
        return longs[VOLUME.ordinal()];
    }

    public synchronized void setVolume(long v, long t)
    {
        longs[TIMESTAMP.ordinal()] = t;
        longs[VOLUME.ordinal()] = v;
    }

    public double open()
    {
        return doubles[OPEN.ordinal()];
    }

    public synchronized void open(double x)
    {
        doubles[OPEN.ordinal()] = x;
    }

    public double high()
    {
        return doubles[HIGH.ordinal()];
    }

    public synchronized void high(double x)
    {
        doubles[HIGH.ordinal()] = x;
    }

    public double low()
    {
        return doubles[LOW.ordinal()];
    }

    public synchronized void low(double x)
    {
        doubles[LOW.ordinal()] = x;
    }

    public double close()
    {
        return doubles[CLOSE.ordinal()];
    }

    public synchronized void close(double x)
    {
        doubles[CLOSE.ordinal()] = x;
    }

    public synchronized void dumpTo(RetailBook sink)
    {
        System.arraycopy(doubles, 0, sink.getDoubles(), 0, values().length);
        System.arraycopy(longs, 0, sink.getLongs(), 0, values().length);
    }

    public synchronized void copyFrom(RetailBook source)
    {
        System.arraycopy(source.getDoubles(), 0, doubles, 0, values().length);
        System.arraycopy(source.getLongs(), 0, longs, 0, values().length);
    }

    public void insertBid( int i, double x, long n, long t)
    {
        insertLevel(BID1.ordinal(), i, x, n, t);
    }

    public void insertAsk(int i, double x, long n, long t)
    {
        insertLevel(ASK1.ordinal(), i, x, n, t);
    }
    
    private synchronized void insertLevel(int side, int i, double x, long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        System.arraycopy(
                doubles, side + i,
                doubles, side + i + 1,
                NUM_LEVELS - i);
        System.arraycopy(
                longs, side + i,
                longs, side + i + 1,
                NUM_LEVELS - i);
        doubles[side + i] = x;
        longs[side + i] = n;
    }

    public void removeBid(int i, long t)
    {
        removeLevel(BID1.ordinal(), i, t);
    }

    public void removeAsk(int i, long t)
    {
        removeLevel(ASK1.ordinal(), i, t);
    }
    
    private synchronized void removeLevel(int side, int i, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        System.arraycopy(
                doubles, side + i + 1,
                doubles, side + i,
                NUM_LEVELS - i);
        System.arraycopy(
                longs, side + i + 1,
                longs, side + i,
                NUM_LEVELS - i);
        doubles[side + NUM_LEVELS - 1] = Double.NaN;
        longs[side + NUM_LEVELS - 1] = -1;
    }

    public void updateBid(int i, double x, long n, long t)
    {
        updateLevel(BID1.ordinal(), i, x, n, t);
    }

    public void updateAsk(int i, double x, long n, long t)
    {
        updateLevel(ASK1.ordinal(), i, x, n, t);
    }

    private synchronized void updateLevel(int side, int i, double x, long n, long t)
    {
        longs[UPDATE_COUNT.ordinal()]++;
        longs[TIMESTAMP.ordinal()] = t;
        doubles[side + i] = x;
        longs[side + i] = n;
    }

    public void setState(RetailBookState st)
    {
        retailState = st;
    }

    public RetailBookState getState()
    {
        return retailState;
    }

    public StringBuffer appendLevel1(StringBuffer sb)
    {
        return sb.append(doubles[BID1.ordinal()]).append('(').append(longs[BID1.ordinal()]).
                append("):").append(doubles[ASK1.ordinal()]).append('(').append(longs[ASK1.ordinal()]).append(')');
    }

    public StringBuffer appendLastdone(StringBuffer sb)
    {
        final DateFormat fmt = new SimpleDateFormat("hh:mm:ss.SSS");
        return sb.append(longs[LAST.ordinal()]).append('@').append(doubles[LAST.ordinal()])
                .append(" at ").append(fmt.format(new Date(longs[LASTTIME.ordinal()])));
    }

    public double getMpv()
    {
        return doubles[MPV.ordinal()];
    }

    public void setMpv(double mpv)
    {
        doubles[MPV.ordinal()] = mpv;
    }

    public StringBuffer selfDescribe(StringBuffer sb, int i)
    {
        // TODO: use i as number of levels to describe
        sb.append("{bsize=").append(bidSize()).append(" bid=").append(bid()).append(" ask=").append(ask())
                .append(" asize=").append(askSize()).append("}");
        return sb;
    }

    public boolean isL1Present()
    {
        return (!(Double.isNaN(doubles[BID1.ordinal()])
                        || Double.isNaN(doubles[ASK1.ordinal()])
                        || longs[BID1.ordinal()] < 0
                        || longs[ASK1.ordinal()] < 0));
    }

    public void addListener(RetailBookListener lis)
    {
        if (!getListeners().contains(lis))
        {
            getListeners().add(lis);
        }
    }

    public void removeListener(RetailBookListener lis)
    {
        getListeners().remove(lis);
    }

    private List<RetailBookListener> getListeners()
    {
        if (listeners == null)
        {
            listeners = Collections.synchronizedList(new Vector<RetailBookListener>());
        }
        return listeners;
    }

    public void broadcastUpdate(long ticks)
    {
        for (RetailBookListener lis : getListeners())
        {
            lis.update(this, ticks);
        }
    }

    public void broadcastLast()
    {
        for (RetailBookListener lis : getListeners())
        {
            lis.last(this);
        }
    }
}
