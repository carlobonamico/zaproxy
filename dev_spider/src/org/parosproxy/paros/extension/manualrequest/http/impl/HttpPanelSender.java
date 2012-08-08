/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
// ZAP: 2012/07/02 Introduced new class. Moved code from class
// ManualRequestEditorDialog to here (HistoryList).
// ZAP: 2012/07/29 Issue 43: Cleaned up access to ExtensionHistory UI

package org.parosproxy.paros.extension.manualrequest.http.impl;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.extension.manualrequest.MessageSender;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpSender;
import org.zaproxy.zap.extension.httppanel.HttpPanel;
import org.zaproxy.zap.extension.httppanel.HttpPanelRequest;
import org.zaproxy.zap.extension.httppanel.HttpPanelResponse;
import org.zaproxy.zap.extension.httppanel.Message;

/**
 * Knows how to send {@link HttpMessage} objects.
 */
public class HttpPanelSender implements MessageSender {

    private static final Logger logger = Logger.getLogger(HttpPanelSender.class);
    
    private final HttpPanelResponse responsePanel;
    private final ExtensionHistory extension;

    private HttpSender delegate;
    
    private JToggleButton followRedirect = null;
    private JToggleButton useTrackingSessionState = null;

    public HttpPanelSender(HttpPanelRequest requestPanel, HttpPanelResponse responsePanel) {
        this.responsePanel = responsePanel;
        
        requestPanel.addOptions(getButtonUseTrackingSessionState(), HttpPanel.OptionsLocation.AFTER_COMPONENTS);
        requestPanel.addOptions(getButtonFollowRedirects(), HttpPanel.OptionsLocation.AFTER_COMPONENTS);

        final boolean isSessionTrackingEnabled = Model.getSingleton().getOptionsParam().getConnectionParam().isHttpStateEnabled();
        getButtonUseTrackingSessionState().setEnabled(isSessionTrackingEnabled);
        
        this.extension = ((ExtensionHistory)Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.NAME));
    }
    
    /* (non-Javadoc)
     * @see org.parosproxy.paros.extension.manualrequest.MessageSender#sendAndReceiveMessage()
     */
    @Override
    public void handleSendMessage(Message aMessage) throws Exception {
        final HttpMessage httpMessage = (HttpMessage)aMessage;
        try {
            httpMessage.getRequestHeader().setContentLength(httpMessage.getRequestBody().length());
            getDelegate().sendAndReceive(httpMessage, getButtonFollowRedirects().isSelected());

            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (!httpMessage.getResponseHeader().isEmpty()) {
                        // Indicate UI new response arrived
                        responsePanel.updateContent();

                        final int finalType = HistoryReference.TYPE_MANUAL;
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                extension.addHistory(httpMessage, finalType);
                            }
                        });
                        t.start();
                    }
                }
            });
        } catch (final HttpMalformedHeaderException mhe) {
            throw new Exception("Malformed header error.");
        } catch (final IOException ioe) {
            throw new Exception("IO error in sending request.");
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.parosproxy.paros.extension.manualrequest.MessageSender#cleanup()
     */
    @Override
    public void cleanup() {
        if (delegate != null) {
            delegate.shutdown();
            delegate = null;
        }

        final boolean isSessionTrackingEnabled = Model.getSingleton().getOptionsParam().getConnectionParam().isHttpStateEnabled();
        getButtonUseTrackingSessionState().setEnabled(isSessionTrackingEnabled);
    }
    
    private HttpSender getDelegate() {
        if (delegate == null) {
            delegate = new HttpSender(Model.getSingleton().getOptionsParam().getConnectionParam(), 
            				getButtonUseTrackingSessionState().isSelected(),
            				HttpSender.MANUAL_REQUEST_INITIATOR);
        }
        return delegate;
    }
    
    private JToggleButton getButtonFollowRedirects() {
        if (followRedirect == null) {
            followRedirect = new JToggleButton(new ImageIcon(HttpPanelSender.class.getResource("/resource/icon/16/118.png"))); // Arrow turn around left
            followRedirect.setToolTipText(Constant.messages.getString("manReq.checkBox.followRedirect"));
            followRedirect.setSelected(true);
        }
        return followRedirect;
    }

    private JToggleButton getButtonUseTrackingSessionState() {
        if (useTrackingSessionState == null) {
            useTrackingSessionState = new JToggleButton(new ImageIcon(HttpPanelSender.class.getResource("/resource/icon/fugue/cookie.png"))); // Cookie
            useTrackingSessionState.setToolTipText(Constant.messages.getString("manReq.checkBox.useSession"));
        }
        return useTrackingSessionState;
    }
    
}