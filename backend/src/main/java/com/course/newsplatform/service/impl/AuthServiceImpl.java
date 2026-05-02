package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.*;
import com.course.newsplatform.entity.Role;
import com.course.newsplatform.entity.User;
import com.course.newsplatform.entity.UserRole;
import com.course.newsplatform.mapper.RoleMapper;
import com.course.newsplatform.mapper.UserMapper;
import com.course.newsplatform.mapper.UserRoleMapper;
import com.course.newsplatform.security.JwtTokenProvider;
import com.course.newsplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthTokenResponse adminLogin(AdminLoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(4011, "用户名或密码错误");
        }
        List<String> roles = roleMapper.findByUserId(user.getId()).stream().map(Role::getCode).toList();
        if (!roles.contains("ADMIN") && !roles.contains("EDITOR")) {
            throw new BizException(4031, "该账号无后台访问权限");
        }
        return buildToken(user, roles);
    }

    @Override
    public AuthTokenResponse wechatLogin(WechatLoginRequest request) {
        String openId = request.getCode() == null || request.getCode().isBlank()
                ? "wx_" + UUID.randomUUID().toString().replace("-", "")
                : "wx_" + request.getCode();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setNickname(request.getNickname() == null || request.getNickname().isBlank() ? "微信用户" : request.getNickname());
            user.setAvatar(request.getAvatar());
            user.setEnabled(1);
            userMapper.insert(user);

            Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER"));
            if (userRole != null) {
                UserRole relation = new UserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(userRole.getId());
                userRoleMapper.insert(relation);
            }
        } else {
            boolean changed = false;
            String reqNickname = request.getNickname();
            if (reqNickname != null && !reqNickname.isBlank() && !reqNickname.equals(user.getNickname())) {
                user.setNickname(reqNickname);
                changed = true;
            }
            String reqAvatar = request.getAvatar();
            if (reqAvatar != null && !reqAvatar.isBlank() && !reqAvatar.equals(user.getAvatar())) {
                user.setAvatar(reqAvatar);
                changed = true;
            }
            if (changed) {
                userMapper.updateById(user);
            }
        }

        List<String> roles = roleMapper.findByUserId(user.getId()).stream().map(Role::getCode).toList();
        return buildToken(user, roles);
    }

    @Override
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            if (!jwtTokenProvider.isRefreshToken(request.getRefreshToken())) {
                throw new BizException(4012, "refreshToken 无效");
            }
            Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BizException(4013, "用户不存在");
            }
            List<String> roles = roleMapper.findByUserId(userId).stream().map(Role::getCode).toList();
            return buildToken(user, roles);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(4014, "refreshToken 已过期或非法");
        }
    }

    @Override
    public UserProfileResponse profile() {
        Long userId = SecurityUtils.currentUserId();
        User user = userMapper.selectById(userId);
        List<String> roles = roleMapper.findByUserId(userId).stream().map(Role::getCode).toList();
        return UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .build();
    }

    @Override
    public UserProfileResponse updateProfile(ProfileUpdateRequest request) {
        Long userId = SecurityUtils.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            if (request.getNickname().length() > 32) {
                throw new BizException("昵称不能超过32个字");
            }
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar().trim());
        }
        userMapper.updateById(user);

        List<String> roles = roleMapper.findByUserId(userId).stream().map(Role::getCode).toList();
        return UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .build();
    }

    private AuthTokenResponse buildToken(User user, List<String> roles) {
        String username = user.getUsername() == null ? String.valueOf(user.getId()) : user.getUsername();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), username, roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpireSeconds())
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roles)
                .build();
    }
}
