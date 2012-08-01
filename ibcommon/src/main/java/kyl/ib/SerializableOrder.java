package kyl.ib;

import com.ib.client.*;

import java.io.*;
import java.util.*;

public class SerializableOrder implements Serializable
{
    // main order fields
    public int 		m_orderId;
    public int 		m_clientId;
    public int  	m_permId;
    public String 	m_action;
    public int 		m_totalQuantity;
    public String 	m_orderType;
    public double 	m_lmtPrice;
    public double 	m_auxPrice;

    // extended order fields
    public String 	m_tif;  // "Time in Force" - DAY, GTC, etc.
    public String 	m_ocaGroup; // one cancels all group name
    public int      m_ocaType;  // 1 = CANCEL_WITH_BLOCK, 2 = REDUCE_WITH_BLOCK, 3 = REDUCE_NON_BLOCK
    public String 	m_orderRef;
    public boolean 	m_transmit;	// if false, order will be created but not transmited
    public int 		m_parentId;	// Parent order Id, to associate Auto STP or TRAIL orders with the original order.
    public boolean 	m_blockOrder;
    public boolean	m_sweepToFill;
    public int 		m_displaySize;
    public int 		m_triggerMethod; // 0=Default, 1=Double_Bid_Ask, 2=Last, 3=Double_Last, 4=Bid_Ask, 7=Last_or_Bid_Ask, 8=Mid-point
    public boolean 	m_outsideRth;
    public boolean  m_hidden;
    public String   m_goodAfterTime; // FORMAT: 20060505 08:00:00 {time zone}
    public String   m_goodTillDate;  // FORMAT: 20060505 08:00:00 {time zone}
    public boolean  m_overridePercentageConstraints;
    public String   m_rule80A;  // Individual = 'I', Agency = 'A', AgentOtherMember = 'W', IndividualPTIA = 'J', AgencyPTIA = 'U', AgentOtherMemberPTIA = 'M', IndividualPT = 'K', AgencyPT = 'Y', AgentOtherMemberPT = 'N'
    public boolean  m_allOrNone;
    public int      m_minQty;
    public double   m_percentOffset;    // REL orders only
    public double   m_trailStopPrice;   // for TRAILLIMIT orders only

    // Financial advisors only
    public String   m_faGroup;
    public String   m_faProfile;
    public String   m_faMethod;
    public String   m_faPercentage;

    // Institutional orders only
    public String 	m_openClose;          // O=Open, C=Close
    public int 		m_origin;             // 0=Customer, 1=Firm
    public int      m_shortSaleSlot;      // 1 if you hold the shares, 2 if they will be delivered from elsewhere.  Only for Action="SSHORT
    public String   m_designatedLocation; // set when slot=2 only.
    public int      m_exemptCode;

    // SMART routing only
    public double   m_discretionaryAmt;
    public boolean  m_eTradeOnly;
    public boolean  m_firmQuoteOnly;
    public double   m_nbboPriceCap;

    // BOX or VOL ORDERS ONLY
    public int      m_auctionStrategy; // 1=AUCTION_MATCH, 2=AUCTION_IMPROVEMENT, 3=AUCTION_TRANSPARENT

    // BOX ORDERS ONLY
    public double   m_startingPrice;
    public double   m_stockRefPrice;
    public double   m_delta;

    // pegged to stock or VOL orders
    public double   m_stockRangeLower;
    public double   m_stockRangeUpper;

    // VOLATILITY ORDERS ONLY
    public double   m_volatility;
    public int      m_volatilityType;     // 1=daily, 2=annual
    public int      m_continuousUpdate;
    public int      m_referencePriceType; // 1=Average, 2 = BidOrAsk
    public String   m_deltaNeutralOrderType;
    public double   m_deltaNeutralAuxPrice;

    // COMBO ORDERS ONLY
    public double   m_basisPoints;      // EFP orders only
    public int      m_basisPointsType;  // EFP orders only

    // SCALE ORDERS ONLY
    public int      m_scaleInitLevelSize;
    public int      m_scaleSubsLevelSize;
    public double   m_scalePriceIncrement;

    // HEDGE ORDERS ONLY
    public String   m_hedgeType; // 'D' - delta, 'B' - beta, 'F' - FX, 'P' - pair
    public String   m_hedgeParam; // beta value for beta hedge, ratio for pair hedge

    // Clearing info
    public String 	m_account; // IB account
    public String   m_settlingFirm;
    public String   m_clearingAccount; // True beneficiary of the order
    public String   m_clearingIntent; // "" (Default), "IB", "Away", "PTA" (PostTrade)

