package com.blanksystem.blankdomain.service.external.other.dto;

import java.util.List;

public record OtherResponse(List<Result> results) {
    public record Result(String data) {
    }
}
