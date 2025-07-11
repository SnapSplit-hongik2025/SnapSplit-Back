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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    @Value("${exchange-rate.auth-key}")
    private String authKey;

    public ExchangeRateResponse fetchExchangeRate(String base) {

        String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String url = "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON"
                + "?authkey=" + authKey
                + "&searchdate=" + searchDate
                + "&data=AP01";

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        JSONParser parser = new JSONParser();
        JSONArray arr;
        try {
            arr = (JSONArray) parser.parse(response);
        } catch (ParseException e) {
            throw new RuntimeException("JSON 파싱 실패!", e);
        }

        JSONObject currencyObj = null;
        if ("JPY".equalsIgnoreCase(base)) {
            currencyObj = findCurrency(arr, "JPY(100)");
        } else if ("USD".equalsIgnoreCase(base)) {
            currencyObj = findCurrency(arr, "USD");
        } else {
            throw new IllegalArgumentException("지원하지 않는 통화 코드: " + base);
        }

        double dealBaseRate = Double.parseDouble(currencyObj.getAsString("deal_bas_r").replace(",", ""));
        double rateToKrw = "JPY".equalsIgnoreCase(base) ? dealBaseRate / 100 : dealBaseRate;

        log.info("조회된 {} 환율: {}", base.toUpperCase(), rateToKrw);

        return ExchangeRateResponse.builder()
                .base(base.toUpperCase())
                .rateToKrw(rateToKrw)
                .date(LocalDate.now().toString())
                .build();
    }

    private JSONObject findCurrency(JSONArray arr, String curUnit) {
        for (Object o : arr) {
            JSONObject obj = (JSONObject) o;
            if (obj.getAsString("cur_unit").equals(curUnit)) {
                return obj;
            }
        }
        throw new RuntimeException("통화 정보를 찾을 수 없습니다: " + curUnit);
    }
}
