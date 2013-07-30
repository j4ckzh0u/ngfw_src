/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.quarantine.InboxIndex;
import com.untangle.node.smtp.quarantine.InboxRecord;

//========================================================
// For add/remove operations, we make a copy
// of our "m_summary" reference, so reads
// do not have to be synchronized.  This assumes
// that lookups of account <-> dir are much more
// frequent (at steady state) than additions/removals
// of accounts.

/**
 * Manager of the master records for mapping users to folders, as well as
 * tracking overall size of the store. <br>
 * <br>
 * Assumes that caller has locked any addresses being referenced, unless
 * otherwise noted.
 */
final class MasterTable
{

    private final Logger m_logger = Logger.getLogger(getClass());

    private String m_rootDir;
    private StoreSummary m_summary;
    private volatile boolean m_closing = false;

    private MasterTable(String dir, StoreSummary initialSummary) {
        m_rootDir = dir;
        m_summary = initialSummary;
    }

    /**
     * Open the MasterTable. The InboxDirectoryTree is needed in case the system
     * closed abnormally and the StoreSummary needs to be rebuilt.
     */
    static MasterTable open(String rootDir, InboxDirectoryTree dirTracker)
    {
        Logger logger = Logger.getLogger(MasterTable.class);

        StoreSummary summary = QuarantineStorageManager.readSummary(rootDir);

        boolean needRebuild = (summary == null || summary.getTotalSz() == 0);

        if (needRebuild) {
            RebuildingVisitor visitor = new RebuildingVisitor();
            logger.debug("About to scan Inbox directories to rebuild summary");
            dirTracker.visitInboxes(visitor);
            logger.debug("Done scanning Inbox directories to rebuild summary");
            return new MasterTable(rootDir, visitor.getSummary());
        } else {
            // QuarantineStorageManager.openSummary(rootDir);
            return new MasterTable(rootDir, summary);
        }
    }

    static MasterTable rebuild(String rootDir, InboxDirectoryTree dirTracker)
    {
        Logger logger = Logger.getLogger(MasterTable.class);
        RebuildingVisitor visitor = new RebuildingVisitor();
        logger.debug("About to scan Inbox directories to rebuild summary");
        dirTracker.visitInboxes(visitor);
        logger.debug("Done scanning Inbox directories to rebuild summary");
        return new MasterTable(rootDir, visitor.getSummary());
    }

    boolean inboxExists(String lcAddress)
    {
        return m_summary.containsInbox(lcAddress);
    }

    /**
     * Assumes caller has already found that there is no such inbox, while
     * holding the master lock for this account
     */
    synchronized void addInbox(String address)
    {

        StoreSummary newSummary = new StoreSummary(m_summary);
        newSummary.addInbox(address, new InboxSummary(address));
        m_summary = newSummary;
        save();
    }

    /**
     *
     */
    synchronized void removeInbox(String address)
    {
        StoreSummary newSummary = new StoreSummary(m_summary);
        newSummary.removeInbox(address);
        m_summary = newSummary;
        save();
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     */
    synchronized boolean mailAdded(String address, long sz)
    {
        InboxSummary meta = m_summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        m_summary.mailAdded(meta, sz);
        save();
        return true;
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     */
    synchronized boolean mailRemoved(String address, long sz)
    {
        InboxSummary meta = m_summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        m_summary.mailRemoved(meta, sz);
        save();
        return true;
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     */
    synchronized boolean updateMailbox(String address, long totalSz, int totalMails)
    {
        InboxSummary meta = m_summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        m_summary.updateMailbox(meta, totalSz, totalMails);
        save();
        return true;
    }

    /**
     * Get the sum total of the lengths of all mails across all inboxes.
     */
    long getTotalQuarantineSize()
    {
        return m_summary.getTotalSz();
    }

    /**
     * Get the total number of mails in all inboxes
     */
    int getTotalNumMails()
    {
        return m_summary.getTotalMails();
    }

    /**
     * Get the total number of inboxes
     */
    int getTotalInboxes()
    {
        return m_summary.size();
    }

    /**
     * Close this table, causing data to be written out to disk. Any subsequent
     * (stray) calls to this object will also cause an update of the on-disk
     * representation of state.
     */
    synchronized void close()
    {
        m_closing = true;
        save();
    }

    private void save()
    {
        if (!QuarantineStorageManager.writeSummary(m_summary, m_rootDir)) {
            m_logger.warn("Unable to save StoreSummary.  Next startup " + "will have to rebuild index");
        }
    }

    /**
     * // * Returns null if not found. Note that since this is not // *
     * synchronized, one should call this while holding the // * master account
     * lock to ensure that concurrent // * creation doesn't take place. //
     */
    // RelativeFileName getInboxDir(String address) {
    // InboxSummary meta = m_summary.getInbox(address);
    // return meta==null?
    // null:
    // meta.getDir();
    // }

    /**
     * Do not modify any of the returned entries, as it is a shared reference.
     * The returned set itself is guaranteed never to be modified.
     */
    Set<Map.Entry<String, InboxSummary>> entries()
    {
        return m_summary.entries();
    }

    // -------------- Inner Class ---------------------

    // Class used when we have to visit
    // all directories in the Inbox tree and
    // rebuild our index.
    private static class RebuildingVisitor implements InboxDirectoryTreeVisitor
    {

        private StoreSummary m_storeMeta = new StoreSummary();

        public void visit(File f)
        {
            String emailAddress = f.getName();
            InboxIndex inboxIndex = QuarantineStorageManager.readQuarantine(emailAddress, f.getAbsolutePath());
            if (inboxIndex != null) {
                long totalSz = 0;
                int totalMails = 0;
                for (InboxRecord record : inboxIndex) {
                    totalSz += record.getSize();
                    totalMails++;
                }
                m_storeMeta.addInbox(inboxIndex.getOwnerAddress(), new InboxSummary(f.getName(), totalSz, totalMails));
            }
        }

        StoreSummary getSummary()
        {
            return m_storeMeta;
        }

    }

}
