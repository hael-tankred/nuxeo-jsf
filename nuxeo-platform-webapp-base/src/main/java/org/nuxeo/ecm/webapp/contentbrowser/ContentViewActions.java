/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentView;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewCache;
import org.nuxeo.ecm.platform.ui.web.contentview.ContentViewService;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Handles cache and refresh for named content views.
 *
 * @author Anahide Tchertchian
 */
@Name("contentViewActions")
@Scope(CONVERSATION)
public class ContentViewActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected ContentViewService contentViewService;

    @In(create = true, required = true)
    protected transient CoreSession documentManager;

    protected final ContentViewCache cache = new ContentViewCache();

    protected Long globalPageSize;

    @In(create = true, required = false)
    protected FacesContext facesContext;

    protected ContentView currentContentView;

    /**
     * Returns the current global content view
     */
    public ContentView getCurrentContentView() {
        return currentContentView;
    }

    /**
     * Sets the current global content view
     */
    public void setCurrentContentView(ContentView cv) {
        currentContentView = cv;
    }

    /**
     * Returns the global page size, or returns the page size on current
     * content view if set.
     */
    public Long getCurrentGlobalPageSize() {
        if (globalPageSize == null && currentContentView != null) {
            return currentContentView.getCurrentPageSize();
        }
        return globalPageSize;
    }

    /**
     * Sets the global page size, useful to set the value having the
     * appropriate selection set, see {@link #getCurrentContentView()}
     */
    public void setCurrentGlobalPageSize(Long pageSize) {
        globalPageSize = pageSize;
    }

    /**
     * Returns the global page size
     */
    public Long getGlobalPageSize() {
        return globalPageSize;
    }

    /**
     * Sets the global page size
     */
    public void setGlobalPageSize(Long pageSize) {
        globalPageSize = pageSize;
    }

    public ContentView getContentView(String name) throws ClientException {
        return getContentView(name, null);
    }

    /**
     * Returns content view with given name, or null if no content view with
     * this name is found.
     * <p>
     * If parameter searchDocumentModel is not null, it will be set on the
     * content view. If it is null and the content is using a provider that
     * needs it, a new document model is created and attached to it. This
     * document model is resolved from the binding put in the content view XML
     * definition, or from the document type in this definition if no binding
     * is set.
     * <p>
     * If not null, this content view is set as the current content view so
     * that subsequent calls to other methods can take information from it,
     * like {@link #getCurrentGlobalPageSize()}
     * <p>
     * The content view is put in a cache map so that it's not rebuilt at each
     * call. It is rebuilt when its cache key changes (if defined).
     *
     * @throws ClientException
     */
    public ContentView getContentView(String name,
            DocumentModel searchDocumentModel) throws ClientException {
        ContentView cView = cache.get(name);
        if (cView == null) {
            cView = contentViewService.getContentView(name);
            if (cView != null) {
                cache.add(cView);
            }
        }
        if (cView != null) {
            if (searchDocumentModel != null) {
                cView.setSearchDocumentModel(searchDocumentModel);
            } else {
                DocumentModel doc = cView.getSearchDocumentModel();
                if (doc == null) {
                    String docType = cView.getSearchDocumentModelType();
                    if (docType != null) {
                        // initialize doc
                        DocumentModel blankDoc = documentManager.createDocumentModel(docType);
                        cView.setSearchDocumentModel(blankDoc);
                    }
                }
            }
            setCurrentContentView(cView);
        }
        return cView;
    }

    public ContentView getContentViewWithProvider(String name)
            throws ClientException {
        return getContentViewWithProvider(name, null, null, null, null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel) throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, null,
                null, null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage) throws ClientException {
        return getContentViewWithProvider(name, searchDocumentModel, sortInfos,
                pageSize, currentPage, (Object[]) null);
    }

    public ContentView getContentViewWithProvider(String name,
            DocumentModel searchDocumentModel, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Object... params)
            throws ClientException {
        ContentView cView = getContentView(name, searchDocumentModel);
        if (cView != null) {
            if (cView.getUseGlobalPageSize()) {
                cView.setCurrentPageSize(globalPageSize);
            }
            // initialize provider
            cView.getPageProvider(searchDocumentModel, sortInfos, pageSize,
                    currentPage, params);
        }
        return cView;
    }

    /**
     * Refreshes all content views that have declared the given seam event name
     * as a refresh event in their XML configuration.
     */
    public void refreshOnSeamEvent(String seamEventName) {
        cache.refreshOnEvent(seamEventName);
    }

    /**
     * Refreshes content views that have declared event
     * {@link EventNames#DOCUMENT_CHILDREN_CHANGED} as a refresh event.
     */
    @Observer(value = { EventNames.DOCUMENT_CHILDREN_CHANGED,
            EventNames.DOCUMENT_CHANGED })
    public void refreshOnDocumentChildrenChanged() {
        refreshOnSeamEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);
        refreshOnSeamEvent(EventNames.DOCUMENT_CHANGED);
    }

    public void refresh(String contentViewName) {
        cache.refresh(contentViewName, false);
    }

    public void refreshAndRewind(String contentViewName) {
        cache.refresh(contentViewName, true);
    }

    public void resetPageProvider(String contentViewName) {
        cache.resetPageProvider(contentViewName);
    }

    public void reset(String contentViewName) {
        cache.reset(contentViewName);
    }

}
