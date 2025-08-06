package com.snapsplit.backend.feature.getExchangeRate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final HolidayService holidayService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // JSON 직렬화/역직렬화용

    @Value("${exchange-rate.auth-key}")
    private String authKey;

    @Value("${exchange-rate.timeout}")
    private int timeout;

    public ExchangeRateResponse fetchExchangeRate(List<String> bases) {

        // 환율을 원하는 통화코드 리스트
        List<String> upperBases = new ArrayList<>(bases.stream()
                .map(String::toUpperCase)
                .distinct()
                .toList());

        String searchDate = getLatestBusinessDay();

        // KRW만 단독 요청 시
        if (upperBases.size() == 1 && upperBases.contains("KRW")) {
            return ExchangeRateResponse.builder()
                    .date(searchDate)
                    .rates(List.of(
                            ExchangeRateResponse.ExchangeRateItem.builder()
                                    .code("KRW")
                                    .rateToBase(BigDecimal.ONE)
                                    .build()
                    ))
                    .build();
        }

        // API 요청
        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + authKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        restTemplate.setRequestFactory(factory);

        String response;
        try {
            response = restTemplate.getForObject(url, String.class);
            if (response == null || response.trim().isEmpty()) {
                log.warn("환율 API 응답 비어있음 → Redis fallback 시도");
                return getFromRedis(upperBases, searchDate);
            }
        } catch (Exception e) {
            log.warn("환율 API 호출 실패 → Redis fallback 시도", e);
            return getFromRedis(upperBases, searchDate);
        }

        // 파싱
        JSONParser parser = new JSONParser();
        JSONArray arr;
        try {
            arr = (JSONArray) parser.parse(response);
        } catch (ParseException e) {
            throw new RuntimeException("JSON 파싱에 실패했습니다.", e);
        }

        List<ExchangeRateResponse.ExchangeRateItem> resultRates = new ArrayList<>(upperBases.stream()
                .filter(code -> !code.equalsIgnoreCase("KRW"))
                .map(code -> {
                    String unit = code.equals("JPY") ? "JPY(100)" : code;
                    JSONObject matched = arr.stream()
                            .map(JSONObject.class::cast)
                            .filter(obj -> unit.equalsIgnoreCase(obj.getAsString("cur_unit")))
                            .findFirst()
                            .orElse(null);

                    if (matched == null) throw new IllegalArgumentException("지원하지 않는 통화 코드: " + code);

                    BigDecimal rate = new BigDecimal(matched.getAsString("deal_bas_r").replace(",", ""));
                    if (code.equals("JPY")) rate = rate.divide(BigDecimal.valueOf(100));

                    // Redis에 저장
                    saveToRedis(code, rate, searchDate);

                    return ExchangeRateResponse.ExchangeRateItem.builder()
                            .code(code)
                            .rateToBase(rate)
                            .build();
                })
                .toList());

        if (upperBases.contains("KRW")) {
            resultRates.add(ExchangeRateResponse.ExchangeRateItem.builder()
                    .code("KRW")
                    .rateToBase(BigDecimal.ONE)
                    .build());
        }

        return ExchangeRateResponse.builder()
                .date(searchDate)
                .rates(resultRates)
                .build();

    }

    // 비영업일 보정
    private String getLatestBusinessDay() {

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Seoul"));

        // 평일 오전 11시 이전이면 하루 전날로
        if (today.getDayOfWeek().getValue() <= 5 && nowTime.isBefore(LocalTime.of(11, 0))) {
            today = today.minusDays(1);
        }

        // 주말/공휴일 보정
        while (isNonBusinessDay(today)) {
            today = today.minusDays(1);
        }

        return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private boolean isNonBusinessDay(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6 || holidayService.isHoliday(date);
    }

    // 레디스에 저장
    private void saveToRedis(String code, BigDecimal rate, String date) {
        try {
            String json = objectMapper.writeValueAsString(ExchangeRateResponse.builder()
                    .date(date)
                    .rates(List.of(
                            ExchangeRateResponse.ExchangeRateItem.builder()
                                    .code(code)
                                    .rateToBase(rate)
                                    .build()))
                    .build());
            redisTemplate.opsForValue().set("exchange:" + code, json, Duration.ofHours(24));
            log.info("Redis에 {} 저장 완료", code);
        } catch (Exception e) {
            log.warn("Redis 저장 실패: {}", code, e);
        }
    }


    // Redis Fallback
    private ExchangeRateResponse getFromRedis(List<String> codes, String date) {
        List<ExchangeRateResponse.ExchangeRateItem> cachedItems = codes.stream()
                .map(code -> {
                    try {
                        String json = redisTemplate.opsForValue().get("exchange:" + code);
                        if (json == null) throw new RuntimeException("캐시 없음: " + code);
                        ExchangeRateResponse cached = objectMapper.readValue(json, ExchangeRateResponse.class);
                        return cached.getRates().get(0); // 각 통화별 하나씩만 들어있음
                    } catch (Exception e) {
                        throw new RuntimeException("Redis 캐시 조회 실패: " + code, e);
                    }
                })
                .toList();

        return ExchangeRateResponse.builder()
                .date(date)
                .rates(cachedItems)
                .build();
    }

}
