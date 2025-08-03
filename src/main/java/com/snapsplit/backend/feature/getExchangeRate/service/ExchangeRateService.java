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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    public ExchangeRateResponse fetchExchangeRate(String base) {
        // base가 krw일 경우 그냥 환율 1로 return
        if (base.equalsIgnoreCase("KRW")) {
            return ExchangeRateResponse.builder()
                    .base("KRW")
                    .rateToKrw(1.0)
                    .date(getLatestBusinessDay())
                    .build();
        }
        
        String searchDate = getLatestBusinessDay();
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        restTemplate.setRequestFactory(factory);

        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + authKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        String response;
        try {
            response = restTemplate.getForObject(url, String.class);

            if (response == null || response.trim().isEmpty()) {
                log.warn("환율 API 응답이 비어 있음. Redis 캐시 fallback 시도");

                String redisKey = "exchange:" + base.toUpperCase();
                try {
                    String cachedJson = redisTemplate.opsForValue().get(redisKey);
                    if (cachedJson != null) {
                        ExchangeRateResponse cached = objectMapper.readValue(cachedJson, ExchangeRateResponse.class);
                        log.info("Redis에서 환율 캐시 응답 반환: {}", cached);
                        return cached;
                    }
                } catch (Exception e) {
                    log.warn("Redis 캐시 조회 실패. 무시하고 예외 처리 진행", e);
                }

                throw new RuntimeException("환율 API 응답이 비어있고, Redis 캐시도 없습니다.");
            }

        } catch (ResourceAccessException e) {
            throw new RuntimeException("환율 API 서버에 연결할 수 없습니다.", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("환율 API 호출 실패: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException("환율 API 요청 중 알 수 없는 에러가 발생했습니다.", e);
        }

        JSONParser parser = new JSONParser();
        JSONArray arr;
        try {
            arr = (JSONArray) parser.parse(response);
        } catch (ParseException e) {
            throw new RuntimeException("JSON 파싱에 실패했습니다.", e);
        }

        JSONObject currencyObj = null;
        String targetUnit = base.equalsIgnoreCase("JPY") ? "JPY(100)" : base.toUpperCase();

        for (Object o : arr) {
            JSONObject obj = (JSONObject) o;
            if (obj.getAsString("cur_unit").equalsIgnoreCase(targetUnit)) {
                currencyObj = obj;
                break;
            }
        }

        if (currencyObj == null) {
            throw new IllegalArgumentException("지원하지 않는 통화 코드: " + base);
        }

        String dealBasR = currencyObj.getAsString("deal_bas_r").replace(",", "");
        BigDecimal rate = new BigDecimal(dealBasR);

        // JPY는 100단위 처리
        if (targetUnit.equals("JPY(100)")) {
            rate = rate.divide(BigDecimal.valueOf(100));
        }

        ExchangeRateResponse result = ExchangeRateResponse.builder()
                .base(base.toUpperCase())
                .rateToKrw(rate.doubleValue())
                .date(searchDate)
                .build();

        // Redis에 저장
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set("exchange:" + base.toUpperCase(), json, Duration.ofHours(24));
            log.info("Redis에 환율 저장 완료: {}", json);
        } catch (Exception e) {
            log.warn("Redis 저장 실패. 무시하고 진행", e);
        }

        return result;

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
}
