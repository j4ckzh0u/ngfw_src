/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.File;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NotificationManager;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.PolicyManager;
import com.untangle.uvm.app.Reporting;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.StaticRoute;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.SimpleResolver;

/**
 * Implements NotificationManager. This class runs a series of test and creates notifications
 * for important things the administrator should know about. The UI displays these
 * notifications when the admin logs into the UI.
 *
 * Possible future notifications to add:
 * recent high load?
 * frequent reboots?
 * semi-frequent power loss?
 * disk almost full?
 * modified sources.list?
 */
public class NotificationManagerImpl implements NotificationManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private I18nUtil i18nUtil;

    private ExecManager execManager = null;

    public NotificationManagerImpl()
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        this.i18nUtil = new I18nUtil(i18nMap);
    }

    public synchronized List<String> getNotifications()
    {
        LinkedList<String> notificationList = new LinkedList<String>();
        boolean dnsWorking = false;

        this.execManager = UvmContextFactory.context().createExecManager();

        try { testUpgrades(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { dnsWorking = testDNS(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { if (dnsWorking) testConnectivity(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { if (dnsWorking) testConnector(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testDiskFree(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testDiskErrors(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testDupeApps(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testRendundantApps(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testBridgeBackwards(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testInterfaceErrors(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testSpamDNSServers(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testZveloDNSServers(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testEventWriteTime(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testEventWriteDelay(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testShieldEnabled(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testRoutesToReachableAddresses(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testServerConf(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }
        try { testLicenseCompliance(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }

        /**
         * Disabled Tests
         */
        //try { testQueueFullMessages(notificationList); } catch (Exception e) { logger.warn("Notification test exception",e); }

        this.execManager.close();
        this.execManager = null;

        return notificationList;
    }

    /**
     * This test tests to see if upgrades are available
     */
    private void testUpgrades(List<String> notificationList)
    {
        try {
            if (UvmContextFactory.context().systemManager().upgradesAvailable(false)) {
                notificationList.add(i18nUtil.tr("Upgrades are available and ready to be installed."));
            }
        } catch (Exception e) {}
    }

    /**
     * This test iterates through the DNS settings on each WAN and tests them individually
     * It creates a notification for each non-working DNS server
     */
    private boolean testDNS(List<String> notificationList)
    {
        ConnectivityTesterImpl connectivityTester = (ConnectivityTesterImpl)UvmContextFactory.context().getConnectivityTester();
        List<InetAddress> nonWorkingDns = new LinkedList<InetAddress>();

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            if (dnsPrimary != null)
                if (!connectivityTester.isDnsWorking(dnsPrimary, null))
                    nonWorkingDns.add(dnsPrimary);
            if (dnsSecondary != null)
                if (!connectivityTester.isDnsWorking(dnsSecondary, null))
                    nonWorkingDns.add(dnsSecondary);
        }

        if (nonWorkingDns.size() > 0) {
            String notificationText = i18nUtil.tr("DNS connectivity failed:") + " ";
            for (InetAddress ia : nonWorkingDns) {
                notificationText += ia.getHostAddress() + " ";
            }
            notificationList.add(notificationText);
            return false;
        }

        return true;
    }

    /**
     * This test tests connectivity to key servers in the untangle datacenter
     */
    private void testConnectivity(List<String> notificationList)
    {
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("updates.untangle.com",80), 7000);
        } catch ( Exception e ) {
            notificationList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [updates.untangle.com:80]") );
        } finally {
            try {if (socket != null) socket.close();} catch (Exception e) {}
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("license.untangle.com",443), 7000);
        } catch ( Exception e ) {
            notificationList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [license.untangle.com:443]") );
        } finally {
            try {if (socket != null) socket.close();} catch (Exception e) {}
        }
    }

    /**
     * This test that pyconnector is connected to cmd.untangle.com
     */
    private void testConnector(List<String> notificationList)
    {
        try {
            if ( UvmContextFactory.context().isDevel() )
                return;
            if ( ! UvmContextFactory.context().systemManager().getSettings().getCloudEnabled() )
                return;

            File pidFile = new File("/var/run/ut-pyconnector.pid");
            if ( !pidFile.exists() ) {
                notificationList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [cmd.untangle.com]") );
                return;
            }

            int result = this.execManager.execResult(System.getProperty("uvm.bin.dir") + "/ut-pyconnector-status");
            if (result != 0)
                notificationList.add( i18nUtil.tr("Failed to connect to Untangle." +  " [cmd.untangle.com]") );
        } catch (Exception e) {

        }
    }

    /*
     * This test that disk free % is less than 75%
     */
    private void testDiskFree(List<String> notificationList)
    {
        String result = this.execManager.execOutput( "df -k / | awk '/\\//{printf(\"%d\",$5)}'");

        try {
            int percentUsed = Integer.parseInt(result);
            if (percentUsed > 75)
                notificationList.add( i18nUtil.tr("Free Disk space is low.") +  " [ " + (100 - percentUsed) + i18nUtil.tr("% free ]") );
        } catch (Exception e) {
            logger.warn("Unable to determine free disk space",e);
        }

    }

    /**
     * Looks for somewhat comman errors in kern.log related to problematic disks
     */
    private void testDiskErrors(List<String> notificationList)
    {
        ExecManagerResult result;

        result = this.execManager.exec( "tail -n 15000 /var/log/kern.log | grep -m1 -B3 'DRDY ERR'" );
        if ( result.getResult() == 0 ) {
            notificationList.add( i18nUtil.tr("Disk errors reported.") + "<br/>\n" + result.getOutput().replaceAll("\n","<br/>\n") );
        }

        result = this.execManager.exec( "tail -n 15000 /var/log/kern.log | grep -v 'dev fd0' | grep -m1 -B3 'I/O error'" );
        if ( result.getResult() == 0 ) {
            notificationList.add( i18nUtil.tr("Disk errors reported.") + "<br/>\n" + result.getOutput().replaceAll("\n","<br/>\n") );
        }
    }
    
    /**
     * This test for multiple instances of the same application in a given rack
     * This is never a good idea
     */
    private void testDupeApps(List<String> notificationList)
    {
        LinkedList<AppSettings> appSettingsList = UvmContextFactory.context().appManager().getSettings().getApps();

        /**
         * Check each node for dupe nodes
         */
        for (AppSettings n1 : appSettingsList ) {
            for (AppSettings n2 : appSettingsList ) {
                if (n1.getId().equals(n2.getId()))
                    continue;

                /**
                 * If they have the same name and are in the same rack - they are dupes
                 * Check both for == and .equals so null is handled
                 */
                if (n1.getPolicyId() == null || n2.getPolicyId() == null) {
                    if (n1.getPolicyId() == n2.getPolicyId() && n1.getAppName().equals(n2.getAppName()))
                        notificationList.add( i18nUtil.tr("Services contains two or more") + " " + n1.getAppName() );
                } else {
                    if (n1.getPolicyId().equals(n2.getPolicyId()) && n1.getAppName().equals(n2.getAppName()))
                        notificationList.add(i18nUtil.tr("A policy/rack") + " [" + n1.getPolicyId() + "] " + i18nUtil.tr("contains two or more") + " " + n1.getAppName());
                }
            }
        }
    }

    /**
     * This test iterates through each rack and test for redundant applications
     * It creates a notification for each redundant pair
     * Currently the redundant apps are:
     * Web Filter and Web Filter Lite
     * Spam Blocker and Spam Blocker Lite
     * Web Filter and Web Monitor
     */
    private void testRendundantApps(List<String> notificationList)
    {
        /**
         * Check for redundant apps
         */
        List<App> spamBlockerLiteList = UvmContextFactory.context().appManager().appInstances("spam-blocker-lite");
        List<App> spamblockerList = UvmContextFactory.context().appManager().appInstances("spam-blocker");
        List<App> webMonitorList = UvmContextFactory.context().appManager().appInstances("web-monitor");
        List<App> webFilterList = UvmContextFactory.context().appManager().appInstances("web-filter");

        for (App node1 : webMonitorList) {
            for (App node2 : webFilterList) {
                if (node1.getAppSettings().getId().equals(node2.getAppSettings().getId()))
                    continue;

                if (node1.getAppSettings().getPolicyId().equals(node2.getAppSettings().getPolicyId()))
                    notificationList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Web Filter " + i18nUtil.tr("and") + " Web Monitor" );
            }
        }

        for (App node1 : spamBlockerLiteList) {
            for (App node2 : spamblockerList) {
                if (node1.getAppSettings().getId().equals(node2.getAppSettings().getId()))
                    continue;

                if (node1.getAppSettings().getPolicyId().equals(node2.getAppSettings().getPolicyId()))
                    notificationList.add(i18nUtil.tr("One or more racks contain redundant apps") + ": " + " Spam Blocker " + i18nUtil.tr("and") + " Spam Blocker Lite" );
            }
        }
    }

    /**
     * This test iterates through each bridged interface and tests that the bridge its in is not
     * "plugged in backwards"
     * It does this by checking the location of the bridge's gateway
     */
    private void testBridgeBackwards(List<String> notificationList)
    {
        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!InterfaceSettings.ConfigType.BRIDGED.equals( intf.getConfigType() ))
                continue;

            logger.debug("testBridgeBackwards: Checking Bridge: " + intf.getSystemDev());
            logger.debug("testBridgeBackwards: Checking Bridge bridgedTo: " + intf.getBridgedTo());

            InterfaceSettings master = UvmContextFactory.context().networkManager().findInterfaceId(intf.getBridgedTo());
            if (master == null) {
                logger.warn("Unable to locate bridge master: " + intf.getBridgedTo());
                continue;
            }

            logger.debug("testBridgeBackwards: Checking Bridge master: " + master.getSystemDev());
            if (master.getSystemDev() == null) {
                logger.warn("Unable to locate bridge master systemName: " + master.getName());
                continue;
            }
            String bridgeName = master.getSymbolicDev();

            String result = this.execManager.execOutput( "brctl showstp " + bridgeName + " | grep '^eth.*' | sed -e 's/(//g' -e 's/)//g'");
            if (result == null || "".equals(result)) {
                logger.warn("Unable to build bridge map");
                continue;
            }
            logger.debug("testBridgeBackwards: brctlOutput: " + result);

            /**
             * Parse output brctl showstp output. Example:
             * eth0 2
             * eth1 1
             */
            Map<Integer,String> bridgeIdToSystemNameMap = new HashMap<Integer,String>();
            for (String line : result.split("\n")) {
                logger.debug("testBridgeBackwards: line: " + line);
                String[] subline = line.split(" ");
                if (subline.length < 2) {
                    logger.warn("Invalid brctl showstp line: \"" + line + "\"");
                    break;
                }
                Integer key;
                try {
                    key = Integer.parseInt(subline[1]);
                } catch (Exception e) {
                    logger.warn("Invalid output: " + subline[1]);
                    continue;
                }
                String systemName = subline[0];
                logger.debug("testBridgeBackwards: Map: " + key + " -> " + systemName);
                bridgeIdToSystemNameMap.put(key, systemName);
            }

            /**
             * This test is only valid for interfaces bridged to WANs.
             * Non-WANs don't have a gateway so there is no "backwards"
             */
            if ( ! master.getIsWan() )
                return;

            InetAddress gateway   = UvmContextFactory.context().networkManager().getInterfaceStatus( master.getInterfaceId() ).getV4Gateway();
            if (gateway == null) {
                logger.warn("Missing gateway on bridge master: " + master.getInterfaceId());
                return;
            }

            /**
             * Lookup gateway MAC using arp -a
             */
            String gatewayMac = this.execManager.execOutput( "arp -a " + gateway.getHostAddress() + " | awk '{print $4}' ");
            if ( gatewayMac == null ) {
                logger.warn("Unable to determine MAC for " + gateway.getHostAddress());
                return;
            }
            gatewayMac = gatewayMac.replaceAll("\\s+","");
            if ( "".equals(gatewayMac) || "entries".equals(gatewayMac)) {
                logger.warn("Unable to determine MAC for " + gateway.getHostAddress());
                return;
            }

            /**
             * Lookup gateway bridge port # using brctl showmacs
             */
            String portNo = this.execManager.execOutput( "brctl showmacs " + bridgeName + " | awk '/" + gatewayMac + "/ {print $1}'");
            if ( portNo == null) {
                logger.warn("Unable to find port number for MAC: " + gatewayMac);
                return;
            }
            portNo = portNo.replaceAll("\\s+","");
            if ( "".equals(portNo) ) {
                logger.warn("Unable to find port number for MAC: " + gatewayMac);
                return;
            }
            logger.debug("testBridgeBackwards: brctl showmacs Output: " + portNo);
            Integer gatewayPortNo = Integer.parseInt(portNo);
            logger.debug("testBridgeBackwards: Gateway Port: " + gatewayPortNo);


            /**
             * Lookup the system name for the bridge port
             */
            String gatewayInterfaceSystemName = bridgeIdToSystemNameMap.get(gatewayPortNo);
            logger.debug("testBridgeBackwards: Gateway Interface: " + gatewayInterfaceSystemName);
            if (gatewayInterfaceSystemName == null)  {
                logger.warn("Unable to find bridge port " + gatewayPortNo);
                return;
            }

            /**
             * Get the interface configuration for the interface where the gateway lives
             */
            InterfaceSettings gatewayIntf = UvmContextFactory.context().networkManager().findInterfaceSystemDev(gatewayInterfaceSystemName);
            if (gatewayIntf == null) {
                logger.warn("Unable to find gatewayIntf " + gatewayInterfaceSystemName);
                return;
            }
            logger.debug("testBridgeBackwards: Final Gateway Inteface: " + gatewayIntf.getName() + " is WAN: " + gatewayIntf.getIsWan());

            /**
             * Ideally, this is the WAN, however if its actually an interface bridged to a WAN, then the interfaces are probably backwards
             */
            if (!gatewayIntf.getIsWan()) {
                String notificationText = i18nUtil.tr("Bridge");
                notificationText += " (";
                notificationText += master.getName();
                notificationText += " <-> ";
                notificationText += intf.getName();
                notificationText += ") ";
                notificationText += i18nUtil.tr("may be backwards.");
                notificationText += " ";
                notificationText += i18nUtil.tr("Gateway");
                notificationText += " (";
                notificationText += gateway.getHostAddress();
                notificationText += ") ";
                notificationText += i18nUtil.tr("is on") + " ";
                notificationText += " ";
                notificationText += gatewayIntf.getName();
                notificationText += ".";

                notificationList.add(notificationText);
            }
        }
    }

    /**
     * This test iterates through each interface and tests for
     * TX and RX errors on each interface.
     * It creates notification if there are a "high number" of errors
     */
    private void testInterfaceErrors(List<String> notificationList)
    {
        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if ( intf.getSystemDev() == null )
                continue;

            String lines = this.execManager.execOutput( "ifconfig " + intf.getPhysicalDev() + " | awk '/errors/ {print $3}'");
            String type = "RX";  //first line is RX erros

            for (String line : lines.split("\n")) {
                line = line.replaceAll("\\s+","");
                logger.debug("testInterfaceErrors line: " + line);

                String[] errorsLine = line.split(":");
                if (errorsLine.length < 2)
                    continue;

                String errorsCountStr = errorsLine[1];
                Integer errorsCount;
                try { errorsCount = Integer.parseInt(errorsCountStr); } catch (NumberFormatException e) { continue; }

                /**
                 * Check for an arbitrarily high number of errors
                 * errors sometimes happen in small numbers and should be ignored
                 */
                if (errorsCount > 2500) {
                    String notificationText = "";
                    notificationText += intf.getName();
                    notificationText += " ";
                    notificationText += i18nUtil.tr("interface NIC card has a high number of");
                    notificationText += " " + type + " ";
                    notificationText += i18nUtil.tr("errors");
                    notificationText += " (";
                    notificationText += errorsCountStr;
                    notificationText += ")";
                    notificationText += ".";

                    notificationList.add(notificationText);
                }

                type = "TX"; // second line is TX
            }

        }
    }

    /**
     * This test tests to make sure public DNS servers are not used if spam blocking applications are installed
     */
    private void testSpamDNSServers(List<String> notificationList)
    {
        List<App> spamBlockerLiteList = UvmContextFactory.context().appManager().appInstances("spam-blocker-lite");
        List<App> spamblockerList = UvmContextFactory.context().appManager().appInstances("spam-blocker");
        String appName = "Spam Blocker";

        if (spamBlockerLiteList.size() == 0 && spamblockerList.size() == 0)
            return;
        if (spamBlockerLiteList.size() > 0)
            appName = "Spam Blocker Lite";
        if (spamblockerList.size() > 0)
            appName = "Spam Blocker";

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            List<String> dnsServers = new LinkedList<String>();
            if ( dnsPrimary != null ) dnsServers.add(dnsPrimary.getHostAddress());
            if ( dnsSecondary != null ) dnsServers.add(dnsSecondary.getHostAddress());

            for (String dnsServer : dnsServers) {
                /* hardcode common known bad DNS */
                if ( "8.8.8.8".equals( dnsServer ) || /* google */
                     "8.8.4.4".equals( dnsServer ) || /* google */
                     "4.2.2.1".equals( dnsServer ) || /* level3 */
                     "4.2.2.2".equals( dnsServer ) || /* level3 */
                     "208.67.222.222".equals( dnsServer ) || /* openDNS */
                     "208.67.222.220".equals( dnsServer ) /* openDNS */ ) {
                    String notificationText = "";
                    notificationText += appName + " " + i18nUtil.tr("is installed but an unsupported DNS server is used");
                    notificationText += " (";
                    notificationText += intf.getName();
                    notificationText += ", ";
                    notificationText += dnsServer;
                    notificationText += ").";

                    notificationList.add(notificationText);
                }
                /* otherwise check each DNS against spamhaus */
                else {
                    Lookup lookup;
                    Record[] records = null;
                    InetAddress expectedResult;

                    try {
                        lookup = new Lookup("2.0.0.127.zen.spamhaus.org");
                        expectedResult = InetAddress.getByName("127.0.0.4");
                    } catch ( Exception e ) {
                        logger.warn( "Invalid Lookup", e );
                        continue;
                    }
                    try {
                        lookup.setResolver( new SimpleResolver( dnsServer ) );
                        records = lookup.run();
                    } catch (Exception e) {
                        logger.warn("Invalid Resolver: " + dnsServer );
                    }

                    if ( records == null ) {
                        records = new Record[0];
                    }

                    boolean found = false;

                    found = false;
                    for (Record r : records) {
                        if (r instanceof ARecord) {
                            InetAddress addr = ((ARecord)r).getAddress();
                            if ( addr != null && addr.equals( expectedResult ) )
                                found = true;
                        }
                    }

                    if ( !found ) {
                        String notificationText = "";
                        notificationText += appName + " " + i18nUtil.tr("is installed but a DNS server");
                        notificationText += " (";
                        notificationText += intf.getName();
                        notificationText += ", ";
                        notificationText += dnsServer + ") ";
                        notificationText += i18nUtil.tr("fails to resolve DNSBL queries.");

                        notificationList.add(notificationText);
                    }
                }
            }
        }
    }

    /**
     * Tests that zvelo queries can be resolved correctly
     */
    @SuppressWarnings("rawtypes")
    private void testZveloDNSServers(List<String> notificationList)
    {
        List<App> webFilterList = UvmContextFactory.context().appManager().appInstances("web-filter");

        if ( webFilterList.size() == 0 )
            return;

        String query = null;
        try {
            Method method;
            App webFilter = webFilterList.get(0);

            Class[] args = { String.class, String.class };
            method = webFilter.getClass().getMethod( "encodeDnsQuery", args );
            query = (String) method.invoke( webFilter, "cnn.com", "/" );
        } catch (Exception e) {
            logger.warn("Exception generating Web Filter DNS query: ",e);
            return;
        }

        if ( query == null ) {
            logger.warn("Invalid zvelo query: " + query);
            return;
        }

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (!intf.getIsWan())
                continue;

            InetAddress dnsPrimary   = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();

            List<String> dnsServers = new LinkedList<String>();
            if ( dnsPrimary != null ) dnsServers.add(dnsPrimary.getHostAddress());
            if ( dnsSecondary != null ) dnsServers.add(dnsSecondary.getHostAddress());

            for (String dnsServer : dnsServers) {

                Lookup lookup;
                Record[] records = null;

                try {
                    lookup = new Lookup( query, Type.TXT );
                } catch ( Exception e ) {
                    logger.warn( "Invalid Lookup", e );
                    continue;
                }
                long t0 = System.currentTimeMillis();
                try {
                    lookup.setResolver( new SimpleResolver( dnsServer ) );
                    records = lookup.run();
                } catch (Exception e) {
                    logger.warn("Invalid Resolver: " + dnsServer );
                }
                long t1 = System.currentTimeMillis();

                if ( records == null ) {
                    records = new Record[0];
                }

                boolean found = false;
                for (Record r : records) {
                    if (r instanceof TXTRecord) {
                        for (Object o : ((TXTRecord)r).getStringsAsByteArrays()) {
                            String resultStr = new String((byte[])o);
                            //if there is a TXT response that includes cnn.com its probably correct
                            if (resultStr.contains("cnn.com"))
                                found = true;
                        }
                    }
                }

                if (!found) {
                    String notificationText = "";
                    notificationText += "Web Filter " + i18nUtil.tr("is installed but a DNS server");
                    notificationText += " (";
                    notificationText += intf.getName();
                    notificationText += ", ";
                    notificationText += dnsServer + ")";
                    notificationText += " " + i18nUtil.tr("fails to resolve categorization queries.");

                    logger.warn("DNS Lookup failed [" + dnsServer + ",TXT]: " + query);
                    notificationList.add(notificationText);
                } else if ( t1-t0 > 500 ) {
                    String notificationText = "";
                    notificationText += i18nUtil.tr("A DNS server responds slowly.");
                    notificationText += " (";
                    notificationText += intf.getName();
                    notificationText += ", ";
                    notificationText += dnsServer;
                    notificationText += ", ";
                    notificationText += (t1-t0) + " ms";
                    notificationText += ") ";
                    notificationText += i18nUtil.tr("This may negatively effect Web Filter performance.");

                    notificationList.add(notificationText);
                }
            }
        }
    }

    /**
     * This test that the event writing time on average is not "too" slow.
     */
    private void testEventWriteTime(List<String> notificationList)
    {
        final double MAX_AVG_TIME_WARN = 50.0;

        Reporting reports = (Reporting) UvmContextFactory.context().appManager().app("reports");
        /* if reports not installed - no events - just return */
        if (reports == null)
            return;

        double avgTime = reports.getAvgWriteTimePerEvent();
        if (avgTime > MAX_AVG_TIME_WARN) {
            String notificationText = "";
            notificationText += i18nUtil.tr("Event processing is slow");
            notificationText += " (";
            notificationText += String.format("%.1f",avgTime) + " ms";
            notificationText += "). ";

            notificationList.add(notificationText);
        }
    }

    /**
     * This test that the event writing delay is not too long
     */
    private void testEventWriteDelay(List<String> notificationList)
    {
        final long MAX_TIME_DELAY_SEC = 600; /* 10 minutes */

        Reporting reports = (Reporting) UvmContextFactory.context().appManager().app("reports");
        /* if reports not installed - no events - just return */
        if (reports == null)
            return;

        long delay = reports.getWriteDelaySec();
        if (delay > MAX_TIME_DELAY_SEC) {
            String notificationText = "";
            notificationText += i18nUtil.tr("Event processing is behind");
            notificationText += " (";
            notificationText += String.format("%.1f",(((float)delay)/60.0)) + " minute delay";
            notificationText += "). ";

            notificationList.add(notificationText);
        }
    }

    /**
     * This test tests for "nf_queue full" messages in kern.log
     */
    private void testQueueFullMessages(List<String> notificationList)
    {
        int result = this.execManager.execResult("tail -n 20 /var/log/kern.log | grep -q 'nf_queue:.*dropping packets'");
        if ( result == 0 ) {
            String notificationText = "";
            notificationText += i18nUtil.tr("Packet processing recently overloaded.");

            notificationList.add(notificationText);
        }
    }

    /**
     * This test that the shield is enabled
     */
    private void testShieldEnabled( List<String> notificationList )
    {
        App shield = UvmContextFactory.context().appManager().app("shield");
        String notificationText = "";
        notificationText += i18nUtil.tr("The shield is disabled. This can cause performance and stability problems.");

        if ( shield.getRunState() != AppSettings.AppState.RUNNING ) {
            notificationList.add(notificationText);
            return;
        }

        try {
            java.lang.reflect.Method method;
            method = shield.getClass().getMethod( "getSettings" );
            Object settings = method.invoke( shield );
            method = settings.getClass().getMethod( "isShieldEnabled" );
            Boolean result = (Boolean) method.invoke( settings );
            if (! result ) {
                notificationList.add(notificationText);
                return;
            }
        } catch (Exception e) {
            logger.warn("Exception reading shield settings: ",e);
        }
    }

    private void testRoutesToReachableAddresses( List<String> notificationList )
    {
        int result;
        List<StaticRoute> routes = UvmContextFactory.context().networkManager().getNetworkSettings().getStaticRoutes();

        for ( StaticRoute route : routes ) {
            if ( ! route.getToAddr() )
                continue;

            /**
             * If already in the ARP table, continue
             */
            result = this.execManager.execResult("arp -n " + route.getNextHop() + " | grep -q HWaddress");
            if ( result == 0 )
                continue;

            /**
             * If not, force arp resolution with ping
             * Then recheck ARP table
             */
            result = this.execManager.execResult("ping -c1 -W1 " + route.getNextHop());
            result = this.execManager.execResult("arp -n " + route.getNextHop() + " | grep -q HWaddress");
            if ( result == 0 )
                continue;

            String notificationText = "";
            notificationText += i18nUtil.tr("Route to unreachable address:");
            notificationText += " ";
            notificationText += route.getNextHop();

            notificationList.add(notificationText);
        }

    }

    private void testServerConf( List<String> notificationList )
    {
        try {
            String arch = System.getProperty("sun.arch.data.model") ;

            // only check 64-bit machines
            if ( arch == null || ! "64".equals( arch ) )
                return;

            // check total memory, return if unable to check total memory
            String result = this.execManager.execOutput( "awk '/MemTotal:/ {print $2}' /proc/meminfo" );
            if ( result == null )
                return;
            result = result.trim();
            if ( "".equals(result) )
                return;

            int memTotal = Integer.parseInt( result );
            if ( memTotal < 1900000 ) {
                String notificationText = i18nUtil.tr("Running 64-bit with less than 2 gigabytes RAM is not suggested.");
                notificationList.add(notificationText);
            }
        } catch (Exception e) {
            logger.warn("Exception testing system: ",e);
        }
    }

    private void testLicenseCompliance( List<String> notificationList )
    {
        int currentSize = UvmContextFactory.context().hostTable().getCurrentActiveSize();
        int seatLimit = UvmContextFactory.context().licenseManager().getSeatLimit( true );
        int actualSeatLimit = UvmContextFactory.context().licenseManager().getSeatLimit( false );

        if ( seatLimit > 0 && currentSize > seatLimit ) {
            String notificationText = i18nUtil.tr("Currently the number of devices significantly exceeds the number of licensed devices.") + " (" + currentSize + " > " + actualSeatLimit + ")";
            notificationList.add(notificationText);
        }
     }
        

}
