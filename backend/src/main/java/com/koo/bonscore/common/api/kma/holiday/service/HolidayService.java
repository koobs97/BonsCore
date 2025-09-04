package com.koo.bonscore.common.api.kma.holiday.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koo.bonscore.common.api.kma.holiday.dto.res.HolidayResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.cache.annotation.Cacheable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class HolidayService {

    @Value("${api.kma.key}")
    private String serviceKey;

    @Value("${api.kma.holiday.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HolidayService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 오늘이 무슨 요일인지, 그리고 휴일 타입을 구분하여 반환하는 메소드
     * @return HolidayResponseDto containing today's day of week and holiday type
     */
    public HolidayResponseDto getTodayHolidayInfo() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 1. 요일 정보 설정
        String todayDayOfWeek = dayOfWeek.toString(); // 예: MONDAY, SUNDAY

        String holidayType;
        boolean isHolidayOrWeekend = false;

        // 2. 휴일 타입 구분
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            holidayType = "휴일"; // 주말은 "휴일"로 통일
            isHolidayOrWeekend = true;
        } else if (isPublicHoliday(today)) { // 공휴일 API 호출
            holidayType = "공휴일";
            isHolidayOrWeekend = true;
        } else if (dayOfWeek == DayOfWeek.FRIDAY) {
            holidayType = "금요일";
        } else {
            holidayType = "평일";
        }

        return HolidayResponseDto.builder()
                .todayDayOfWeek(todayDayOfWeek)
                .holidayType(holidayType)
                .isHolidayOrWeekend(isHolidayOrWeekend)
                .build();
    }

    /**
     * 특정 날짜가 공휴일인지 확인하는 메소드
     * @param date 확인할 날짜
     * @return true if it's a public holiday, false otherwise
     */
    @Cacheable("holidays") // 캐싱 적용 (optional)
    public boolean isPublicHoliday(LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());

        try {
            String encodedServiceKey = URLDecoder.decode(serviceKey, "UTF-8");
            encodedServiceKey = URLEncoder.encode(encodedServiceKey, "UTF-8");

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl + "/getHoliDeInfo")
                    .queryParam("serviceKey", encodedServiceKey)
                    .queryParam("solYear", year)
                    .queryParam("solMonth", month);

            String responseBody = restTemplate.getForObject(builder.toUriString(), String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String holidayDate = String.valueOf(item.path("locdate").asLong());
                    if (holidayDate.equals(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")))) {
                        return true;
                    }
                }
            } else if (!itemsNode.isMissingNode()) {
                String holidayDate = String.valueOf(itemsNode.path("locdate").asLong());
                if (holidayDate.equals(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")))) {
                    return true;
                }
            }

            return false;

        } catch (UnsupportedEncodingException e) {
            System.err.println("API Service Key Encoding Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Failed to fetch holiday info: " + e.getMessage());
            return false;
        }
    }
}