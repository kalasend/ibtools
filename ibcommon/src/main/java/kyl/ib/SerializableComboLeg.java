package kyl.ib;

import com.ib.client.ComboLeg;

import java.io.Serializable;

public class SerializableComboLeg implements Serializable
{
    public int 					m_conId;
    public int 					m_ratio;
    public String 				m_action; // BUY/SELL/SSHORT/SSHORTX
    public String 				m_exchange;
    public int 					m_openClose;

    // for stock legs when doing short sale
    public int                  m_shortSaleSlot; // 1 = clearing broker, 2 = third party
    public String               m_designatedLocation;
    public int                  m_exemptCode;

    public SerializableComboLeg(ComboLeg leg)
    {
        mirrorFrom(leg);
    }

    public void mirrorFrom(ComboLeg leg)
    {
        m_conId = leg.m_conId;
        m_ratio = leg.m_ratio;
        m_action = leg.m_exchange;
        m_openClose = leg.m_openClose;
        m_shortSaleSlot = leg.m_shortSaleSlot;
        m_designatedLocation = leg.m_designatedLocation;
        m_exemptCode = leg.m_exemptCode;
    }

    public void mirrorTo(ComboLeg leg)
    {
        leg.m_conId = m_conId;
        leg.m_ratio = m_ratio;
        leg.m_exchange = m_exchange;
        leg.m_openClose = m_openClose;
        leg.m_shortSaleSlot = m_shortSaleSlot;
        leg.m_designatedLocation = m_designatedLocation;
        leg.m_exemptCode = m_exemptCode;
    }
}
