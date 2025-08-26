package com.koo.bonscore.common.api.weather.config;

import lombok.Getter;

// 위경도 -> 기상청 격자 좌표 변환 유틸리티
public class GpsTransfer {

    private double lat;
    private double lon;

    @Getter
    private int nx;
    @Getter
    private int ny;

    public GpsTransfer(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        transfer();
    }

    private void transfer() {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기_준점 Y좌표(GRID)

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (this.lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = this.lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        this.nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        this.ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
    }
}
