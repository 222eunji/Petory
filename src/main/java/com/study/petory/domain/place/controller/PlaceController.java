package com.study.petory.domain.place.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.study.petory.common.response.CommonResponse;
import com.study.petory.domain.place.dto.request.PlaceCreateRequestDto;
import com.study.petory.domain.place.dto.request.PlaceReviewCreateRequestDto;
import com.study.petory.domain.place.dto.request.PlaceReviewUpdateRequestDto;
import com.study.petory.domain.place.dto.request.PlaceStatusChangeRequestDto;
import com.study.petory.domain.place.dto.request.PlaceUpdateRequestDto;
import com.study.petory.domain.place.dto.response.PlaceCreateResponseDto;
import com.study.petory.domain.place.dto.response.PlaceGetAllResponseDto;
import com.study.petory.domain.place.dto.response.PlaceGetResponseDto;
import com.study.petory.domain.place.dto.response.PlaceReviewCreateResponseDto;
import com.study.petory.domain.place.dto.response.PlaceReviewUpdateResponseDto;
import com.study.petory.domain.place.dto.response.PlaceUpdateResponseDto;
import com.study.petory.domain.place.entity.PlaceType;
import com.study.petory.domain.place.service.BookmarkPlaceService;
import com.study.petory.domain.place.service.PlaceReviewService;
import com.study.petory.domain.place.service.PlaceService;
import com.study.petory.exception.enums.SuccessCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/places")
public class PlaceController {

	private final PlaceService placeService;
	private final PlaceReviewService placeReviewService;
	private final BookmarkPlaceService bookmarkPlaceService;

	/**
	 * 장소 등록
	 * 사진, 평균 평점 추가 예정
	 * @param requestDto 장소 등록에 필요한 정보
	 * @return CommonResponse 방식의 등록된 장소 정보
	 */
	@PostMapping
	public ResponseEntity<CommonResponse<PlaceCreateResponseDto>> createPlace(
		@Valid @RequestBody PlaceCreateRequestDto requestDto
	) {
		return CommonResponse.of(SuccessCode.CREATED, placeService.savePlace(requestDto));
	}

	/**
	 * 전체 장소 조회
	 * 페이징 관련 스크롤 방식이 옳을 지에 대한 고민
	 * 필터 검색 로직 QueryDSL 사용으로 바꾸는게 옳을 지에 대한 고민
	 * @param placeName 장소 이름 일부 입력 시에도 조회 가능
	 * @param placeType 장소 타입 입력 시 타입 조건 기준 조회 가능
	 * @param pageable 기본 페이징 설정. 한 페이지에 10개의 게시글(장소) 조회
	 * @return CommonResponse 방식의 페이징된 장소 정보 목록
	 */
	@GetMapping
	public ResponseEntity<CommonResponse<Page<PlaceGetAllResponseDto>>> getAllPlace(
		@RequestParam(required = false) String placeName,
		@RequestParam(required = false) PlaceType placeType,
		@PageableDefault(size = 10) Pageable pageable
	) {
		return CommonResponse.of(SuccessCode.REQUESTED, placeService.findAllPlace(placeName, placeType, pageable));
	}

	/**
	 * 특정 장소 조회
	 * 사진, 평균 평점, 페이징 된 리뷰 목록 추가 예정
	 * @param placeId 장소 식별자
	 * @return CommonResponse 방식의 특정 장소 정보 + 해당 장소의 리뷰 목록
	 */
	@GetMapping("/{placeId}")
	public ResponseEntity<CommonResponse<PlaceGetResponseDto>> getByPlaceId(
		@PathVariable Long placeId
	) {
		return CommonResponse.of(SuccessCode.REQUESTED, placeService.findByPlaceId(placeId));
	}

	/**
	 * 장소 수정
	 * @param placeId 장소 식별자
	 * @param requestDto 장소 수정에 필요한 정보
	 * @return CommonResponse 방식의 특정 장소의 수정된 정보
	 */
	@PatchMapping("/{placeId}")
	public ResponseEntity<CommonResponse<PlaceUpdateResponseDto>> updatePlace(
		@PathVariable Long placeId,
		@Valid @RequestBody PlaceUpdateRequestDto requestDto
	) {
		return CommonResponse.of(SuccessCode.REQUESTED, placeService.updatePlace(placeId, requestDto));
	}