    // ALGO ORDERS ONLY
    public String m_algoStrategy;
    public TagValue[] m_algoParams = null;

    // What-if
    public boolean  m_whatIf;

    // Not Held
    public boolean  m_notHeld;


    public void mirrorFrom(Order o)
    {
        m_orderId = o.m_orderId;
        m_clientId = o.m_clientId;
        m_permId = o.m_permId;
        m_action = o.m_action;
        m_totalQuantity = o.m_totalQuantity;
        m_orderType = o.m_orderType;
        m_lmtPrice = o.m_lmtPrice;
        m_auxPrice = o.m_auxPrice;

        // extended order fields
        m_tif = o.m_tif;
        m_ocaGroup = o.m_ocaGroup;
        m_ocaType = o.m_ocaType;
        m_orderRef = o.m_orderRef;
        m_transmit = o.m_transmit;
        m_parentId = o.m_parentId;
        m_blockOrder = o.m_blockOrder;
        m_sweepToFill = o.m_sweepToFill;
        m_displaySize = o.m_displaySize;
        m_triggerMethod = o.m_triggerMethod;
        m_outsideRth = o.m_outsideRth;
        m_hidden = o.m_hidden;
        m_goodAfterTime = o.m_goodAfterTime;
        m_goodTillDate = o.m_goodTillDate;
        m_overridePercentageConstraints = o.m_overridePercentageConstraints;
        m_rule80A = o.m_rule80A;
        m_allOrNone = o.m_allOrNone;
        m_minQty = o.m_minQty;
        m_percentOffset = o.m_percentOffset;
        m_trailStopPrice = o.m_trailStopPrice;

        // Financial advisors only
        m_faGroup = o.m_faGroup;
        m_faProfile = o.m_faProfile;
        m_faMethod = o.m_faMethod;
        m_faPercentage = o.m_faPercentage;

        // Institutional orders only
        m_openClose = o.m_openClose;
        m_origin = o.m_origin;
        m_shortSaleSlot = o.m_shortSaleSlot;
        m_designatedLocation = o.m_designatedLocation;
        m_exemptCode = o.m_exemptCode;

        // SMART routing only
        m_discretionaryAmt = o.m_discretionaryAmt;
        m_eTradeOnly = o.m_eTradeOnly;
        m_firmQuoteOnly = o.m_firmQuoteOnly;
        m_nbboPriceCap = o.m_nbboPriceCap;

        // BOX or VOL ORDERS ONLY
        m_auctionStrategy = o.m_auctionStrategy;

        // BOX ORDERS ONLY
        m_startingPrice = o.m_startingPrice;
        m_stockRefPrice = o.m_stockRefPrice;
        m_delta = o.m_delta;

        // pegged to stock or VOL orders
        m_stockRangeLower = o.m_stockRangeLower;
        m_stockRangeUpper = o.m_stockRangeUpper;

        // VOLATILITY ORDERS ONLY
        m_volatility = o.m_volatility;
        m_volatilityType = o.m_volatilityType;
        m_continuousUpdate = o.m_continuousUpdate;
        m_referencePriceType = o.m_referencePriceType;
        m_deltaNeutralOrderType = o.m_deltaNeutralOrderType;
        m_deltaNeutralAuxPrice = o.m_deltaNeutralAuxPrice;

        // COMBO ORDERS ONLY
        m_basisPoints = o.m_basisPoints;
        m_basisPointsType = o.m_basisPointsType;

        // SCALE ORDERS ONLY
        m_scaleInitLevelSize = o.m_scaleInitLevelSize;
        m_scaleSubsLevelSize = o.m_scaleSubsLevelSize;
        m_scalePriceIncrement = o.m_scalePriceIncrement;

        // HEDGE ORDERS ONLY
        m_hedgeType = o.m_hedgeType;
        m_hedgeParam = o.m_hedgeParam;

        // Clearing info
        m_account = o.m_account;
        m_settlingFirm = o.m_settlingFirm;
        m_clearingAccount = o.m_clearingAccount;
        m_clearingIntent = o.m_clearingIntent;

        // ALGO ORDERS ONLY
        m_algoStrategy = o.m_algoStrategy;
        if (o.m_algoParams != null)
        {
            m_algoParams = new TagValue[2];
            for (int i = 0; i < o.m_algoParams.size(); i++)
            {
                TagValue tag = o.m_algoParams.get(i);
                m_algoParams[i] = tag;
            }
        }

        // What-if
        m_whatIf = o.m_whatIf;

        // Not Held
        m_notHeld = o.m_notHeld;
    }

