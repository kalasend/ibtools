package kyl.ib;

public class TickType extends com.ib.client.TickType
{
    public static final int BID_INSERT = 201;
    public static final int BID_REMOVE = 202;
    public static final int BID_UPDATE = 203;
    public static final int ASK_INSERT = 204;
    public static final int ASK_REMOVE = 205;
    public static final int ASK_UPDATE = 206;
    public static final int SNAPSHOT = 207;

    public static String getField(int t)
    {
        switch (t)
        {
        case BID_INSERT:
            return "bid_insert";
        case BID_REMOVE:
            return "bid_remove";
        case BID_UPDATE:
            return "bid_update";
        case ASK_INSERT:
            return "ask_insert";
        case ASK_REMOVE:
            return "ask_remove";
        case ASK_UPDATE:
            return "ask_update";
        case SNAPSHOT:
            return "snapshot";
        default:
            return com.ib.client.TickType.getField(t);
        }
    }
}
