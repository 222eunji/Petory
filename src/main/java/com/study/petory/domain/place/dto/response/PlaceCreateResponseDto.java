package com.study.petory.domain.place.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

	private final List<String> imageUrls;

	private final BigDecimal latitude;

	private final BigDecimal longitude;

	private final LocalDateTime createdAt;

	private PlaceCreateResponseDto(Place place, List<String> imageUrls) {
		this.id = place.getId();
		this.placeName = place.getPlaceName();
		this.placeInfo = place.getPlaceInfo();
		this.placeType = place.getPlaceType();
		this.ratio = place.getRatio();
		this.imageUrls = imageUrls;
		this.latitude = place.getLatitude();
		this.longitude = place.getLongitude();
		this.createdAt = place.getCreatedAt();
	}

	public static PlaceCreateResponseDto of(Place place, List<String> imageUrls) {
		return new PlaceCreateResponseDto(place, imageUrls);
	}
}
