package com.kkl.kklplus.b2b.viomi.entity.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class VioMiCommonRequest<T> implements Serializable {
    private String key;
    private T data;
}
