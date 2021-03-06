package com.restful.web;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.core.entity.ErrorResult;
import com.core.entity.HttpResult;
import com.core.entity.Result;
import com.restful.annotation.SaveLog;
import com.restful.config.security.CustomAuthenticationProvider;
import com.restful.entity.SysUser;
import com.restful.service.SysUserService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Base64;
import java.util.Collections;

/**
 * <p>
 * 登录信息系统级api,不受权限控制
 * </p>
 *
 * @author  *
 *   ┏ ┓   ┏ ┓
 *  ┏┛ ┻━━━┛ ┻┓
 *  ┃         ┃
 *  ┃    ━    ┃
 *  ┃  ┳┛  ┗┳ ┃
 *  ┃         ┃
 *  ┃    ┻    ┃
 *  ┃         ┃
 *  ┗━━┓    ┏━┛
 *     ┃    ┃神兽保佑
 *     ┃    ┃代码无BUG！
 *     ┃    ┗━━━━━━━┓
 *     ┃            ┣┓
 *     ┃            ┏┛
 *     ┗┓┓┏━━━━━━┳┓┏┛
 *      ┃┫┫      ┃┫┫
 *      ┗┻┛      ┗┻┛
 * @since 2018-08-12
 */
@RestController
@RequestMapping("/auth")
public class MyRestController {

    private Logger logger = Logger.getLogger(MyRestController.class);

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OAuth2ClientProperties oAuth2ClientProperties;

    @Autowired
    private OAuth2ProtectedResourceDetails oAuth2ProtectedResourceDetails;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @PostMapping("/login")
    public HttpResult login(HttpServletRequest request, HttpServletResponse response, SysUser user, String imageCode) {
        if (!validCode(imageCode, request)) {
            logger.info("验证码错误或已过期");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return ErrorResult.UNAUTHORIZED("验证码错误或已过期");
        }
        HttpEntity httpEntity = buildRequestInfoMap(user);
        ResponseEntity<OAuth2AccessToken> oAuth2AccessToken = null;
        EntityWrapper<SysUser> sysUserEntityWrapper = new EntityWrapper<SysUser>();
        sysUserEntityWrapper.eq("username", user.getUsername());
        try {
            oAuth2AccessToken = restTemplate.exchange(oAuth2ProtectedResourceDetails.getAccessTokenUri(), HttpMethod.POST, httpEntity, OAuth2AccessToken.class);
            saveToSession(request.getSession(), sysUserService.selectOne(sysUserEntityWrapper), oAuth2AccessToken.getBody().getValue());
            if (ObjectUtils.isEmpty(oAuth2AccessToken)) {
                return ErrorResult.UNAUTHORIZED("登录失败");
            }
        } catch (Exception e) {
            logger.error(e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ErrorResult.UNAUTHORIZED("登录失败, 请检查用户名密码");
        }
        return Result.OK("登录成功", sysUserService.selectOne(sysUserEntityWrapper));
    }

    /**
     * 往session里存放当前登录信息
     * @param session
     * @param user
     */
    private void saveToSession(HttpSession session, SysUser user, String access_token) {
        user.setPassword(null);
        session.setAttribute("currentUser", user);
        session.setAttribute("ACCESS_TOKEN", access_token);
    }

    private HttpEntity buildRequestInfoMap(SysUser loginUser) {
        String clientAndSecret = oAuth2ClientProperties.getClientId() + ":" + oAuth2ClientProperties.getClientSecret();
        clientAndSecret = "Basic " + Base64.getEncoder().encodeToString(clientAndSecret.getBytes());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.set("Authorization", clientAndSecret);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.put("username", Collections.singletonList(loginUser.getUsername()));
        map.put("password", Collections.singletonList(loginUser.getPassword()));
        map.put("grant_type", Collections.singletonList(oAuth2ProtectedResourceDetails.getGrantType()));
        map.put("scope", oAuth2ProtectedResourceDetails.getScope());
        //HttpEntity
        return new HttpEntity(map, httpHeaders);
    }

    private boolean validCode(String verifyCode, HttpServletRequest request) {
        if(StringUtils.isEmpty(verifyCode)){
            return false;
        }
        String session_verifyTime = (String) request.getSession().getAttribute("session_imageTime");
        Long st = Long.parseLong(session_verifyTime);
        if (st - System.currentTimeMillis() > 60) {
            return false;
        }
        StringBuffer sessiom_code = (StringBuffer) request.getSession().getAttribute("session_imageCode");
        return verifyCode.toLowerCase().equals(sessiom_code.toString().toLowerCase());
    }

}
