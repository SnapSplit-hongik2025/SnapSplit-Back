package com.snapsplit.backend.feature.createTrip.service;

import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.country.repository.CountryRepository;
import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.createTrip.dto.CreateTripRequest;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateTripService {

    private final TripRepository tripRepository;
    private final CountryRepository countryRepository;
    private final TripCountryRepository tripCountryRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TotalSharedRepository totalSharedRepository;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final AlbumRepository albumRepository;

    // 신규 여행 등록하기
    @Transactional
    public Long createTrip(CreateTripRequest request, MultipartFile tripImageFile) throws IOException {

        // 8자리 랜덤 코드 생성
        String tripCode = generateTripCode();

        // 여행 대표 사진 S3 업로드
        String tripImageUrl = null;
        if (tripImageFile != null && !tripImageFile.isEmpty()) {
            S3UploadResult uploadResult = s3Uploader.upload(tripImageFile, "trip-cover");
            tripImageUrl = uploadResult.getFileUrl();
        }

        // Trip 생성
        Trip trip = Trip.builder()
                .tripName(request.getTripName())
                .startDate(LocalDate.parse(request.getStartDate()))
                .endDate(LocalDate.parse(request.getEndDate()))
                .tripImage(tripImageUrl)
                .tripTotalExpense(BigDecimal.ZERO)
                .tripCode(tripCode)
                .defaultCurrency("KRW")
                .build();
        tripRepository.save(trip);

        // Trip 생성 직후, 해당 Trip에 대한 Album 생성
        Album album = Album.builder()
                .trip(trip)
                .build();
        albumRepository.save(album);

        // 통화 코드 수집용 Set
        Set<String> currencySet = new HashSet<>();

        // TripCountry 생성
        request.getCountries().forEach(countryDto -> {
            Country country = countryRepository.findById(countryDto.getCountryId())
                    .orElseThrow(() -> new RuntimeException("국가를 찾을 수 없습니다."));

            TripCountry tripCountry = TripCountry.builder()
                    .trip(trip)
                    .country(country)
                    .build();
            tripCountryRepository.save(tripCountry);

            currencySet.add(country.getCurrency());
        });


        // TripMember 생성 - 실제 여행 참여 유저
        request.getUsersId().forEach(memberId -> {
            TripMember tripMember = TripMember.builder()
                    .trip(trip)
                    .user(userRepository.findById(memberId)
                            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다.")))
                    .memberType(com.snapsplit.backend.domain.tripmember.entity.MemberType.USER)
                    .build();
            tripMemberRepository.save(tripMember);
        });

        // TripMember 생성 - 공동경비
        TripMember sharedFundMember = TripMember.builder()
                .trip(trip)
                .user(null)
                .memberType(com.snapsplit.backend.domain.tripmember.entity.MemberType.SHARED_FUND)
                .build();
        tripMemberRepository.save(sharedFundMember);

        // 선택된 국가에 한국이 없을 경우 공동경비 통화에 기본적으로 KRW 들어가도록 추가
        if (!currencySet.contains("KRW")) {
            currencySet.add("KRW");
        }

        // 선택된 여행 국가들에 대해
        // 각 국가의 통화 기준으로 초기 TotalShared(경비 총액) 엔티티 생성
        currencySet.forEach(currency -> {
            TotalShared totalShared = TotalShared.builder()
                    .trip(trip)
                    .totalSharedAmount(BigDecimal.ZERO)
                    .totalSharedCurrency(currency)
                    .latestModified(LocalDate.now())
                    .build();
            totalSharedRepository.save(totalShared);
        });

        return trip.getId();
    }

    // 8자리 랜덤 코드 생성 함수
    private String generateTripCode() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
    }
}
