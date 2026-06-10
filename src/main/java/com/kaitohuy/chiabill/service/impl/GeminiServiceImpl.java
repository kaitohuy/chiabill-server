package com.kaitohuy.chiabill.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.service.interfaces.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Override
    public Map<String, Object> scanReceipt(byte[] imageBytes, String mimeType, List<String> availableCategories) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new BusinessException("Hệ thống AI chưa được cấu hình khóa API. Vui lòng liên hệ quản trị viên.");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException("Dữ liệu hình ảnh không hợp lệ.");
        }

        try {
            // Xây dựng prompt
            String categoriesList = String.join(", ", availableCategories);
            String prompt = "Bạn là trợ lý ảo AI chuyên phân tích hóa đơn (receipt) cho ứng dụng quản lý chi tiêu nhóm.\n" +
                    "Hãy phân tích ảnh này và trích xuất các thông tin sau:\n" +
                    "1. Kiểm tra xem ảnh này có phải là ảnh chụp hóa đơn, biên lai, chứng từ, vé, hóa đơn chuyển khoản, hoặc bảng kê chi tiêu/thanh toán hay không. Trả về giá trị 'isReceipt' là true nếu đúng, hoặc false nếu không phải (ví dụ: ảnh tự sướng, phong cảnh, động vật, đồ vật ngẫu nhiên không liên quan đến chi tiêu).\n" +
                    "2. Tổng số tiền thanh toán (totalAmount) - trả về dạng số thực (double/float). Hãy tìm dòng số tiền thanh toán cuối cùng (Tổng cộng/Total/Thành tiền/Cộng). Nếu hóa đơn dùng đơn vị tiền tệ khác (như USD, EUR, THB...) nhưng số tiền ghi rõ ràng, hãy trích xuất đúng con số thô đó (không cần tự quy đổi sang VND).\n" +
                    "3. Nội dung/Mô tả ngắn gọn (description) - ví dụ: tên cửa hàng/quán ăn + món ăn/mặt hàng chính hoặc tóm tắt nội dung chi tiêu (ví dụ: 'Starbucks Coffee - Cà phê', 'Grab - Di chuyển', 'Circle K - Mua sắm'). Mô tả ngắn gọn, súc tích bằng tiếng Việt.\n" +
                    "4. Gợi ý tên danh mục (categoryName) - Hãy đối chiếu nội dung hóa đơn và chọn danh mục phù hợp nhất từ danh sách danh mục có sẵn sau đây: [" + categoriesList + "]. Nếu không có danh mục nào thực sự khớp, hãy chọn 'Chi phí phát sinh'.\n\n" +
                    "Đầu ra PHẢI LÀ một chuỗi JSON hợp lệ với 4 trường: 'isReceipt', 'totalAmount', 'description', 'categoryName'. Không kèm theo bất kỳ văn bản giải thích hay markdown code block nào.";

            // Xây dựng JSON payload thủ công qua Map để đảm bảo cấu trúc chuẩn của Gemini API
            Map<String, Object> partText = new HashMap<>();
            partText.put("text", prompt);

            Map<String, Object> partImage = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", mimeType != null ? mimeType : "image/jpeg");
            inlineData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            partImage.put("inlineData", inlineData);

            List<Map<String, Object>> parts = Arrays.asList(partText, partImage);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);

            List<Map<String, Object>> contents = Collections.singletonList(content);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", contents);
            requestBody.put("generationConfig", generationConfig);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            // Gửi request tới Gemini API
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String jsonText = rootNode.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();

                log.info("Gemini raw response text: {}", jsonText);
                
                // Parse kết quả trả về thành Map
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(jsonText, Map.class);
                return result;
            } else {
                log.error("Gemini API error, status: {}", response.getStatusCode());
                throw new BusinessException("Lỗi hệ thống AI quét hóa đơn. Vui lòng tự nhập tay.");
            }
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini API Rate limit exceeded: {}", e.getMessage());
            throw new BusinessException("Hệ thống AI đang bị quá tải lượt quét (Rate Limit). Vui lòng thử lại sau hoặc nhập tay thông tin.");
        } catch (HttpClientErrorException e) {
            log.error("Gemini HttpClientError: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException("Lỗi kết nối dịch vụ AI (" + e.getStatusCode() + "). Vui lòng tự nhập tay.");
        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
            throw new BusinessException("Không thể quét hóa đơn tự động lúc này. Vui lòng tự nhập tay thông tin.");
        }
    }
}
