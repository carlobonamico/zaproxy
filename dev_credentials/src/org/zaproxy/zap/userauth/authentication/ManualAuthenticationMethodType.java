/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2013 The ZAP Development Team
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
package org.zaproxy.zap.userauth.authentication;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.model.Session;
import org.zaproxy.zap.extension.httpsessions.ExtensionHttpSessions;
import org.zaproxy.zap.extension.httpsessions.HttpSession;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.userauth.User;
import org.zaproxy.zap.userauth.session.SessionManagementMethod;
import org.zaproxy.zap.userauth.session.WebSession;
import org.zaproxy.zap.view.LayoutHelper;

/**
 * The implementation for an {@link AuthenticationMethodType} where the user manually authenticates
 * and then just selects an already authenticated {@link WebSession}.
 */
public class ManualAuthenticationMethodType extends AuthenticationMethodType {

	private static final int METHOD_IDENTIFIER = 0;

	/** The Authentication method's name. */
	private static final String METHOD_NAME = Constant.messages
			.getString("authentication.method.manual.name");

	/**
	 * The implementation for an {@link AuthenticationMethod} where the user manually authenticates
	 * and then just selects an already authenticated {@link WebSession}.
	 */
	public static class ManualAuthenticationMethod extends AuthenticationMethod {
		private int contextId;

		public ManualAuthenticationMethod(int contextId) {
			super();
			this.contextId = contextId;
		}

		protected int getContextId() {
			return contextId;
		}

		@Override
		public boolean isConfigured() {
			// Nothing to configure
			return true;
		}

		@Override
		public AuthenticationCredentials createAuthenticationCredentials() {
			return new ManualAuthenticationCredentials();
		}

		@Override
		public WebSession authenticate(SessionManagementMethod sessionManagementMethod,
				AuthenticationCredentials credentials, User user) {
			// Check proper type
			if (!(credentials instanceof ManualAuthenticationCredentials)) {
				Logger.getLogger(ManualAuthenticationMethod.class).error(
						"Manual authentication credentials should be used for Manual authentication.");
				throw new UnsupportedAuthenticationCredentialsException(
						"Manual authentication credentials should be used for Manual authentication.");
			}

			ManualAuthenticationCredentials mc = (ManualAuthenticationCredentials) credentials;
			return mc.getSelectedSession();
		}

		@Override
		public AuthenticationMethodType getType() {
			return new ManualAuthenticationMethodType();
		}

		@Override
		public AuthenticationMethod duplicate() {
			return new ManualAuthenticationMethod(contextId);
		}

		@Override
		public void onMethodPersisted() {
			// Do nothing
		}

		@Override
		public void onMethodDiscarded() {
			// Do nothing
		}
	}

	/**
	 * A credentials implementation that allows users to manually select an existing
	 * {@link WebSession} that corresponds to an already authenticated session.
	 */
	private static class ManualAuthenticationCredentials implements AuthenticationCredentials {

		private WebSession selectedSession;

		protected WebSession getSelectedSession() {
			return selectedSession;
		}

		@Override
		public boolean isConfigured() {
			return selectedSession != null;
		}

		protected void setSelectedSession(WebSession selectedSession) {
			this.selectedSession = selectedSession;
		}

		@Override
		public String encode(String parentStringSeparator) {
			return Base64.encodeBase64String(selectedSession.getName().getBytes());
		}

		@Override
		public void decode(String encodedCredentials) {
			// TODO: Currently, cannot be decoded as HttpSessions are not persisted.
			throw new IllegalStateException("Manual Authentication Credentials cannot be decoded.");
		}
	}

