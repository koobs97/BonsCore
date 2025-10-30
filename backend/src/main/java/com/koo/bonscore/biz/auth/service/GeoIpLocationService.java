package com.koo.bonscore.biz.auth.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetAddress;

/**
 * <pre>
 * GeoIpLocationService.java
 * 설명 : IP 주소를 기반으로 지리적 위치 정보(국가, 도시 등)를 조회하는 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-29
 */
@Service
public class GeoIpLocationService {

    private final DatabaseReader dbReader;

    /**
     * 생성자
     * 서비스가 초기화될 때 GeoLite2-City.mmdb 데이터베이스 파일을 로드하여
     * DatabaseReader를 설정
     */
    public GeoIpLocationService() throws Exception {
        Resource resource = new ClassPathResource("geodb/GeoLite2-City.mmdb");
        InputStream dbStream = resource.getInputStream();
        dbReader = new DatabaseReader.Builder(dbStream).build();
    }

    /**
     * 주어진 IP 주소 문자열에 해당하는 위치 정보를 조회
     *
     * @param ip 조회할 IP 주소
     * @return CityResponse 조회된 위치 정보를 담고 있는 객체. 국가, 도시, 좌표 등의 데이터를 포함
     * @throws AddressNotFoundException 데이터베이스에서 해당 IP 주소를 찾을 수 없는 경우
     * @throws Exception 그 외 IP 주소 변환 실패 등 다양한 조회 실패 시
     */
    public CityResponse getLocation(String ip) throws Exception {
        InetAddress ipAddress = InetAddress.getByName(ip);
        return dbReader.city(ipAddress);
    }
}
