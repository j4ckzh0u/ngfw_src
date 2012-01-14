/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetSocketAddress;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Compiles pipes based on subscriptions and interest sets.
 */
public interface PipelineFoundry
{
    PipelineEndpoints createInitialEndpoints(IPSessionDesc start, String username);
    void registerEndpoints(PipelineEndpoints pe);

    ArgonConnector createArgonConnector(PipeSpec spec, SessionEventListener listener);
    void registerArgonConnector(ArgonConnector argonConnector);
    void deregisterArgonConnector(ArgonConnector argonConnector);

    void registerCasing(ArgonConnector insideArgonConnector, ArgonConnector outsideArgonConnector);
    void deregisterCasing(ArgonConnector insideArgonConnector);

    /* Remove all of the cached chains */
    void clearChains();

    void registerConnection(InetSocketAddress socketAddress, Fitting fitting);
    Pipeline getPipeline(long sessionId);
}
