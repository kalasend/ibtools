package kyl.ib.util;

public final class DoubleMath
{
	private static final double round_base = 1e-4;
	private static final double epsilon = 1e-9;

	public static boolean ne(Double a, Double b)
	{
		return !eq(a, b);
	}
	
	public static boolean eq(Double a, Double b)
	{
		return Math.abs(a-b) < epsilon;
	}
	
	public static boolean lt(Double a, Double b)
	{
		return (b - a) > epsilon;
	}
	
	public static boolean gt(Double a, Double b)
	{
		return (a - b) > epsilon;
	}
	
	public static boolean le(Double a, Double b)
	{
		return !gt(a,b);
	}
	
	public static boolean ge(Double a, Double b)
	{
		return !lt(a,b);
	}
	
	public static int cmp(Double a, Double b)
	{
		if (lt(a,b))
			return -1;
		else if (eq(a,b))
			return 0;
        else
		    return 1;
	}
	
	public static double round(double x)
	{
		return round(x, round_base);
	}

	public static double round(double x, double base)
	{
		return Math.round(x / base) * base;
	}
}
