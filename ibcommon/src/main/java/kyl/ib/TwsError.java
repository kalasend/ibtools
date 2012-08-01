package kyl.ib;

public class TwsError 
{
	private int id;
	private int errorCode;
	private String message;
	private long timestamp;
	
	public TwsError(int id, int err, String msg)
	{
		timestamp = System.currentTimeMillis(); 
		this.id = id;
		this.errorCode = err;
		this.message = msg;
	}

	public int getId() {
		return id;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		return sb.append("TwsError: id=").append(id).append(" code=").append(errorCode)
			.append(" text=\"").append(message).append("\"").toString();
	}
}
