/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.mail.papi;

import java.util.List;

import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.EmailAddressWithRcptType;
import com.untangle.node.mime.MIMEMessageHeaders;
import com.untangle.node.mime.RcptType;
import com.untangle.uvm.node.PipelineEndpoints;

public class MessageInfoFactory
{
    private MessageInfoFactory() { }

    /**
     * Helper MessageInfo factory method which constructs a MessageInfo from
     * the contents of a MIME message.  Moved here because xdoclet doesn't let
     * it be in MessageInfo.java.
     */
    public static MessageInfo fromMIMEMessage(MIMEMessageHeaders headers,
                                              PipelineEndpoints pe,
                                              int port) {

        MessageInfo ret = new MessageInfo(pe, port, headers.getSubject());

        //Drain all TO and CC
        List<EmailAddressWithRcptType> allRcpts = headers.getAllRecipients();
        for(EmailAddressWithRcptType eawrt : allRcpts) {
            if(!eawrt.address.isNullAddress()) {
                ret.addAddress(
                               ((eawrt.type == RcptType.TO)?AddressKind.TO:AddressKind.CC),
                               eawrt.address.getAddress(),
                               eawrt.address.getPersonal());
            }
        }

        //Drain FROM
        EmailAddress from = headers.getFrom();
        if(from != null &&
           !from.isNullAddress()) {
            ret.addAddress(
                           AddressKind.FROM,
                           from.getAddress(),
                           from.getPersonal());
        }
        return ret;
    }
}
