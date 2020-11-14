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

package de.nextbill.oauth.config;

import de.nextbill.domain.config.CustomTokenEnhancer;
import de.nextbill.domain.model.Settings;
import de.nextbill.domain.services.SettingsService;
import de.nextbill.oauth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationConfigurer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultTokenServices defaultTokenServices;

    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;

    @Autowired
    private SettingsService settingsService;

    @Bean
    @Primary
    public JwtAccessTokenConverter jwtAccessTokenConverter(SettingsService settingsService) throws Exception {

        JwtAccessTokenConverter  accessTokenConverter = new CustomTokenEnhancer();
        accessTokenConverter.setSigningKey(settingsService.getCurrentSettings().getJwtStoreKey());
        return accessTokenConverter;
    }

    @Bean
    @Primary
    public DefaultTokenServices defaultTokenServices(final TokenStore tokenStore, final JwtAccessTokenConverter jwtAccessTokenConverter) {
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setTokenStore(tokenStore);
        tokenServices.setTokenEnhancer(jwtAccessTokenConverter);
        tokenServices.setSupportRefreshToken(true);
        tokenServices.setAccessTokenValiditySeconds(864000);
        tokenServices.setRefreshTokenValiditySeconds(864000);
        return tokenServices;
    }

    @Bean
    @Primary
    public TokenStore tokenStore(final JwtAccessTokenConverter jwtAccessTokenConverter) throws Exception {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

        Settings settings = settingsService.getCurrentSettings();

        clients.inMemory()
                .withClient("nextbillMobileClient")
                .secret(passwordEncoder.encode(settings.getClientSecret()))
                .authorizedGrantTypes("refresh_token", "password")
                .accessTokenValiditySeconds(864000)
                .refreshTokenValiditySeconds(864000)
                .autoApprove(true)
                .scopes("openid");
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(authenticationManager)
                .userDetailsService(userDetailsService)
                .tokenEnhancer(jwtAccessTokenConverter)
                .accessTokenConverter(jwtAccessTokenConverter)
                .tokenServices(defaultTokenServices);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()").allowFormAuthenticationForClients().checkTokenAccess("isAuthenticated()");
    }
}