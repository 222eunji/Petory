package com.study.petory.domain.place.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.study.petory.domain.place.entity.Place;
import com.study.petory.domain.place.entity.PlaceType;

import lombok.Getter;

@Getter
public class PlaceCreateResponseDto {

	private final Long id;

	private final String placeName;

	private final String placeInfo;

	private final PlaceType placeType;

	private final BigDecimal ratio;

	// private final String photoList;  // 게시글에 댓글 리스트 조회하듯이 가져와야할듯? dto로 만들어서 넣는것도 방법!

	private final BigDecimal latitude;

	private final BigDecimal longitude;

	private final LocalDateTime createdAt;

	private PlaceCreateResponseDto(Place place) {
		this.id = place.getId();
		this.placeName = place.getPlaceName();
		this.placeInfo = place.getPlaceInfo();
		this.placeType = place.getPlaceType();
		this.ratio = place.getRatio();
		this.latitude = place.getLatitude();
		this.longitude = place.getLongitude();
		this.createdAt = place.getCreatedAt();
	}

	public static PlaceCreateResponseDto from(Place place) {
		return new PlaceCreateResponseDto(place);
	}
}
