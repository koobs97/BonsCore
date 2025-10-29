package com.koo.bonscore.biz.auth.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.InetAddress;

@Service
public class GeoIpLocationService {

    private final DatabaseReader dbReader;

    public GeoIpLocationService() throws Exception {
        Resource resource = new ClassPathResource("geodb/GeoLite2-City.mmdb");
        InputStream dbStream = resource.getInputStream();
        dbReader = new DatabaseReader.Builder(dbStream).build();
    }

    public CityResponse getLocation(String ip) throws Exception {
        InetAddress ipAddress = InetAddress.getByName(ip);
        return dbReader.city(ipAddress);
    }
}
