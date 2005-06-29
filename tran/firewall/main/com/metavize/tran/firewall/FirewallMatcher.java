/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.firewall;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.IPSessionDesc;

import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.TrafficMatcher;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a FirewallRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
class FirewallMatcher extends TrafficMatcher {
    private static final Logger logger = Logger.getLogger( FirewallMatcher.class );
    
    public static final FirewallMatcher MATCHER_DISABLED = 
        new FirewallMatcher( false, ProtocolMatcher.MATCHER_NIL,
                             IntfMatcher.MATCHER_NIL, IntfMatcher.MATCHER_NIL,
                             IPMatcher.MATCHER_NIL,   IPMatcher.MATCHER_ALL, 
                             PortMatcher.MATCHER_NIL, PortMatcher.MATCHER_NIL,
                             false );

    /* Used for logging */
    private final FirewallRule rule;
    private final int ruleIndex;

    private final boolean isTrafficBlocker;


    // XXX For the future
    // TimeMatcher time;
    public FirewallMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                            IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                            IPMatcher   srcAddress, IPMatcher       dstAddress,
                            PortMatcher srcPort,    PortMatcher     dstPort,
                            boolean isTrafficBlocker )
    {
        super( isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );

        /* Attributes of the firewall rule */
        this.isTrafficBlocker = isTrafficBlocker;
        
        /* XXX probably want to set this to a more creative value, or just get rid of this constructor
         * it is never used */
        this.rule      = null;
        this.ruleIndex = 0;
    }

    FirewallMatcher( FirewallRule rule, int ruleIndex )
    {
        super( rule );
        
        this.rule      = rule;
        this.ruleIndex = ruleIndex;

        /* Attributes of the redirect */
        isTrafficBlocker = rule.isTrafficBlocker();
    }

    public boolean isTrafficBlocker()
    {
        return this.isTrafficBlocker;
    }

    public FirewallRule rule()
    {
        return this.rule;
    }

    public int ruleIndex()
    {
        return this.ruleIndex;
    }
}
