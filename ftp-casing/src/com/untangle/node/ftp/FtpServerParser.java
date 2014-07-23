/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.ChunkToken;
import com.untangle.node.token.EndMarkerToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenStreamer;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Parser for the server side of FTP connection.
 */
public class FtpServerParser extends AbstractParser
{
    private static final char SP = ' ';
    private static final char HYPHEN = '-';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Logger logger = Logger.getLogger(FtpServerParser.class);

    public FtpServerParser()
    {
        super( false );
    }

    public void handleNewSession( NodeTCPSession session )
    {
        lineBuffering( session, true );
    }

    public void parse( NodeTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getOutputFitting();
        if (Fitting.FTP_CTL_STREAM == fitting) {
            parseServerCtl( session, buf );
            return;
        } else if (Fitting.FTP_DATA_STREAM == fitting) {
            parseServerData( session, buf );
            return;
        } else {
            throw new IllegalStateException("bad input fitting: " + fitting);
        }
    }

    public void parseEnd( NodeTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getOutputFitting();
        if ( fitting == Fitting.FTP_DATA_STREAM ) {
            session.sendObjectToClient( EndMarkerToken.MARKER );
            return;
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return;
        }
    }

    public void endSession( NodeTCPSession session )
    {
        session.shutdownClient();
        return;
    }

    private void parseServerCtl( NodeTCPSession session, ByteBuffer buf )
    {
        ByteBuffer dup = buf.duplicate();

        if (completeLine(dup)) {
            int replyCode = replyCode(dup);

            if (-1 == replyCode) {
                throw new RuntimeException("expected reply code");
            }

            switch (dup.get()) {
            case SP: {
                String message = AsciiCharBuffer.wrap(buf).toString();

                FtpReply reply = new FtpReply(replyCode, message);
                session.sendObjectToClient( reply );
                return;
            }

            case HYPHEN: {
                int i = dup.limit() - 2;
                while (3 < --i && LF != dup.get(i));

                if (LF != dup.get(i++)) {
                    break;
                }

                ByteBuffer end = dup.duplicate();
                end.position(i);
                end.limit(end.limit() - 2);
                int endCode = replyCode(end);

                if (-1 == endCode || SP != end.get()) {
                    break;
                }

                String message = AsciiCharBuffer.wrap(buf).toString();

                FtpReply reply = new FtpReply(replyCode, message);

                session.sendObjectToClient( reply );
                return;
            }

            default:
                throw new RuntimeException("expected a space");
            }
        }

        // incomplete input
        if (buf.limit() + 80 > buf.capacity()) {
            ByteBuffer b = ByteBuffer.allocate(2 * buf.capacity());
            b.put(buf);
            buf = b;
        } else {
            buf.compact();
        }

        session.setServerBuffer( buf ); // wait for more data
        return;
    }

    private void parseServerData( NodeTCPSession session, ByteBuffer buf )
    {
        ChunkToken c = new ChunkToken(buf.duplicate());
        session.sendObjectToClient( c );
        return;
    }

    private int replyCode(ByteBuffer buf)
    {
        int i = 0;

        byte c = buf.get();
        if (48 <= c && 57 >= c) {
            i = (c - 48) * 100;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48) * 10;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48);
        } else {
            return -1;
        }

        return i;
    }

    /**
     * Checks if the buffer contains a complete line.
     *
     * @param buf to check.
     * @return true if a complete line.
     */
    private boolean completeLine(ByteBuffer buf)
    {
        int l = buf.limit();
        return buf.remaining() >= 2 && buf.get(l - 2) == CR
            && buf.get(l - 1) == LF;
    }
}
