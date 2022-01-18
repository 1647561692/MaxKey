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
 

package org.maxkey.authz.formbased.endpoint.adapter;

import java.time.Instant;

import org.maxkey.authn.SigninPrincipal;
import org.maxkey.authz.endpoint.adapter.AbstractAuthorizeAdapter;
import org.maxkey.constants.ConstsBoolean;
import org.maxkey.crypto.DigestUtils;
import org.maxkey.entity.UserInfo;
import org.maxkey.entity.apps.AppsFormBasedDetails;
import org.springframework.web.servlet.ModelAndView;

public class FormBasedDefaultAdapter extends AbstractAuthorizeAdapter {

	@Override
	public String generateInfo(SigninPrincipal authentication,UserInfo userInfo,Object app) {
		return null;
	}

	@Override
	public String encrypt(String data, String algorithmKey, String algorithm) {
		return null;
	}

	@Override
	public ModelAndView authorize(UserInfo userInfo, Object app, String data,ModelAndView modelAndView) {
		modelAndView.setViewName("authorize/formbased_sso_submint");
		AppsFormBasedDetails details=(AppsFormBasedDetails)app;
		
		String password = details.getAppUser().getRelatedPassword();
        if(null==details.getPasswordAlgorithm()||details.getPasswordAlgorithm().equals("")){
        }else if(details.getPasswordAlgorithm().indexOf("HEX")>-1){
            password = DigestUtils.digestHex(details.getAppUser().getRelatedPassword(),details.getPasswordAlgorithm().substring(0, details.getPasswordAlgorithm().indexOf("HEX")));
        }else{
            password = DigestUtils.digestBase64(details.getAppUser().getRelatedPassword(),details.getPasswordAlgorithm());
        }
        
        modelAndView.addObject("id", details.getId());
		modelAndView.addObject("action", details.getRedirectUri());
		modelAndView.addObject("redirectUri", details.getRedirectUri());
		modelAndView.addObject("loginUrl", details.getLoginUrl());
		modelAndView.addObject("usernameMapping", details.getUsernameMapping());
		modelAndView.addObject("passwordMapping", details.getPasswordMapping());
		modelAndView.addObject("username", details.getAppUser().getRelatedUsername());    
        modelAndView.addObject("password",  password);
        modelAndView.addObject("timestamp",  ""+Instant.now().getEpochSecond());
		
	    if(ConstsBoolean.isTrue(details.getIsExtendAttr())){
	        modelAndView.addObject("extendAttr", details.getExtendAttr());
	        modelAndView.addObject("isExtendAttr", true);
        }else{
            modelAndView.addObject("isExtendAttr", false);
        }
	    
		if(details.getAuthorizeView()!=null&&!details.getAuthorizeView().equals("")){
			modelAndView.setViewName("authorize/"+details.getAuthorizeView());
		}
		
		return modelAndView;
	}

}
