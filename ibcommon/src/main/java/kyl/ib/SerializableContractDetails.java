package kyl.ib;


import com.ib.client.*;
import kyl.ib.util.StringId;

import java.io.Serializable;
import java.util.Iterator;

public class SerializableContractDetails implements Serializable
{
    public StringId id;

    public SerializableContract m_summary;
    public String 	m_marketName;
    public String 	m_tradingClass;
    public double 	m_minTick;
    public int      m_priceMagnifier;
    public String 	m_orderTypes;
    public String 	m_validExchanges;
    public int      m_underConId;
    public String 	m_longName;
    public String	m_contractMonth;
    public String	m_industry;
    public String	m_category;
    public String	m_subcategory;
    public String	m_timeZoneId;
    public String	m_tradingHours;
    public String	m_liquidHours;

    // BOND values
    public String 	m_cusip;
    public String 	m_ratings;
    public String 	m_descAppend;
    public String 	m_bondType;
    public String 	m_couponType;
    public boolean 	m_callable			= false;
    public boolean 	m_putable			= false;
    public double 	m_coupon			= 0;
    public boolean 	m_convertible		= false;
    public String 	m_maturity;
    public String 	m_issueDate;
    public String 	m_nextOptionDate;
    public String 	m_nextOptionType;
    public boolean 	m_nextOptionPartial = false;
    public String 	m_notes;

    public void mirrorFrom(ContractDetails cd)
    {
        if (!cd.m_summary.m_secType.equals("BAG"))
        {
            id = new StringId("IB." + cd.m_summary.m_conId + "." + cd.m_summary.m_exchange);
        }
        else
        {
            StringBuffer sb = new StringBuffer("IB.");
            ComboLeg leg = null;
            Iterator iter = cd.m_summary.m_comboLegs.iterator();
            while (iter.hasNext())
            {
                leg = (ComboLeg) iter.next();
                sb.append(leg.m_conId).append(".");
            }
            sb.append(leg.m_exchange);
            id = new StringId(sb.toString());
        }

        m_summary = new SerializableContract(cd.m_summary);

        m_marketName = cd.m_marketName;
        m_tradingClass = cd.m_tradingClass;
        m_minTick = cd.m_minTick;
        m_priceMagnifier = cd.m_priceMagnifier;
        m_orderTypes = cd.m_orderTypes;
        m_validExchanges = cd.m_validExchanges;
        m_underConId = cd.m_underConId;
        m_longName = cd.m_longName;
        m_contractMonth = cd.m_contractMonth;
        m_industry = cd.m_industry;
        m_category = cd.m_category;
        m_subcategory = cd.m_subcategory;
        m_timeZoneId = cd.m_timeZoneId;
        m_tradingHours = cd.m_tradingHours;
        m_liquidHours = cd.m_liquidHours;

        m_cusip = cd.m_cusip;
        m_ratings = cd.m_ratings;
        m_descAppend = cd.m_descAppend;
        m_bondType = cd.m_bondType;
        m_couponType = cd.m_couponType;
        m_callable = cd.m_callable;
        m_putable = cd.m_putable;
        m_coupon = cd.m_coupon;
        m_convertible = cd.m_convertible;
        m_maturity = cd.m_maturity;
        m_issueDate = cd.m_issueDate;
        m_nextOptionDate = cd.m_nextOptionDate;
        m_nextOptionType = cd.m_nextOptionType;
        m_nextOptionPartial = cd.m_nextOptionPartial;
        m_notes = cd.m_notes;
    }
    
    public void mirrorTo(ContractDetails cd)
    {
        m_summary.mirrorTo(cd.m_summary);
        
        cd.m_marketName = m_marketName;
        cd.m_tradingClass = m_tradingClass;
        cd.m_minTick = m_minTick;
        cd.m_priceMagnifier = m_priceMagnifier;
        cd.m_orderTypes = m_orderTypes;
        cd.m_validExchanges = m_validExchanges;
        cd.m_underConId = m_underConId;
        cd.m_longName = m_longName;
        cd.m_contractMonth = m_contractMonth;
        cd.m_industry = m_industry;
        cd.m_category = m_category;
        cd.m_subcategory = m_subcategory;
        cd.m_timeZoneId = m_timeZoneId;
        cd.m_tradingHours = m_tradingHours;
        cd.m_liquidHours = m_liquidHours;

        cd.m_cusip = m_cusip;
        cd.m_ratings = m_ratings;
        cd.m_descAppend = m_descAppend;
        cd.m_bondType = m_bondType;
        cd.m_couponType = m_couponType;
        cd.m_callable = m_callable;
        cd.m_putable = m_putable;
        cd.m_coupon = m_coupon;
        cd.m_convertible = m_convertible;
        cd.m_maturity = m_maturity;
        cd.m_issueDate = m_issueDate;
        cd.m_nextOptionDate = m_nextOptionDate;
        cd.m_nextOptionType = m_nextOptionType;
        cd.m_nextOptionPartial = m_nextOptionPartial;
        cd.m_notes = m_notes;
        
    }
}
