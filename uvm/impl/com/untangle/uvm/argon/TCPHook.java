/**
 * $Id$
 */
package com.untangle.uvm.argon;

import java.net.InetAddress;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapHook;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.TCPSink;
import com.untangle.jvector.TCPSource;
import com.untangle.uvm.node.SessionEvent;

public class TCPHook implements NetcapHook
{
    private static TCPHook INSTANCE;
    private final Logger logger = Logger.getLogger(getClass());


    public static TCPHook getInstance()
    {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private TCPHook() {}

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new TCPHook();
    }

    public void event( long sessionID )
    {
        new TCPArgonHook( sessionID ).run();
    }

    private class TCPArgonHook extends ArgonHook
    {
        protected static final int TIMEOUT = -1;

        /* Get a reasonable time */
        /* After 10 seconds, this session better die if both endpoints are dead */
        protected static final int DEAD_TIMEOUT = 10 * 1000;

        protected final NetcapTCPSession netcapTCPSession;

        protected boolean ifServerComplete = false;
        protected boolean ifClientComplete = false;

        protected ArgonTCPSession prevSession = null;
        protected final TCPSideListener clientSideListener = new TCPSideListener();
        protected final TCPSideListener serverSideListener = new TCPSideListener();

        protected TCPArgonHook( long id )
        {
            netcapTCPSession   = new NetcapTCPSession( id );
        }

        protected int timeout()
        {
            return TIMEOUT;
        }

        protected NetcapSession netcapSession()
        {
            return netcapTCPSession;
        }

        protected SideListener clientSideListener()
        {
            return clientSideListener;
        }

        protected SideListener serverSideListener()
        {
            return serverSideListener;
        }

        protected boolean serverComplete()
        {
            InetAddress clientAddr;
            int clientPort;
            InetAddress serverAddr;
            int serverPort;

            if ( sessionList.isEmpty()) {
                clientAddr = netcapTCPSession.serverSide().client().host();
                clientPort = netcapTCPSession.serverSide().client().port();

                serverAddr = netcapTCPSession.serverSide().server().host();
                serverPort = netcapTCPSession.serverSide().server().port();
            } else {
                /* Complete with the parameters from the last node */
                ArgonTCPSession session = (ArgonTCPSession)sessionList.get( sessionList.size() - 1 );

                clientAddr = session.getClientAddr();
                clientPort = session.getClientPort();
                serverAddr = session.getServerAddr();
                serverPort = session.getServerPort();
            }

            /* XXX Have to check if it is destined locally, if so, you don't create two
             * connections, you just connect locally, instead, we could redirect the connection
             * from 127.0.0.1 to 127.0.0.1, it just limits the number of possible sessions
             * to that one server to 0xFFFF */

            if ( logger.isInfoEnabled()) {
                logger.info( "TCP - Completing server connection: " + sessionGlobalState );
                logger.debug( "Client: " + clientAddr + ":" + clientPort );
                logger.debug( "Server: " + serverAddr + ":" + serverPort );
            }

            try {
                int intfId = clientSide.getServerIntf();
                netcapTCPSession.serverComplete( clientAddr, clientPort, serverAddr, serverPort, intfId );
                netcapTCPSession.tcpServerSide().blocking( false );
                ifServerComplete = true;
            } catch ( Exception e ) {
                logger.info( "TCP - Unable to complete connection to the server: " + e );
                ifServerComplete = false;
            }

            return ifServerComplete;
        }

        protected boolean clientComplete()
        {
            if ( logger.isInfoEnabled()) {
                logger.info( "TCP - Completing client connection: " + sessionGlobalState );
                logger.info( "TCP - client acked: " + netcapTCPSession.acked());
            }

            try {
                if ( !netcapTCPSession.acked())
                    netcapTCPSession.clientComplete();

                netcapTCPSession.tcpClientSide().blocking( false );

                ifClientComplete = true;
            } catch ( Exception e ) {
                logger.info( "TCP - Unable to complete connection to the client: " + e );
                ifClientComplete = false;
            }

            return ifClientComplete;
        }

