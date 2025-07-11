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
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    @Value("${exchange-rate.auth-key}")
    private String authKey;

    public ExchangeRateResponse fetchExchangeRate(String base) {
        log.info("요청받은 통화 코드 base: {}", base);
        String searchDate = getLatestBusinessDay();
        RestTemplate restTemplate = new RestTemplate();

        String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + authKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        String response = restTemplate.getForObject(url, String.class);
        log.info("환율 API 응답 원문: {}", response);
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
            String curUnit = obj.getAsString("cur_unit");
            log.info("응답 cur_unit: {}", curUnit);
        }

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

        log.info("조회된 {} 환율: {}", base.toUpperCase(), rate);

        return ExchangeRateResponse.builder()
                .base(base.toUpperCase())
                .rateToKrw(rate.doubleValue())
                .date(LocalDate.now().toString())
                .build();
    }

    private String getLatestBusinessDay() {
        LocalDate today = LocalDate.now();
        // 어제 날짜 기준
        LocalDate target = today.minusDays(1);

        // 어제가 토요일이면 → 금요일
        if (target.getDayOfWeek().getValue() == 6) {
            target = target.minusDays(1);
        }
        // 어제가 일요일이면 → 금요일
        else if (target.getDayOfWeek().getValue() == 7) {
            target = target.minusDays(2);
        }

        return target.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
