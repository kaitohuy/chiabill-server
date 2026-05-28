package com.kaitohuy.chiabill.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ImportMembersRequest {
    @NotEmpty(message = "Danh sách thành viên không được để trống")
    private List<Long> userIds;
}
