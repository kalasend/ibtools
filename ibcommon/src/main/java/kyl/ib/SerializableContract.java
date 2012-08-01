package kyl.ib;


import com.ib.client.ComboLeg;
import com.ib.client.Contract;
import com.ib.client.UnderComp;

import java.io.Serializable;
import java.util.Vector;

public class SerializableContract implements Serializable
{
    public int    m_conId;
    public String m_symbol;
    public String m_secType;
    public String m_expiry;
    public double m_strike;
    public String m_right;
    public String m_multiplier;
    public String m_exchange;

    public String m_currency;
    public String m_localSymbol;
    public String m_primaryExch;
    public boolean m_includeExpired;

    public String m_secIdType;
    public String m_secId;

    public String m_comboLegsDescrip;
    public SerializableComboLeg[] m_comboLegs = null;

    public UnderComp m_underComp;

    public SerializableContract()
    {
    }

    public SerializableContract(Contract con)
    {
        mirrorFrom(con);
    }

    public void mirrorFrom(Contract con)
    {
        m_conId = con.m_conId;
        m_symbol = con.m_symbol;
        m_secType = con.m_secType;
        m_expiry = con.m_expiry;
        m_strike = con.m_strike;
        m_right = con.m_right;
        m_multiplier = con.m_multiplier;
        m_exchange = con.m_exchange;

        m_currency = con.m_currency;
        m_localSymbol = con.m_localSymbol;
        m_primaryExch = con.m_primaryExch;
        m_includeExpired = con.m_includeExpired;

        m_secIdType = con.m_secIdType;
        m_secId = con.m_secId;
        m_comboLegsDescrip = con.m_comboLegsDescrip;

        m_underComp = con.m_underComp;



        if (con.m_comboLegs != null && con.m_comboLegs.size() > 0)
        {
            int n = con.m_comboLegs.size();
            m_comboLegs = new SerializableComboLeg[n];
            for (int i = 0; i < n; i++)
            {
                m_comboLegs[i] = new SerializableComboLeg((ComboLeg) con.m_comboLegs.get(i));
            }
        }
    }

    public void mirrorTo(Contract con)
    {
        con.m_conId = m_conId;
        con.m_symbol = m_symbol;
        con.m_secType = m_secType;
        con.m_expiry = m_expiry;
        con.m_strike = m_strike;
        con.m_right = m_right;
        con.m_multiplier = m_multiplier;
        con.m_exchange = m_exchange;

        con.m_currency = m_currency;
        con.m_localSymbol = m_localSymbol;
        con.m_primaryExch = m_primaryExch;
        con.m_includeExpired = m_includeExpired;

        con.m_secIdType = m_secIdType;
        con.m_secId = m_secId;
        con.m_comboLegsDescrip = m_comboLegsDescrip;

        con.m_underComp = m_underComp;

        if (m_comboLegs != null && m_comboLegs.length > 0)
        {
            if (con.m_comboLegs != null)
            {
                con.m_comboLegs.removeAllElements();
            }
            else
            {
                con.m_comboLegs = new Vector<Object>();
            }

            for (int i = 0; i < m_comboLegs.length; i++)
            {
                ComboLeg leg = new ComboLeg();
                m_comboLegs[i].mirrorTo(leg);
                con.m_comboLegs.add(leg);
            }
        }
    }
}
