package com.study.petory.domain.dailyQna.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.study.petory.domain.dailyQna.dto.request.DailyQnaCreateRequestDto;
import com.study.petory.domain.dailyQna.dto.request.DailyQnaUpdateRequestDto;
import com.study.petory.domain.dailyQna.dto.response.DailyQnaGetDeletedResponse;
import com.study.petory.domain.dailyQna.dto.response.DailyQnaGetHiddenResponseDto;
import com.study.petory.domain.dailyQna.dto.response.DailyQnaGetResponseDto;
import com.study.petory.domain.dailyQna.entity.DailyQna;
import com.study.petory.domain.dailyQna.entity.DailyQnaStatus;

public interface DailyQnaService {

	DailyQna findDailyQnaByStatusAndId(List<DailyQnaStatus> statusList, Long dailyQnaId);

	DailyQnaStatus findDailyQnaStatusById(Long dailyQnaId);

	void validateAuthor(Long userId, DailyQna dailyQna);

	void saveDailyQna(Long userId, Long questionId, DailyQnaCreateRequestDto requestDto);

	List<DailyQnaGetResponseDto> findDailyQna(Long userId, Long questionId);

	void updateDailyQna(Long userId, Long dailyQnaId, DailyQnaUpdateRequestDto requestDto);

	void hideDailyQna(Long userId, Long dailyQnaId);

	Page<DailyQnaGetHiddenResponseDto> findHiddenDailyQna(Long userId, Pageable pageable);

	void updateDailyQnaStatusActive(Long userId, Long dailyQnaId);

	void deleteDailyQna(Long dailyQnaId);

	Page<DailyQnaGetDeletedResponse> findDeletedDailyQna(Long userId, Pageable pageable);

	void restoreDailyQna(Long dailyQnaId);
}
