/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:nchilders@metavize.com">Nick Childers</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_EVT"
 * mutable="false"
 */
public class IDSLogEvent extends PipelineEvent {

    private String message;
    private boolean blocked;
    private int ruleSid;

    // constructors -----------------------------------------------------------

    public IDSLogEvent() { }

    public IDSLogEvent(PipelineEndpoints pe, int ruleSid, String message, boolean blocked) {
        super(pe);

        this.ruleSid = ruleSid;
        this.message = message;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * SID of the rule that fired.
     *
     * @hibernate.property
     * column="RULE_SID"
     */
    public int getRuleSid() { return this.ruleSid; }
    public void setRuleSid(int sid) { this.ruleSid = ruleSid; }	

    /**
     * Message of signature that generated this event.
     *
     * @return the message
     * @hibernate.property
     * column="MESSAGE"
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Was it blocked.
     *
     * @return whether or not the session was blocked (closed)
     * @hibernate.property
     * column="BLOCKED"
     */
      public boolean isBlocked() {
          return blocked;
      }

      public void setBlocked(boolean blocked) {
          this.blocked = blocked;
      }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("message", message);
        sb.addField("blocked", blocked);
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return blocked ? SyslogPriority.NOTICE : SyslogPriority.INFORMATIONAL;
    }

    // Object methods ---------------------------------------------------------

    public String toString() {
        return "IDSLogEvent id: " + getId() + " Message: " + message;
    }
}
