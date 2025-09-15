package com.koo.bonscore.common.api.kma.subway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulSubwayApiDto {

    @JsonProperty("CardSubwayTime")
    private CardSubwayTime cardSubwayTime;

    @Getter
    @NoArgsConstructor
    public static class CardSubwayTime {
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    public static class Row {
        @JsonProperty("TEN_RIDE_PASGR_NUM") private double tenRide;
        @JsonProperty("TEN_ALIGHT_PASGR_NUM") private double tenAlight;
        @JsonProperty("ELEVEN_RIDE_PASGR_NUM") private double elevenRide;
        @JsonProperty("ELEVEN_ALIGHT_PASGR_NUM") private double elevenAlight;
        @JsonProperty("TWELVE_RIDE_PASGR_NUM") private double twelveRide;
        @JsonProperty("TWELVE_ALIGHT_PASGR_NUM") private double twelveAlight;
        @JsonProperty("THIRTEEN_RIDE_PASGR_NUM") private double thirteenRide;
        @JsonProperty("THIRTEEN_ALIGHT_PASGR_NUM") private double thirteenAlight;
        @JsonProperty("FOURTEEN_RIDE_PASGR_NUM") private double fourteenRide;
        @JsonProperty("FOURTEEN_ALIGHT_PASGR_NUM") private double fourteenAlight;
        @JsonProperty("FIFTEEN_RIDE_PASGR_NUM") private double fifteenRide;
        @JsonProperty("FIFTEEN_ALIGHT_PASGR_NUM") private double fifteenAlight;
        @JsonProperty("SIXTEEN_RIDE_PASGR_NUM") private double sixteenRide;
        @JsonProperty("SIXTEEN_ALIGHT_PASGR_NUM") private double sixteenAlight;
        @JsonProperty("SEVENTEEN_RIDE_PASGR_NUM") private double seventeenRide;
        @JsonProperty("SEVENTEEN_ALIGHT_PASGR_NUM") private double seventeenAlight;
        @JsonProperty("EIGHTEEN_RIDE_PASGR_NUM") private double eighteenRide;
        @JsonProperty("EIGHTEEN_ALIGHT_PASGR_NUM") private double eighteenAlight;
        @JsonProperty("NINETEEN_RIDE_PASGR_NUM") private double nineteenRide;
        @JsonProperty("NINETEEN_ALIGHT_PASGR_NUM") private double nineteenAlight;
        @JsonProperty("TWENTY_RIDE_PASGR_NUM") private double twentyRide;
        @JsonProperty("TWENTY_ALIGHT_PASGR_NUM") private double twentyAlight;
        @JsonProperty("TWENTY_ONE_RIDE_PASGR_NUM") private double twentyOneRide;
        @JsonProperty("TWENTY_ONE_ALIGHT_PASGR_NUM") private double twentyOneAlight;
        @JsonProperty("TWENTY_TWO_RIDE_PASGR_NUM") private double twentyTwoRide;
        @JsonProperty("TWENTY_TWO_ALIGHT_PASGR_NUM") private double twentyTwoAlight;
    }
}
