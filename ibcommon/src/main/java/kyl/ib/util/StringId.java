package kyl.ib.util;

import java.io.Serializable;

public class StringId implements Serializable, Comparable<StringId>, CharSequence 
{
//	private static final long serialVersionUID = 5466466782904098861L;
	private static final String ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int DEFAULT_ID_LENGTH = 10;
	
	private String id;
	
	public StringId(String s)
	{
		id = s;
	}
	
	public String toString()
	{
		return id;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof StringId)
		{
			return id.equals(((StringId) obj).toString());
		}
		else if (obj instanceof String)
		{
			return id.equals((String) obj);
		}
		return super.equals(obj);
	}
	
	public int compareTo(StringId o)
	{
		return id.compareTo(o.toString());
	}

	public static StringId generate()
	{
		return generate(null, DEFAULT_ID_LENGTH, null);
	}
	
	public static StringId generate(int len, String suffix)
	{
		return generate(null, len, suffix);
	}

	public static StringId generate(String prefix, int len)
	{
		return generate(prefix, len, null);
	}
	
	public static StringId generate(int len)
	{
		return generate(null, len, null);
	}
	
	public static StringId generate(String prefix, int len, String suffix)
	{
		len = Math.max(len, 4);
		
		StringBuffer sb = new StringBuffer();
		if (prefix != null)
		{
			sb.append(prefix);
		}
		
		for (int i = 0; i < len; i++)
		{
			int n = (int) Math.floor(Math.random() * (ALLOWED_CHARS.length()));
			sb.append(ALLOWED_CHARS.charAt(n));
		}
		
		if (suffix != null)
		{
			sb.append(suffix);
		}
		
		return new StringId(sb.toString());
	}
	
	public static StringId generate(CharSequence cs)
	{
		// example: "IB.####"
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < cs.length(); i++)
		{
			char c = cs.charAt(i); 
			if (c == '#')
			{
				c = ALLOWED_CHARS.charAt(
						(int) Math.floor(Math.random() * (ALLOWED_CHARS.length())));
			}
			sb.append(c);
		}
		return new StringId(sb.toString());
	}

	public static StringId parse(Object o)
	{
		if (o instanceof StringId)
		{
			return (StringId) o;
		}
		else
		{
			return new StringId(o.toString());
		}
	}

	@Override
	public int length()
	{
		return id.length();
	}

	@Override
	public char charAt(int index)
	{
		return id.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		return id.subSequence(start, end);
	}
	
	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
}
