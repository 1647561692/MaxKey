package org.maxkey.boot.monitor;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class MonitorSecurityConfigurer extends WebSecurityConfigurerAdapter {


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // ��¼�ɹ�������
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl("/");

        http.authorizeRequests()
                //������֤
                .antMatchers(
                            "/login",           //��¼ҳ��
                            "/assets/**",       //��̬�ļ��������
                            "/actuator/**",     //springboot-admin��ص�����
                            "/instances/**"     //springboot-admin��ص�ʵ����Ϣ����
                ).permitAll()
                //��������������Ҫ��¼
                .anyRequest().authenticated()
                //��¼
                .and().formLogin().loginPage("/login").successHandler(successHandler)
                //�ǳ�
                .and().logout().logoutUrl("/logout").logoutSuccessUrl("/login")
                .and().httpBasic()
                .and().csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers(
                        "/instances",
                        "/actuator/**"
                );

    }
}
