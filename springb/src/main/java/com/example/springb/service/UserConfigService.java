package com.example.springb.service;

import com.example.springb.entity.UserConfig;
import com.example.springb.exception.CustomerException;
import com.example.springb.mapper.UserConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserConfigService {
    private static final String KEY_FEISHU_USER_TOKEN = "feishu.user_token";
    private static final String DESC_FEISHU_USER_TOKEN = "飞书 User Access Token";
    private static final int MAX_TOKEN_LENGTH = 4096;

    @Resource
    private UserConfigMapper userConfigMapper;

    public String getFeishuUserToken(Integer userId) {
        Integer normalizedUserId = normalizeUserId(userId);
        UserConfig config = userConfigMapper.selectByUserIdAndKey(normalizedUserId, KEY_FEISHU_USER_TOKEN);
        return config == null || config.getConfigValue() == null ? "" : config.getConfigValue().trim();
    }

    public void saveFeishuUserToken(Integer userId, String token) {
        Integer normalizedUserId = normalizeUserId(userId);
        String normalizedToken = normalizeToken(token);

        UserConfig config = new UserConfig();
        config.setUserId(normalizedUserId);
        config.setConfigKey(KEY_FEISHU_USER_TOKEN);
        config.setConfigValue(normalizedToken);
        config.setDescription(DESC_FEISHU_USER_TOKEN);
        userConfigMapper.upsert(config);
    }

    private Integer normalizeUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new CustomerException("401", "未获取到当前登录用户信息");
        }
        return userId;
    }

    private String normalizeToken(String token) {
        String value = token == null ? "" : token.trim();
        if (value.length() > MAX_TOKEN_LENGTH) {
            throw new CustomerException("400", "飞书 Token 长度超出限制");
        }
        return value;
    }
}