	/**
	 * 삭제된 장소 복구
	 * soft delete 된 장소를 다시 복구하는 기능
	 * 삭제된 장소인지 검증 로직 필요
	 * @param placeId 장소 식별자
	 * @param requestDto 장소 복구에 필요한 정보
	 * @return CommonResponse 방식의 삭제된 장소 복구 메시지
	 */
	@PatchMapping("/{placeId}/restore")
	public ResponseEntity<CommonResponse<Void>> restorePlace(
		@PathVariable Long placeId,
		@Valid @RequestBody PlaceStatusChangeRequestDto requestDto
	) {
		placeService.restorePlace(placeId, requestDto);
		return CommonResponse.of(SuccessCode.RESTORED);
	}

	/**
	 * 장소 삭제
	 * soft delete 구현
	 * 삭제 가능한 장소인지 검증 로직 필요
	 * @param placeId 장소 식별자
	 * @param requestDto 장소 삭제에 필요한 Enum 정보
	 * @return CommonResponse 방식의 장소 삭제 메시지
	 */
	@DeleteMapping("/{placeId}")
	public ResponseEntity<CommonResponse<Void>> deletePlace(
		@PathVariable Long placeId,
		@Valid @RequestBody PlaceStatusChangeRequestDto requestDto
	) {
		placeService.deletePlace(placeId, requestDto);
		return CommonResponse.of(SuccessCode.DELETED);
	}

	/**
	 * 리뷰 등록
	 * 유저 부분은 추후 수정 예정
	 * @param placeId 장소 식별자
	 * @param requestDto 리뷰 등록에 필요한 정보
	 * @return CommonResponse 방식의 등록된 리뷰에 대한 정보
	 */
	@PostMapping("/{placeId}/reviews")
	public ResponseEntity<CommonResponse<PlaceReviewCreateResponseDto>> createPlaceReview(
		@PathVariable Long placeId,
		@Valid @RequestBody PlaceReviewCreateRequestDto requestDto
	) {
		return CommonResponse.of(SuccessCode.CREATED, placeReviewService.savePlaceReview(placeId, requestDto));
	}

	/**
	 * 리뷰 수정
	 * @param placeId 장소 식별자
	 * @param reviewId 리뷰 식별자
	 * @param requestDto 리뷰 등록에 필요한 정보
	 * @return CommonResponse 방식의 수정된 리뷰에 대한 정보
	 */
	@PatchMapping("/{placeId}/reviews/{reviewId}")
	public ResponseEntity<CommonResponse<PlaceReviewUpdateResponseDto>> updatePlaceReview(
		@PathVariable Long placeId,
		@PathVariable Long reviewId,
		@Valid @RequestBody PlaceReviewUpdateRequestDto requestDto
	) {
		return CommonResponse.of(SuccessCode.REQUESTED, placeReviewService.updatePlaceReview(placeId, reviewId, requestDto));
	}

	/**
	 * 삭제된 리뷰 복구
	 * soft delete 된 리뷰를 다시 복구하는 기능
	 * @param placeId 장소 식별자
	 * @param reviewId 리뷰 식별자
	 * @return CommonResponse 방식의 삭제된 리뷰 복구 메시지
	 */
	@PatchMapping("/{placeId}/reviews/{reviewId}/restore")
	public ResponseEntity<CommonResponse<Void>> restorePlaceReview(
		@PathVariable Long placeId,
		@PathVariable Long reviewId
	) {
		placeReviewService.restorePlaceReview(placeId, reviewId);
		return CommonResponse.of(SuccessCode.RESTORED);
	}

	/**
	 * 리뷰 삭제
	 * soft delete 구현
	 * @param placeId 장소 식별자
	 * @param reviewId 리뷰 식별자
	 * @return CommonResponse 방식의 리뷰 삭제 메시지
	 */
	@DeleteMapping("/{placeId}/reviews/{reviewId}")
	public ResponseEntity<CommonResponse<Void>> deletePlaceReview(
		@PathVariable Long placeId,
		@PathVariable Long reviewId
	) {
		placeReviewService.deletePlaceReview(placeId, reviewId);
		return CommonResponse.of(SuccessCode.DELETED);
	}

	/**
	 * Json data 저장하기
	 * @return CommonResponse 방식의 성공 메시지
	 */
	@PostMapping("/write-json")
	public ResponseEntity<CommonResponse<Void>> writeJsonData() {
		// 현재 프로젝트의 루트 경로를 가져와서 src 이하의 경로를 붙이는 과정
		String filePath = System.getProperty("user.dir") + "/src/main/resources/data";
		bookmarkPlaceService.writeJsonData(filePath);
		return CommonResponse.of(SuccessCode.OK);
	}
}
