/*
 * NextBill server application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.domain.config;

import de.nextbill.domain.services.SettingsService;
import de.nextbill.oauth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
@EnableWebSecurity
@Order(99)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private CustomUserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SettingsService settingsService;

	@Value("${security.xsrfTokenName}")
	private String xsrfTokenName;

	private static final String[] RESOURCE_LOCATIONS = {"/public/**", "/i18n/**", "/webjars/**", "/online/**", "/favicon.ico", "/webapp/api/mobileDevices/delete/*", "/logout", "/login/**","/oauth/**","/css/**", "/third-party-report.html",
			"/node_modules/**", "/js/**","/vendor/**", "/img/**","/error/**","/index.html", "/dist/**", "/app/**" , "/fonts/**", "/favicon.ico", "/webapp/api/settings/isCustomized", "/webapp/api/settings/searchForUpdate", "/webapp/api/settings/initSetupData" };

	@Override
	public void configure( WebSecurity web ) throws Exception {
		web.ignoring().antMatchers( HttpMethod.OPTIONS, "/**" );
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		LogoutHandler logoutSuccessHandler = new LogoutHandler();

//		CharacterEncodingFilter filter = new CharacterEncodingFilter();
//		filter.setEncoding("UTF-8");
//		filter.setForceEncoding(true);
//		http.addFilterBefore(filter, CsrfFilter.class);
		http.addFilterBefore(getLogFilter(), UsernamePasswordAuthenticationFilter.class);

		http.antMatcher("/**")
			.authorizeRequests()
			.antMatchers(RESOURCE_LOCATIONS).permitAll()
			.anyRequest()
			.authenticated()
			.and()
			.formLogin()
			.loginProcessingUrl("/login")
			.failureUrl("/login?error")
			.successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
			.permitAll()
			.and()
			.logout()
			.logoutSuccessHandler(logoutSuccessHandler)
			.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")).permitAll()
			.and().exceptionHandling()
			.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
			.and()
			.csrf().disable()
//			.csrf().ignoringAntMatchers("/api/**")
//			.csrfTokenRepository(csrfTokenRepository())
//			.and()
			.headers().frameOptions().disable()
			.and()
			.addFilterAfter(csrfHeaderFilter(), CsrfFilter.class)
			.cors().disable();
		// @formatter:on
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
	}

	@Bean
	public Filter getLogFilter() {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
				Enumeration iter = httpServletRequest.getHeaderNames();
				while (iter.hasMoreElements()) {
					String headerAttribute = iter.nextElement().toString();
					logger.debug(headerAttribute + ": " + httpServletRequest.getHeader(headerAttribute));
				}
				filterChain.doFilter(httpServletRequest, httpServletResponse);

			}
		};
	}

	private Filter csrfHeaderFilter() {
		return new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request,
											HttpServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {
				CsrfToken csrf = (CsrfToken) request
						.getAttribute(CsrfToken.class.getName());
				if (csrf != null) {
					Cookie cookie = WebUtils.getCookie(request, xsrfTokenName);
					String token = csrf.getToken();
					if (cookie == null
							|| token != null && !token.equals(cookie.getValue())) {
						cookie = new Cookie(xsrfTokenName, token);
						cookie.setPath("/");
						response.addCookie(cookie);
					}
				}
				filterChain.doFilter(request, response);
			}
		};
	}

	private CsrfTokenRepository csrfTokenRepository() {
		HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
		repository.setHeaderName(xsrfTokenName);
		return repository;
	}

	public class LogoutHandler extends AbstractAuthenticationTargetUrlRequestHandler implements LogoutSuccessHandler {
		public LogoutHandler() {
		}

		private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

		public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
			Cookie cookie = new Cookie("JSESSIONID", null);
			cookie.setPath(request.getContextPath());
			cookie.setMaxAge(0);
			response.addCookie(cookie);

			String domainUrl = settingsService.getCurrentSettings().getDomainUrl();

			redirectStrategy.sendRedirect(request, response, domainUrl);
		}
	}

}