package com.kaitohuy.chiabill.service.interfaces;

public interface ExportService {
    byte[] exportTripToExcel(Long tripId, Long userId);
    byte[] exportTripToPdf(Long tripId, Long userId);
}
