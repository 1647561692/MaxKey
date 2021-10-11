/*
 * Copyright [2020] [MaxKey of copyright http://www.maxkey.top]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 

package org.maxkey;

import javax.sql.DataSource;
import org.maxkey.authz.oauth2.provider.client.JdbcClientDetailsService;
import org.maxkey.authz.oauth2.provider.token.DefaultTokenServices;
import org.maxkey.authz.oauth2.provider.token.TokenStore;
import org.maxkey.authz.oauth2.provider.token.store.InMemoryTokenStore;
import org.maxkey.authz.oauth2.provider.token.store.RedisTokenStore;
import org.maxkey.jobs.DynamicGroupsJob;
import org.maxkey.password.onetimepwd.AbstractOtpAuthn;
import org.maxkey.password.onetimepwd.impl.TimeBasedOtpAuthn;
import org.maxkey.persistence.db.LoginHistoryService;
import org.maxkey.persistence.db.LoginService;
import org.maxkey.persistence.db.PasswordPolicyValidator;
import org.maxkey.persistence.redis.RedisConnectionFactory;
import org.maxkey.persistence.service.GroupsService;
import org.maxkey.persistence.service.UserInfoService;
import org.opensaml.xml.ConfigurationException;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.maxkey.authn.realm.jdbc.JdbcAuthenticationRealm;
import org.maxkey.authn.support.rememberme.AbstractRemeberMeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class MaxKeyMgtConfig  implements InitializingBean {
    private static final  Logger _logger = LoggerFactory.getLogger(MaxKeyMgtConfig.class);
    

    @Bean(name = "oauth20JdbcClientDetailsService")
    public JdbcClientDetailsService JdbcClientDetailsService(
                DataSource dataSource,PasswordEncoder passwordReciprocal) {
	    JdbcClientDetailsService clientDetailsService = new JdbcClientDetailsService(dataSource);
	    clientDetailsService.setPasswordEncoder(passwordReciprocal);
	    _logger.debug("JdbcClientDetailsService inited.");
        return clientDetailsService;
    }
	
    /**
     * TokenStore. 
     * @param persistence int
     * @return oauth20TokenStore
     */
    @Bean(name = "oauth20TokenStore")
    public TokenStore oauth20TokenStore(
            @Value("${maxkey.server.persistence}") int persistence,
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory jedisConnectionFactory) {
        TokenStore tokenStore = null;
        if (persistence == 2) {
            tokenStore = new RedisTokenStore(jedisConnectionFactory);
            _logger.debug("RedisTokenStore");
        }else {
            tokenStore = new InMemoryTokenStore();
            _logger.debug("InMemoryTokenStore"); 
        }
        
        return tokenStore;
    }
    
    /**
     * clientDetailsUserDetailsService. 
     * @return oauth20TokenServices
     */
    @Bean(name = "oauth20TokenServices")
    public DefaultTokenServices DefaultTokenServices(
            JdbcClientDetailsService oauth20JdbcClientDetailsService,
            TokenStore oauth20TokenStore) {
        DefaultTokenServices tokenServices = new DefaultTokenServices();
        tokenServices.setClientDetailsService(oauth20JdbcClientDetailsService);
        tokenServices.setTokenStore(oauth20TokenStore);
        tokenServices.setSupportRefreshToken(true);
        return tokenServices;
    }
    
	
	//浠ヤ笅鍐呭鍙互娉ㄩ噴鎺夊悗鍐峹ml涓厤缃�,xml寮曞叆鍦∕axKeyMgtApplication涓�
	@Bean(name = "authenticationRealm")
	public JdbcAuthenticationRealm authenticationRealm(
 			PasswordEncoder passwordEncoder,
	    		PasswordPolicyValidator passwordPolicyValidator,
	    		LoginService loginService,
	    		LoginHistoryService loginHistoryService,
	    		AbstractRemeberMeService remeberMeService,
	    		UserInfoService userInfoService,
             JdbcTemplate jdbcTemplate) {
		
        JdbcAuthenticationRealm authenticationRealm = new JdbcAuthenticationRealm(
        		passwordEncoder,
        		passwordPolicyValidator,
        		loginService,
        		loginHistoryService,
        		remeberMeService,
        		userInfoService,
        		jdbcTemplate);
        
        _logger.debug("JdbcAuthenticationRealm inited.");
        return authenticationRealm;
    }

	@Bean(name = "timeBasedOtpAuthn")
    public AbstractOtpAuthn timeBasedOtpAuthn() {
		AbstractOtpAuthn tfaOtpAuthn = new TimeBasedOtpAuthn();
	    _logger.debug("TimeBasedOtpAuthn inited.");
        return tfaOtpAuthn;
    }
	
    /**
     * schedulerJobsInit.
     * @return schedulerJobsInit
     * @throws ConfigurationException 
     * @throws SchedulerException 
     */
    @Bean(name = "schedulerJobs")
    public String  schedulerJobs(
            SchedulerFactoryBean schedulerFactoryBean,
            GroupsService groupsService,
            @Value("${maxkey.job.cron.dynamicgroups}") String cronScheduleDynamicGroups
            ) throws SchedulerException {
       
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        dynamicGroupsJob(scheduler,cronScheduleDynamicGroups,groupsService);
        
        return "schedulerJobs";
    }
    
	
    private void dynamicGroupsJob(Scheduler scheduler ,
                                  String cronSchedule,
                                  GroupsService groupsService) throws SchedulerException {
        JobDetail jobDetail = 
                JobBuilder.newJob(DynamicGroupsJob.class) 
                .withIdentity("DynamicGroupsJob", "DynamicGroups")
                .build();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("groupsService", groupsService);
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronSchedule);
        CronTrigger cronTrigger = 
                TriggerBuilder.newTrigger()
                .withIdentity("triggerDynamicGroups", "DynamicGroups")
                .usingJobData(jobDataMap)
                .withSchedule(scheduleBuilder)
                .build();
        scheduler.scheduleJob(jobDetail,cronTrigger);    
    }
	 
    @Override
    public void afterPropertiesSet() throws Exception {
        
    }

}
