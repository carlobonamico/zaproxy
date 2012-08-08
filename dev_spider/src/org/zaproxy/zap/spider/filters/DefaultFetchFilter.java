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
package org.zaproxy.zap.spider.filters;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

/**
 * The DefaultFetchFilter is an implementation of a FetchFilter that is default for spidering
 * process. Its filter rules are the following:<br/>
 * <ul>
 * <li>the resource protocol/scheme must be 'http' or 'https'.</li>
 * <li>the resource must be found in the scope (domain) of the spidering process.</li>
 * <li>the resource must be not be excluded by user request - exclude list.</li>
 * </ul>
 * 
 */
public class DefaultFetchFilter extends FetchFilter {

	/** The scope. */
	private LinkedList<String> scopes = new LinkedList<String>();

	/** The exclude list. */
	private List<String> excludeList = null;

	/* (non-Javadoc)
	 * 
	 * @see
	 * org.zaproxy.zap.spider.filters.FetchFilter#checkFilter(org.apache.commons.httpclient.URI) */
	@Override
	public FetchStatus checkFilter(URI uri) {

		log.info("Checking: "+uri);
		// Protocol check
		String scheme = uri.getScheme();
		if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
			return FetchStatus.ILLEGAL_PROTOCOL;

		try {
			// Scope check
			boolean ok = false;
			for (String scope : scopes)
				if (uri.getHost().endsWith(scope)) {
					ok = true;
					break;
				}
			if (!ok)
				return FetchStatus.OUT_OF_SCOPE;

			// Check if any of the exclusion regexes match.
			if (excludeList != null) {
				String uriS = uri.toString();
				for (String ex : excludeList)
					if (uriS.matches(ex))
						return FetchStatus.USER_RULES;
			}

		} catch (URIException e) {
			log.warn("Error while fetching host for uri: " + uri, e);
			return FetchStatus.OUT_OF_SCOPE;
		}

		return FetchStatus.VALID;
	}

	/**
	 * Adds a new domain to the scope list of the spider process.
	 * 
	 * @param scope the scope
	 */
	public void addScopeDomain(String scope) {
		this.scopes.add(scope);
	}

	/**
	 * Sets the regexes which are used for checking if an uri should be skipped.
	 * 
	 * @param excl the new exclude regexes
	 */
	public void setExcludeRegexes(List<String> excl) {
		excludeList = excl;
	}

}