    public void mirrorTo(Order o)
    {
        o.m_orderId = m_orderId;
        o.m_clientId = m_clientId;
        o.m_permId = m_permId;
        o.m_action = m_action;
        o.m_totalQuantity = m_totalQuantity;
        o.m_orderType = m_orderType;
        o.m_lmtPrice = m_lmtPrice;
        o.m_auxPrice = m_auxPrice;

        // extended order fields
        o.m_tif = m_tif;
        o.m_ocaGroup = m_ocaGroup;
        o.m_ocaType = m_ocaType;
        o.m_orderRef = m_orderRef;
        o.m_transmit = m_transmit;
        o.m_parentId = m_parentId;
        o.m_blockOrder = m_blockOrder;
        o.m_sweepToFill = m_sweepToFill;
        o.m_displaySize = m_displaySize;
        o.m_triggerMethod = m_triggerMethod;
        o.m_outsideRth = m_outsideRth;
        o.m_hidden = m_hidden;
        o.m_goodAfterTime = m_goodAfterTime;
        o.m_goodTillDate = m_goodTillDate;
        o.m_overridePercentageConstraints = m_overridePercentageConstraints;
        o.m_rule80A = m_rule80A;
        o.m_allOrNone = m_allOrNone;
        o.m_minQty = m_minQty;
        o.m_percentOffset = m_percentOffset;
        o.m_trailStopPrice = m_trailStopPrice;

        // Financial advisors only
        o.m_faGroup = m_faGroup;
        o.m_faProfile = m_faProfile;
        o.m_faMethod = m_faMethod;
        o.m_faPercentage = m_faPercentage;

        // Institutional orders only
        o.m_openClose = m_openClose;
        o.m_origin = m_origin;
        o.m_shortSaleSlot = m_shortSaleSlot;
        o.m_designatedLocation = m_designatedLocation;
        o.m_exemptCode = m_exemptCode;

        // SMART routing only
        o.m_discretionaryAmt = m_discretionaryAmt;
        o.m_eTradeOnly = m_eTradeOnly;
        o.m_firmQuoteOnly = m_firmQuoteOnly;
        o.m_nbboPriceCap = m_nbboPriceCap;

        // BOX or VOL ORDERS ONLY
        o.m_auctionStrategy = m_auctionStrategy;

        // BOX ORDERS ONLY
        o.m_startingPrice = m_startingPrice;
        o.m_stockRefPrice = m_stockRefPrice;
        o.m_delta = m_delta;

        // pegged to stock or VOL orders
        o.m_stockRangeLower = m_stockRangeLower;
        o.m_stockRangeUpper = m_stockRangeUpper;

        // VOLATILITY ORDERS ONLY
        o.m_volatility = m_volatility;
        o.m_volatilityType = m_volatilityType;
        o.m_continuousUpdate = m_continuousUpdate;
        o.m_referencePriceType = m_referencePriceType;
        o.m_deltaNeutralOrderType = m_deltaNeutralOrderType;
        o.m_deltaNeutralAuxPrice = m_deltaNeutralAuxPrice;

        // COMBO ORDERS ONLY
        o.m_basisPoints = m_basisPoints;
        o.m_basisPointsType = m_basisPointsType;

        // SCALE ORDERS ONLY
        o.m_scaleInitLevelSize = m_scaleInitLevelSize;
        o.m_scaleSubsLevelSize = m_scaleSubsLevelSize;
        o.m_scalePriceIncrement = m_scalePriceIncrement;

        // HEDGE ORDERS ONLY
        o.m_hedgeType = m_hedgeType;
        o.m_hedgeParam = m_hedgeParam;

        // Clearing info
        o.m_account = m_account;
        o.m_settlingFirm = m_settlingFirm;
        o.m_clearingAccount = m_clearingAccount;
        o.m_clearingIntent = m_clearingIntent;

        // What-if
        o.m_whatIf = m_whatIf;

        // Not Held
        o.m_notHeld = m_notHeld;

        // ALGO ORDERS ONLY
        o.m_algoStrategy = m_algoStrategy;
        if (m_algoParams != null && m_algoParams.length > 0)
        {
            o.m_algoParams = new Vector<TagValue>();
            for (int i = 0;  i < m_algoParams.length; i++)
            {
                o.m_algoParams.add(m_algoParams[i]);
            }
        }
    }
}
