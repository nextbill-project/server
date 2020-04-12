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

import de.nextbill.oauth.dtos.AuthUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomTokenEnhancer extends JwtAccessTokenConverter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken,
                                     OAuth2Authentication authentication) {

        Map<String, Object> info = new LinkedHashMap<String, Object>(accessToken.getAdditionalInformation());
        AuthUserDTO principal = (AuthUserDTO) authentication.getPrincipal();
        info.put("userId",principal.getAppUserId().toString());

        DefaultOAuth2AccessToken customAccessToken = new DefaultOAuth2AccessToken(accessToken);
        customAccessToken.setAdditionalInformation(info);
        return super.enhance(customAccessToken, authentication);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        final OAuth2Authentication authentication = super.extractAuthentication(map);
        authentication.setDetails(map);
        return authentication;
    }
}
