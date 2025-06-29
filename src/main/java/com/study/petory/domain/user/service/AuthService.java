package com.study.petory.domain.user.service;

import java.util.List;

import com.study.petory.domain.user.dto.TokenResponseDto;
import com.study.petory.domain.user.entity.Role;
import com.study.petory.domain.user.entity.User;

public interface AuthService {

	TokenResponseDto issueToken(User user);

	TokenResponseDto reissue(String accessToken, String refreshTokenRaw);

	List<Role> addRoleToUser(Long userId, Role newRole);

	List<Role> removeRoleFromUser(Long userId, Role roleToRemove);

	void suspendUser(Long targetUserId);

	void restoreUser(Long targetUserId);
}
