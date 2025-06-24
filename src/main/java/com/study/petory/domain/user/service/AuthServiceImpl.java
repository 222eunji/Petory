package com.study.petory.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.petory.common.exception.CustomException;
import com.study.petory.common.exception.enums.ErrorCode;
import com.study.petory.common.security.JwtProvider;
import com.study.petory.domain.user.dto.TokenResponseDto;
import com.study.petory.domain.user.entity.Role;
import com.study.petory.domain.user.entity.User;
import com.study.petory.domain.user.entity.UserRole;
import com.study.petory.domain.user.entity.UserStatus;
import com.study.petory.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserService userService;
	private final JwtProvider jwtProvider;
	private final RedisTemplate<String, String> loginRefreshToken;

	/*
	 * [토큰 발급]
	 * Google OAuth2 로그인 성공 시 토큰 발급
	 * Redis 에 refreshToken 저장 (userId 기준 키)
	 */
	@Override
	@Transactional
	public TokenResponseDto issueToken(User user) {

		User savedUser = userService.findUserByEmail(user.getEmail());

		// 로그인 불가 상태는 예외 처리: 계정 정지 상태 (SUSPENDED)
		if (savedUser.getUserStatus() == UserStatus.SUSPENDED) {
			throw new CustomException(ErrorCode.LOGIN_UNAVAILABLE);
		}

		// userStatus 가 휴면 (DEACTIVATED) 상태로 90일이 지나기 전에 로그인 한 경우 휴면 상태 해제
		if (savedUser.getUserStatus() == UserStatus.DEACTIVATED) {
			LocalDateTime deletedAt = savedUser.getDeletedAt();

			if (deletedAt != null && deletedAt.plusDays(90).isAfter(LocalDateTime.now())) {

				// 기간 안에 로그인 했기 때문에 복구 처리
				savedUser.activateUser();
			}
		}

		// 재로그인/재회원가입 했을 경우 복구 처리
		if (savedUser.getUserStatus() == UserStatus.DELETED) {
			savedUser.activateUser();
		}

		// 로그인 시간 기록
		savedUser.updateLastLoginAt(LocalDateTime.now());

		if (savedUser.getId() == null) {
			throw new CustomException(ErrorCode.USER_ID_NOT_GENERATED);
		}

		List<String> roles = savedUser.getUserRole().stream()
			.map(userRole -> "ROLE_" + userRole.getRole().name())
			.toList();

		String accessToken = jwtProvider.createAccessToken(
			savedUser.getId(),
			savedUser.getEmail(),
			savedUser.getNickname(),
			roles  // JWT Claim 에 포함된 권한 리스트
		);

		String refreshToken = jwtProvider.createRefreshToken(savedUser.getId());

		jwtProvider.storeRefreshToken(savedUser.getId(), refreshToken);

		return new TokenResponseDto(accessToken, refreshToken);
	}

	/*
	 * [로그아웃 처리]
	 * AccessToken 을 블랙리스트 등록 - 만료시간되면 자동 삭제
	 * Redis 에 저장된 RefreshToken 제거
	 */
	@Override
	@Transactional
	public void logout(String accessToken) {

		String pureToken = jwtProvider.subStringToken(accessToken);
		/* TODO - 배포 전 주석 라인 삭제 가능
		 * Long userId = Long.valueOf(jwtProvider.getClaims(accessToken).getSubject());
		 */
		Long userId = Long.valueOf(jwtProvider.getClaims(pureToken).getSubject());

		long expiration = jwtProvider.getClaims(accessToken).getExpiration().getTime() - System.currentTimeMillis();

		/*
		 * AccessToken 을 블랙리스트에 등록하는 로직
		 * expiration 시간은 AccessToken 의 남은 유효기간만큼 설정되어, 만료 시 자동으로 삭제
		 */
		loginRefreshToken.opsForValue()
			.set("BLACKLIST_" + pureToken, "logout", expiration, TimeUnit.MILLISECONDS);

		jwtProvider.deleteRefreshToken(userId);
	}

	/*
	 * [토큰 재발급]
	 * AccessToken 이 만료된 경우에만, 전달된 refreshToken 기반으로 AccessToken 재발급
	 */
	@Override
	@Transactional
	public TokenResponseDto reissue(String accessToken, String refreshTokenRaw) {

		// 1. AccessToken 만료 여부 확인
		if (!jwtProvider.isAccessTokenExpired(accessToken)) {
			throw new CustomException(ErrorCode.TOKEN_NOT_EXPIRED);
		}

		// 2. Bearer 접두사 제거 (있는 경우만)
		String refreshToken;
		if (refreshTokenRaw.startsWith("Bearer ")) {
			refreshToken = jwtProvider.subStringToken(refreshTokenRaw);
		} else {
			refreshToken = refreshTokenRaw;
		}

		// 3. userId 추출
		Long userId = Long.valueOf(jwtProvider.getClaims(refreshToken).getSubject());

		// 4. Redis RefreshToken 검증
		if (!jwtProvider.isValidRefreshToken(userId, refreshToken)) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		// 5. 사용자 조회
		User user = userService.findUserById(userId);

		// 6. 역할 목록 추출
		List<String> roles = user.getUserRole().stream()
			.map(userRole -> "ROLE_" + userRole.getRole().name())
			.toList();

		// 7. 새 토큰 발급
		String newAccessToken = jwtProvider.createAccessToken(
			user.getId(), user.getEmail(), user.getNickname(), roles
		);
		String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

		// 8. Redis 저장 갱신
		jwtProvider.deleteRefreshToken(user.getId());
		jwtProvider.storeRefreshToken(user.getId(), newRefreshToken);

		return new TokenResponseDto(newAccessToken, newRefreshToken);
	}

	/*
	 * [관리자 전용 - 권한 추가]
	 * 중복되는 권한이면 예외 처리
	 * 지정한 사용자에게 새 Role 을 부여하고, 전체 권한 목록 반환
	 */
	@Override
	@Transactional
	public List<Role> addRoleToUser(Long userId, Role newRole) {

		User user = userService.findUserById(userId);

		boolean alreadyHasSameRole = user.getUserRole().stream()
			.anyMatch(userRole -> userRole.isEqualRole(newRole));

		if (alreadyHasSameRole) {
			throw new CustomException(ErrorCode.ALREADY_HAS_SAME_ROLE);
		}

		user.getUserRole().add(UserRole.builder().role(newRole).build());

		return user.getUserRole().stream()
			.map(UserRole::getRole)
			.toList();
	}

	/*
	 * [관리자 전용 - 권한 제거]
	 * 지정한 사용자에게서 Role 을 제거하고, 전체 권한 목록 반환
	 */
	@Override
	@Transactional
	public List<Role> removeRoleFromUser(Long userId, Role roleToRemove) {

		User user = userService.findUserById(userId);

		boolean hasRole = user.getUserRole().stream()
			.anyMatch(userRole -> userRole.isEqualRole(roleToRemove));

		if (!hasRole) {
			throw new CustomException(ErrorCode.ROLE_NOT_FOUND);
		}

		// Role 제거
		user.getUserRole().removeIf(userRole -> userRole.isEqualRole(roleToRemove));

		return user.getUserRole().stream()
			.map(UserRole::getRole)
			.toList();
	}

	/*
	 * [관리자 전용 - 유저 비활성화]
	 * 지정한 사용자를 계정 정지 처리
	 */
	@Override
	@Transactional
	public void suspendUser(Long targetUserId) {

		User user = userService.findUserById(targetUserId);

		if (user.getUserStatus() == UserStatus.SUSPENDED) {
			throw new CustomException(ErrorCode.ALREADY_SUSPENDED);
		}

		user.deactivateEntity();
		user.updateStatus(UserStatus.SUSPENDED);
	}

	/*
	 * [관리자 전용 - 유저 복구]
	 * 계정 정지 처리된 유저를 복구
	 */
	@Override
	@Transactional
	public void restoreUser(Long targetUserId) {

		User user = userService.findUserById(targetUserId);

		if (user.isDeletedAtNull()) {
			throw new CustomException(ErrorCode.USER_NOT_DEACTIVATED);
		}

		user.activateUser();
	}
}
