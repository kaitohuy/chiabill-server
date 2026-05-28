package com.kaitohuy.chiabill.service.interfaces;

public interface ExportService {
    byte[] exportTripToExcel(Long tripId, Long userId, boolean includeDetails, boolean includeSettlement);
    byte[] exportTripToPdf(Long tripId, Long userId, boolean includeDetails, boolean includeSettlement);
}
