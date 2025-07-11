package com.snapsplit.backend.feature.getExchangeRate.service;

import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    @Value("${exchange-rate.auth-key}")
    private String authKey;

    public ExchangeRateResponse fetchExchangeRate(String base) {
        String searchDate = getLatestBusinessDay();
        RestTemplate restTemplate = new RestTemplate();

        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + authKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        String response = restTemplate.getForObject(url, String.class);

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

        return ExchangeRateResponse.builder()
                .base(base.toUpperCase())
                .rateToKrw(rate.doubleValue())
                .date(LocalDate.now().toString())
                .build();
    }

    // 비영업일 또는 영업당일 11시 이전 요청 시 날짜 보정
    private String getLatestBusinessDay() {
        // 현재 한국 날짜와 시간
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Seoul"));

        // 평일 오전 11시 이전이면 전일 기준
        if (today.getDayOfWeek().getValue() <= 5 && nowTime.isBefore(LocalTime.of(11, 0))) {
            today = today.minusDays(1);
        }

        // 주말 보정
        if (today.getDayOfWeek().getValue() == 6) { // 토요일
            today = today.minusDays(1);
        } else if (today.getDayOfWeek().getValue() == 7) { // 일요일
            today = today.minusDays(2);
        }

        return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
