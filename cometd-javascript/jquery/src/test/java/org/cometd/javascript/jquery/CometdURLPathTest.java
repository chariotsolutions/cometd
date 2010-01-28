package org.cometd.javascript.jquery;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.BayeuxServer.Extension;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.transports.HttpTransport;
import org.mozilla.javascript.ScriptableObject;

/**
 * @version $Revision$ $Date$
 */
public class CometdURLPathTest extends AbstractCometdJQueryTest
{
    @Override
    protected void customizeBayeux(BayeuxServerImpl bayeux)
    {
        bayeux.addExtension(new BayeuxURLExtension(bayeux));
    }

    public void testURLPath() throws Exception
    {
        defineClass(Latch.class);
        evaluateScript("var connectLatch = new Latch(1);");
        Latch connectLatch = get("connectLatch");
        evaluateScript("var handshake = undefined;");
        evaluateScript("var connect = undefined;");
        evaluateScript("$.cometd.addListener('/meta/handshake', function(message) { handshake = message; });");
        evaluateScript("$.cometd.addListener('/meta/connect', function(message) { connect = message; connectLatch.countDown(); });");
        evaluateScript("$.cometd.init({url: '" + cometdURL + "/', logLevel: 'debug'})");
        assertTrue(connectLatch.await(1000));

        evaluateScript("window.assert(handshake !== undefined, 'handshake is undefined');");
        evaluateScript("window.assert(handshake.ext !== undefined, 'handshake without ext');");
        String handshakeURI = evaluateScript("handshake.ext.uri");
        assertTrue(handshakeURI.endsWith("/handshake"));

        evaluateScript("window.assert(connect !== undefined, 'connect is undefined');");
        evaluateScript("window.assert(connect.ext !== undefined, 'connect without ext');");
        String connectURI = evaluateScript("connect.ext.uri");
        assertTrue(connectURI.endsWith("/connect"));

        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = get("disconnectLatch");
        evaluateScript("var disconnect = undefined;");
        evaluateScript("$.cometd.addListener('/meta/disconnect', function(message) { disconnect = message; disconnectLatch.countDown(); });");
        evaluateScript("$.cometd.disconnect();");
        assertTrue(disconnectLatch.await(1000));

        evaluateScript("window.assert(disconnect !== undefined, 'disconnect is undefined');");
        evaluateScript("window.assert(disconnect.ext !== undefined, 'disconnect without ext');");
        String disconnectURI = evaluateScript("disconnect.ext.uri");
        assertTrue(disconnectURI.endsWith("/disconnect"));
    }

    public static class Latch extends ScriptableObject
    {
        private volatile CountDownLatch latch;

        public String getClassName()
        {
            return "Latch";
        }

        public void jsConstructor(int count)
        {
            latch = new CountDownLatch(count);
        }

        public boolean await(long timeout) throws InterruptedException
        {
            return latch.await(timeout, TimeUnit.MILLISECONDS);
        }

        public void jsFunction_countDown()
        {
            latch.countDown();
        }
    }

    public static class BayeuxURLExtension implements Extension
    {
        private final BayeuxServerImpl bayeux;

        public BayeuxURLExtension(BayeuxServerImpl bayeux)
        {
            this.bayeux = bayeux;
        }
        
        @Override
        public boolean rcv(ServerSession from, Mutable message)
        {
            return true;
        }

        @Override
        public boolean rcvMeta(ServerSession from, Mutable message)
        {
            return true;
        }

        @Override
        public boolean send(Mutable message)
        {
            return true;
        }

        @Override
        public boolean sendMeta(ServerSession to, Mutable message)
        {
            if (Channel.META_HANDSHAKE.equals(message.getChannel()) ||
                Channel.META_CONNECT.equals(message.getChannel()) ||
                Channel.META_DISCONNECT.equals(message.getChannel()))
            {
                HttpTransport transport = (HttpTransport)bayeux.getCurrentTransport();
                HttpServletRequest request = transport.getCurrentRequest();
                String uri = request.getRequestURI();
                message.getExt(true).put("uri", uri);
            }
            return true;
        }
    }
    
}