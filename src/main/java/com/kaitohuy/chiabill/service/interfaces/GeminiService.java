package com.kaitohuy.chiabill.service.interfaces;

import java.util.List;
import java.util.Map;

public interface GeminiService {
    Map<String, Object> scanReceipt(byte[] imageBytes, String mimeType, List<String> availableCategories);
}
