/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.Manager;

@Startup
@Name("conversationIdGenerator")
@Scope(SESSION)
public class ConversationIdGenerator implements Serializable {

    private static final long serialVersionUID = 15643987456876L;

    private static final String MAIN_CONVERSATION_PREFIX = "0NXMAIN";

    private static final String CREATE_CONVERSATION_PREFIX = "0NXCREATE";

    private int mainConversationCounter = 0;

    private int createConversationCounter = 0;

    @In(value = "org.jboss.seam.core.manager")
    public transient Manager conversationManager;

    public String getNextMainConversationId() {
        String newMainConversationId;
        if (mainConversationCounter == 0) {
            newMainConversationId = MAIN_CONVERSATION_PREFIX;
        } else {
            newMainConversationId = MAIN_CONVERSATION_PREFIX
                    + mainConversationCounter;
        }
        mainConversationCounter += 1;
        return newMainConversationId;
    }

    public String getNextCreateConversationId() {
        String newCreateConversationId;
        if (createConversationCounter == 0) {
            newCreateConversationId = CREATE_CONVERSATION_PREFIX;
        } else {
            newCreateConversationId = CREATE_CONVERSATION_PREFIX
                    + createConversationCounter;
        }
        createConversationCounter += 1;
        return newCreateConversationId;
    }

    public String getCurrentOrNewMainConversationId() {

        // this case can happend if user logged in from a bookmarked URL that
        // contains conversation ID
        String existingConversationId = getConversationIdInURL();
        if (existingConversationId != null
                && existingConversationId.startsWith(MAIN_CONVERSATION_PREFIX)) {
            return existingConversationId;
        }

        if (conversationManager.isReallyLongRunningConversation()) {
            existingConversationId = conversationManager.getCurrentConversationId();
            if (existingConversationId.startsWith(MAIN_CONVERSATION_PREFIX)) {
                return existingConversationId;
            } else {
                return getNextMainConversationId();
            }
        } else {
            return getNextMainConversationId();
        }
    }

    protected String getConversationIdInURL() {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }
        Map<String, Object> rMap = facesContext.getExternalContext().getRequestMap();
        String conversationIdInUrl;
        if (rMap.containsKey(conversationManager.getConversationIdParameter())) {
            conversationIdInUrl = (String) rMap.get(conversationManager.getConversationIdParameter());
        } else {
            Map<String, String> pMap = facesContext.getExternalContext().getRequestParameterMap();
            if (pMap.containsKey(conversationManager.getConversationIdParameter())) {
                conversationIdInUrl = pMap.get(conversationManager.getConversationIdParameter());
            } else {
                return null;
            }
        }
        return conversationIdInUrl;
    }

}