	/**
	 * The option panel for configuring {@link ManualAuthenticationCredentials} objects.
	 */
	private static class ManualAuthenticationCredentialsOptionsPanel extends
			AbstractCredentialsOptionsPanel<ManualAuthenticationCredentials> {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = -8081914793980311435L;

		private static final Logger log = Logger.getLogger(ManualAuthenticationCredentialsOptionsPanel.class);
		private JComboBox<HttpSession> sessionsComboBox;
		private Context uiSharedContext;

		public ManualAuthenticationCredentialsOptionsPanel(ManualAuthenticationCredentials credentials,
				Context uiSharedContext) {
			super(credentials);
			this.uiSharedContext = uiSharedContext;
			initialize();
		}

		/**
		 * Initialize the panel.
		 */
		@SuppressWarnings("unchecked")
		protected void initialize() {
			this.setLayout(new GridBagLayout());

			JLabel sessionsLabel = new JLabel(
					Constant.messages.getString("authentication.method.manual.field.session"));

			this.add(sessionsLabel, LayoutHelper.getGBC(0, 0, 1, 0.5D));
			this.add(getSessionsComboBox(), LayoutHelper.getGBC(1, 0, 1, 0.5D));
			this.getSessionsComboBox().setRenderer(new HttpSessionRenderer());
		}

		/**
		 * A renderer for properly displaying the name of an HttpSession in a ComboBox.
		 */
		private static class HttpSessionRenderer extends BasicComboBoxRenderer {
			private static final long serialVersionUID = 3654541772447187317L;

			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value != null) {
					HttpSession item = (HttpSession) value;
					setText(item.getName());
				}
				return this;
			}
		}

		private JComboBox<HttpSession> getSessionsComboBox() {
			if (sessionsComboBox == null) {
				ExtensionHttpSessions extensionHttpSessions = (ExtensionHttpSessions) Control.getSingleton()
						.getExtensionLoader().getExtension(ExtensionHttpSessions.NAME);
				List<HttpSession> sessions = extensionHttpSessions.getHttpSessionsForContext(uiSharedContext);
				if (log.isDebugEnabled())
					log.debug("Found sessions for Manual Authentication Config: " + sessions);
				sessionsComboBox = new JComboBox<>(sessions.toArray(new HttpSession[sessions.size()]));
				sessionsComboBox.setSelectedItem(this.getCredentials().getSelectedSession());
			}
			return sessionsComboBox;
		}

		@Override
		public boolean validateFields() {
			if (sessionsComboBox.getSelectedIndex() < 0) {
				JOptionPane.showMessageDialog(this, Constant.messages
						.getString("authentication.method.manual.dialog.error.nosession.text"),
						Constant.messages.getString("authentication.method.manual.dialog.error.title"),
						JOptionPane.WARNING_MESSAGE);
				sessionsComboBox.requestFocusInWindow();
				return false;
			}
			return true;
		}

		@Override
		public void saveCredentials() {
			log.info("Saving Manual Authentication Method: " + getSessionsComboBox().getSelectedItem());
			getCredentials().setSelectedSession((WebSession) getSessionsComboBox().getSelectedItem());
		}
	}

	@Override
	public String getName() {
		return METHOD_NAME;
	}

	@Override
	public boolean hasOptionsPanel() {
		// No options panel for the method
		return false;
	}

	@Override
	public boolean hasCredentialsOptionsPanel() {
		return true;
	}

	@Override
	public ManualAuthenticationMethod createAuthenticationMethod(int contextId) {
		return new ManualAuthenticationMethod(contextId);
	}

	@Override
	public AbstractAuthenticationMethodOptionsPanel buildOptionsPanel(Context uiSharedContext) {
		// Not needed
		return null;
	}

	@Override
	public AbstractCredentialsOptionsPanel<? extends AuthenticationCredentials> buildCredentialsOptionsPanel(
			AuthenticationCredentials credentials, Context uiSharedContext) {
		return new ManualAuthenticationCredentialsOptionsPanel((ManualAuthenticationCredentials) credentials,
				uiSharedContext);
	}

	@Override
	public boolean isTypeForMethod(AuthenticationMethod method) {
		return (method instanceof ManualAuthenticationMethod);
	}

	@Override
	public void hook(ExtensionHook extensionHook) {
		// Do nothing
	}

	@Override
	public AuthenticationMethod loadMethodFromSession(Session session, int contextId) {
		return new ManualAuthenticationMethod(contextId);
	}

	@Override
	public void persistMethodToSession(Session session, int contextId, AuthenticationMethod authMethod) {
		// Nothing to persist
	}

	@Override
	public int getUniqueIdentifier() {
		return METHOD_IDENTIFIER;
	}

	@Override
	public AuthenticationCredentials createAuthenticationCredentials() {
		return new ManualAuthenticationCredentials();
	}

}