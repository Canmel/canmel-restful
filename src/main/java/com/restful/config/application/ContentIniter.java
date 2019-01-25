package com.restful.config.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

@Configuration
public class ContentIniter {
    @Bean
    public AuthorizationCodeResourceDetails initCodeResourceDatails(){
        return new AuthorizationCodeResourceDetails();
    }
}
