package com.neotys.neoload.model.readers.loadrunner.method;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.neotys.neoload.model.core.Element;
import com.neotys.neoload.model.parsers.CPP14Parser.MethodcallContext;
import com.neotys.neoload.model.readers.loadrunner.LoadRunnerVUVisitor;
import com.neotys.neoload.model.readers.loadrunner.MethodCall;
import com.neotys.neoload.model.readers.loadrunner.MethodUtils;
import com.neotys.neoload.model.repository.ImmutableAddCookie;
import com.neotys.neoload.model.repository.ImmutableAddCookie.Builder;
public class WebAddCookieMethod implements LoadRunnerMethod {
		
	private static final Logger LOGGER = LoggerFactory.getLogger(WebAddCookieMethod.class);
	
	public WebAddCookieMethod() {
		super();
	}

	@Override
	public Element getElement(final LoadRunnerVUVisitor visitor, final MethodCall method, final MethodcallContext ctx) {
		Preconditions.checkNotNull(method);
		if(method.getParameters() == null || method.getParameters().isEmpty()){
			visitor.readSupportedFunctionWithWarn(method.getName(), ctx, METHOD + method.getName() + " should have at least 1 parameter.");
			return null;
		}
		final String cookie = MethodUtils.normalizeString(visitor.getLeftBrace(), visitor.getRightBrace(), method.getParameters().get(0));
		final List<HttpCookie> httpCookies;
		try{
			httpCookies = HttpCookie.parse(cookie);
		} catch (final Exception exception){
			final String warnMessage = "Cannot parse cookie (" + cookie + "): " + exception.getMessage();
			visitor.readSupportedFunctionWithWarn(method.getName(), ctx, warnMessage);
			LOGGER.warn(warnMessage, exception);
			return null;
		}
		if(httpCookies == null || httpCookies.size() != 1){
			visitor.readSupportedFunctionWithWarn(method.getName(), ctx, "Cannot parse cookie (" + cookie + ").");			
			return null;
		}	
		visitor.readSupportedFunction(method.getName(), ctx);
		final HttpCookie httpCookie = httpCookies.get(0);
		final String cookieName = httpCookie.getName();
		final String cookieValue = httpCookie.getValue();
		final String cookieDomain = httpCookie.getDomain();
		final String name = "Set cookie " + cookieName + " for server " + cookieDomain;
		final Builder builder = ImmutableAddCookie.builder()
				.name(name)
				.cookieName(cookieName)
				.cookieValue(cookieValue);
		URL url;
		try{
			url = new URL(cookieDomain);
		} catch(final MalformedURLException e1){
			try{
				url = new URL("http://" + cookieDomain);
			} catch(final MalformedURLException e2){
				url = null;
			}			
		}
		if(url != null){
			builder.server(visitor.getReader().getServer(url));
		}
		final long maxAge = httpCookie.getMaxAge();
		if(maxAge != -1L){
			builder.expires(Long.toString(maxAge));
		}	
		return builder.build();
	}
}