        protected void clientReject()
        {
            if ( logger.isDebugEnabled()) logger.debug( "TCP - Rejecting client" );

            switch( rejectCode ) {
            case ArgonIPNewSessionRequest.TCP_REJECT_RESET:
                netcapTCPSession.clientReset();
                break;

            case ArgonIPNewSessionRequest.NET_UNREACHABLE:
            case ArgonIPNewSessionRequest.HOST_UNREACHABLE:
            case ArgonIPNewSessionRequest.PROTOCOL_UNREACHABLE:
            case ArgonIPNewSessionRequest.PORT_UNREACHABLE:
            case ArgonIPNewSessionRequest.DEST_HOST_UNKNOWN:
            case ArgonIPNewSessionRequest.PROHIBITED:
                netcapTCPSession.clientSendIcmpDestUnreach((byte)rejectCode );
                break;

            case REJECT_CODE_SRV:
                netcapTCPSession.clientForwardReject();
                break;

            default:
                logger.error( "TCP - Unknown reject code: " + rejectCode + " resetting " );
                netcapTCPSession.clientReset();
            }
        }

        protected void clientRejectSilent()
        {
            if ( logger.isDebugEnabled()) logger.debug( "TCP - Dropping client" );

            netcapTCPSession.clientDrop();
        }

        protected Sink makeClientSink()
        {
            return new TCPSink( netcapTCPSession.tcpClientSide().fd(), clientSideListener );
        }

        protected Sink makeServerSink()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server sink for an uncompleted connection" );
            }

            return new TCPSink( netcapTCPSession.tcpServerSide().fd(), serverSideListener );
        }

        protected Source makeClientSource()
        {
            return new TCPSource( netcapTCPSession.tcpClientSide().fd(), clientSideListener );
        }

        protected Source makeServerSource()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server source for an uncompleted connection" );
            }

            return new TCPSource( netcapTCPSession.tcpServerSide().fd(), serverSideListener );
        }

        protected void newSessionRequest( ArgonAgent agent, Iterator<?> iter, SessionEvent pe )
        {
            ArgonTCPNewSessionRequest request;

            if ( prevSession == null ) {
                request = new ArgonTCPNewSessionRequestImpl( sessionGlobalState, agent, pe );
            } else {
                request = new ArgonTCPNewSessionRequestImpl( prevSession, agent, pe, sessionGlobalState );
            }

            // newSession() returns null when rejecting the session
            ArgonTCPSession session = agent.getNewSessionEventListener().newSession( request );

            try {
                processSession( request, session );
            } catch (IllegalStateException e) {
                logger.warn(agent.toString() + " Exception: ", e);
                throw e;
            }
	    
            if ( iter.hasNext()) {
            	/* Advance the previous session if the node requested or released the session */
            	if (( request.state() == ArgonIPNewSessionRequest.REQUESTED ) ||
            			( request.state() == ArgonIPNewSessionRequest.RELEASED && session != null )) {
            		prevSession = session;
            	}
            } else {
            	prevSession = null;
            }
        }

        protected void raze()
        {
            netcapTCPSession.raze();
        }

        public void checkEndpoints()
        {
            /* If both sides are shutdown, give a timeout to complete vectoring */
            if ( clientSideListener.isShutdown() && serverSideListener.isShutdown()) {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "Setting timeout for dead endpoint TCP session to " + DEAD_TIMEOUT );
                    logger.debug( "Stats" );
                    logger.debug( "client side: " + clientSideListener.stats());
                    logger.debug( "server side: " + serverSideListener.stats());
                }

                vector.timeout( DEAD_TIMEOUT );
            }
        }

        private class TCPSideListener extends SideListener
        {
            protected TCPSideListener()
            {
            }

            public void dataEvent( Source source, int numBytes )
            {
                super.dataEvent( source, numBytes );
            }

            public void dataEvent( Sink sink, int numBytes )
            {
                super.dataEvent( sink, numBytes );
            }

            public void shutdownEvent( Source source )
            {
                super.shutdownEvent( source );
                checkEndpoints();
            }

            public void shutdownEvent( Sink sink )
            {
                super.shutdownEvent( sink );
                checkEndpoints();
            }
        }
    }
}
