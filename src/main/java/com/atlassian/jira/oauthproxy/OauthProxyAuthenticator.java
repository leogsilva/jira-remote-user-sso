package com.atlassian.jira.oauthproxy;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.seraph.auth.AuthenticatorException;
import com.atlassian.jira.security.login.JiraSeraphAuthenticator; 

import java.security.Principal;

/*
Biel Bienne Authenticator russo: 
Authenticating to Jira with the X_Forwarded_User HTTP header
Copyright (C) 2014  Christian Loosli

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * Extension of DefaultAuthenticator that uses the Apache set X-Forwarded-User
 * header in a HTTPRequest object for single sign on. 
 * @author Christian Loosli
 *
 */
public class OauthProxyAuthenticator extends JiraSeraphAuthenticator
{

	// Header we read. Has to be lowercase even if the header is set uppercase in apache
	private static final long serialVersionUID = 1807345345435345234L;
	private static final Logger log = Logger.getLogger(OauthProxyAuthenticator.class);
	private static final String COOKIE_NAME = "_oauthproxy";
	
	// Print additional information and warnings, useful when developing, else it just spams the logs a bit. 
	private static final boolean useDebug = true; 

	/**
	 * Default method getting the user, first calls the JIRA based method, then checks 
	 * for X-Forwarded-User in the header. This should ensure that everything using
	 * other methods than Apache Kerberos Auth should still work, but in addition to that, 
	 * the header set after Kerberos auth will be considered and should also allow a log-in. 
	 * 
	 * @param request The request containing the headers
	 * @param response The response sent
	 * @return The user principal, can be null if authentication failed. 
	 */
	public Principal getUser(HttpServletRequest request, HttpServletResponse response)
	{

		Principal user = null; 
		String username = null;
		try
		{
			// This shall also take care of the user already being logged in, as the parent checks that. 
			Cookie[] cookies = request.getCookies();
			Cookie proxyCookie = null;
			for (Cookie c : cookies) {
				if (COOKIE_NAME.equals(c.getName())) {
					proxyCookie = c;
					break;
				}
			}
			
			if (proxyCookie != null) {
				String value = proxyCookie.getValue();
//				String[] tokens = value.split("|");
				byte[] decodeBase64 = Base64.decodeBase64(value);
				if (useDebug) {
					log.info("Cookie username value:" + value + ",base64 : " + new String(decodeBase64));
				}
				username = new String(decodeBase64);
				user = getUser(username);
			}
			// Neither an already existing user nor a forwarded one in the header. 
			// This will return null, which should have JIRA redirect the user to the configurated login page
			if ( (user == null) && (username == null))
	        {
				if(useDebug)
				{
					log.warn("No user or username found");
				}
	            return user;
	        }
			
			if(useDebug)
			{
				log.info("Got " + username + " from cookie " + COOKIE_NAME);
			}
			
			if (user != null)
	        {
	            if ( (username != null) && (user.getName().equals(username)))
	            {
	                return user;
	            }
	            else
	            {
	            	if(useDebug)
	            	{
	            		log.warn("Session found; different user already logged in. Using that user : " + user.getName());
	            	}
	            	return user; 
	            }
	        }
			
			try
			{
				user = super.getUser(username);
				if(useDebug)
				{
					log.info("Got " + user + " from the Jira Authenticator");
				}
			}
			catch (Exception e)
			{
				log.error("Exception caught: " + e.getMessage() + " :: " ,e);
			}
			        
	        return user;
		} 
		catch (Exception e) // catch class cast exceptions
		{
			log.error("Exception caught: " + e.getMessage() + " :: ",e);
			return user; 
		}
	}

	@Override
	protected boolean authenticate(Principal pPrincipal, String pStrPwd) 
			throws AuthenticatorException
	{
		return super.authenticate(pPrincipal, pStrPwd);
	}

	@Override
	protected Principal getUser(String pStrUsername)
	{
		return super.getUser(pStrUsername);
	}

}