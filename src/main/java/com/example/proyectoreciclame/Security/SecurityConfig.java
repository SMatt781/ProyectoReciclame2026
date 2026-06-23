package com.example.proyectoreciclame.Security;

import com.example.proyectoreciclame.Service.RecaptchaService;
import com.example.proyectoreciclame.Service.SessionStore;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final RecaptchaService recaptchaService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                          CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                          CustomLogoutSuccessHandler customLogoutSuccessHandler,
                          RecaptchaService recaptchaService) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.recaptchaService = recaptchaService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public ServletListenerRegistrationBean<SessionStore> sessionStoreListener(SessionStore sessionStore) {
        return new ServletListenerRegistrationBean<>(sessionStore);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.csrfTokenRequestHandler(csrfHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/post-login",
                                "/registro/**",
                                "/solicitud-enviada",
                                "/password/**",
                                "/terminos",
                                "/privacidad",
                                "/soporte",
                                "/soporte/enviar",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/acceso-denegado",
                                "/api/validar-documento"
                        ).permitAll()
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/socio/**").hasRole("SOCIO")
                        .requestMatchers("/visualizador/**").hasRole("VISUALIZADOR")
                        .requestMatchers("/normativas", "/normativas/**").hasAnyRole("SOCIO", "VISUALIZADOR")
                        .requestMatchers("/estudios", "/estudios/**").hasAnyRole("SOCIO", "VISUALIZADOR")
                        .requestMatchers("/perfil/**").authenticated()
                        .requestMatchers("/ai/**").hasAnyRole("SOCIO", "VISUALIZADOR", "ADMIN", "SUPERADMIN")
                        .requestMatchers("/chatbot/**").hasAnyRole("SOCIO", "VISUALIZADOR", "ADMIN", "SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, ex) ->
                                response.sendRedirect(request.getContextPath() + "/acceso-denegado"))
                )
                .addFilterBefore(
                        new RecaptchaLoginFilter(recaptchaService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .key("reciclameRememberMeKey")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .userDetailsService(customUserDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );

        return http.build();
    }
}