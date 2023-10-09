package net.rhizomik.rhizomer.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Created by http://rhizomik.net/~roberto/
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${allowed-origins}")
    String[] allowedOrigins;
    @Value("${spring.application.name}")
    String applicationName;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/user").authenticated()
            .antMatchers(HttpMethod.GET, "/datasets").authenticated()

            .antMatchers(HttpMethod.GET, "/admins*/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.POST, "/admins*/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.PUT, "/admins*/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.PATCH, "/admins*/*").hasRole("ADMIN")
            .antMatchers(HttpMethod.DELETE, "/admins*/**").hasRole("ADMIN")

            .antMatchers(HttpMethod.GET, "/users").hasRole("ADMIN")
            .antMatchers(HttpMethod.GET, "/users/*").authenticated()
            .antMatchers(HttpMethod.POST, "/users/*").hasRole("ADMIN")

            .antMatchers(HttpMethod.PUT, "/**/*").authenticated()
            .antMatchers(HttpMethod.POST, "/**/*").authenticated()
            .antMatchers(HttpMethod.PATCH, "/**/*").authenticated()
            .antMatchers(HttpMethod.DELETE, "/**/*").authenticated()
            .anyRequest().permitAll()
            .and()
            .httpBasic().realmName("Rhizomer")
            .and()
            .cors()
            .and()
            .csrf().disable();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        
        for (String allowed: allowedOrigins) {
            System.out.println("allowedOrigins:"+allowed);
        }
        
        System.out.println("application:"+applicationName);
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        corsConfiguration
            .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
