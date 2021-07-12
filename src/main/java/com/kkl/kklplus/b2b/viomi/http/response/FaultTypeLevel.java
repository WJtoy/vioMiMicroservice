package com.kkl.kklplus.b2b.viomi.http.response;

import lombok.Data;

import java.util.List;

@Data
public class FaultTypeLevel {

    private String name;

    private List<FaultTypeLevel> data;

    private String serviceMeasures;

    private String serviceLevel;

}
