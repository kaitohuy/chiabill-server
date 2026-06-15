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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${ocrspace.api-key:}")
    private String ocrApiKey;

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Override
    public Map<String, Object> scanReceipt(byte[] imageBytes, String mimeType, List<String> availableCategories) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException("Dữ liệu hình ảnh không hợp lệ.");
        }

        // Nếu có cấu hình OCR.space và DeepSeek thì ưu tiên chạy luồng mới
        if (ocrApiKey != null && !ocrApiKey.trim().isEmpty() &&
            deepseekApiKey != null && !deepseekApiKey.trim().isEmpty()) {
            log.info("Using OCR.space + DeepSeek API for bill scanning");
            return scanWithOcrSpaceAndDeepSeek(imageBytes, mimeType, availableCategories);
        }

        // Fallback sang Gemini
        log.info("Using Gemini API for bill scanning");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is not configured");
            throw new BusinessException("Hệ thống AI chưa được cấu hình khóa API. Vui lòng liên hệ quản trị viên.");
        }

        try {
            // Xây dựng prompt
            String categoriesList = String.join(", ", availableCategories);
            String prompt = "Bạn là trợ lý ảo AI chuyên phân tích hóa đơn (receipt) cho ứng dụng quản lý chi tiêu nhóm.\n" +
                    "Hãy phân tích ảnh này và trích xuất các thông tin sau:\n" +
                    "1. Kiểm tra xem ảnh này có phải là ảnh chụp hóa đơn, biên lai, chứng từ, vé, hóa đơn chuyển khoản, hoặc bảng kê chi tiêu/thanh toán hay không. Trả về giá trị 'isReceipt' là true nếu đúng, hoặc false nếu không phải (ví dụ: ảnh tự sướng, phong cảnh, động vật, đồ vật ngẫu nhiên không liên quan đến chi tiêu).\n" +
                    "2. Tổng số tiền thanh toán (totalAmount) - trả về dạng số thực (double/float). Hãy tìm dòng số tiền thanh toán cuối cùng (Tổng cộng/Total/Thành tiền/Cộng). Nếu hóa đơn dùng đơn vị tiền tệ khác (như USD, EUR, THB...) nhưng số tiền ghi rõ ràng, hãy trích xuất đúng con số thô đó (không cần tự quy đổi sang VND).\n" +
                    "3. Nội dung/Mô tả ngắn gọn (description) - ví dụ: tên cửa hàng/quán ăn + món ăn/mặt hàng chính hoặc tóm tắt nội dung chi tiêu (ví dụ: 'Starbucks Coffee - Cà phê', 'Grab - Di chuyển', 'Circle K - Mua sắm'). Mô tả ngắn gọn, súc tích bằng tiếng Việt.\n" +
                    "4. Gợi ý tên danh mục (categoryName) - Hãy đối chiếu nội dung hóa đơn và chọn danh mục phù hợp nhất từ danh sách danh mục có sẵn sau đây: [" + categoriesList + "]. Nếu không có danh mục nào thực sự khớp, hãy chọn 'Chi phí phát sinh'.\n" +
                    "5. Ngày chi tiêu (expenseDate) - Hãy tìm ngày giao dịch, ngày in, ngày thanh toán, date,... trên hóa đơn và trả về định dạng chuỗi 'yyyy-MM-dd' (ví dụ: '2026-06-15'). Nếu không tìm thấy, hãy trả về null.\n\n" +
                    "Đầu ra PHẢI LÀ một chuỗi JSON hợp lệ với 5 trường: 'isReceipt', 'totalAmount', 'description', 'categoryName', 'expenseDate'. Không kèm theo bất kỳ văn bản giải thích hay markdown code block nào.";

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
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
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
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.warn("Gemini HttpServerError: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Dịch vụ AI của Google đang quá tải hoặc tạm thời gián đoạn (Lỗi " + e.getStatusCode().value() + "). Vui lòng thử lại sau hoặc nhập tay.");
        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
            throw new BusinessException("Không thể quét hóa đơn tự động lúc này. Vui lòng tự nhập tay thông tin.");
        }
    }

    private Map<String, Object> scanWithOcrSpaceAndDeepSeek(byte[] imageBytes, String mimeType, List<String> availableCategories) {
        String ocrText = callOcrSpace(imageBytes, mimeType);
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new BusinessException("OCR.space không thể nhận diện được chữ nào từ hình ảnh này. Vui lòng tự nhập tay.");
        }

        log.info("OCR.space extracted text length: {}", ocrText.length());
        return callDeepSeek(ocrText, availableCategories);
    }

    private String callOcrSpace(byte[] imageBytes, String mimeType) {
        byte[] processedBytes = imageBytes;
        if (processedBytes.length > 800 * 1024) {
            log.info("Image size ({} KB) exceeds 800KB, compressing and resizing...", processedBytes.length / 1024);
            processedBytes = compressAndResizeImage(processedBytes);
        }

        // Kiểm tra dung lượng file đối với tài khoản Free (giới hạn 1024 KB ~ 1MB)
        if (processedBytes.length > 1024 * 1024) {
            log.warn("Image size ({} bytes) exceeds OCR.space Free limit of 1MB after compression", processedBytes.length);
            throw new BusinessException("Kích thước ảnh quá lớn (" + (processedBytes.length / 1024) + " KB) và không thể nén xuống dưới 1MB. Vui lòng gửi ảnh nhỏ hơn hoặc tự nhập tay.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", ocrApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(processedBytes) {
                @Override
                public String getFilename() {
                    return "receipt.jpg";
                }
            };
            
            // Đặt Content-Type cụ thể cho phần file upload để tránh lỗi nhận diện của OCR.space
            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(MediaType.parseMediaType(mimeType != null ? mimeType : "image/jpeg"));
            HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(resource, partHeaders);
            
            body.add("file", fileEntity);
            body.add("apikey", ocrApiKey);
            body.add("language", "vnm");
            body.add("OCREngine", "2");
            body.add("isOverlayRequired", "false");
            body.add("scale", "true");
            body.add("detectOrientation", "true");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            
            log.info("Sending image to OCR.space API...");
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.ocr.space/parse/image", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("OCR.space response body: {}", response.getBody());
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                if (rootNode.path("IsErroredOnProcessing").asBoolean()) {
                    JsonNode errorNode = rootNode.path("ErrorMessage");
                    String errorMsg = errorNode.isArray() ? errorNode.toString() : errorNode.asText();
                    log.error("OCR.space API error response: {}", response.getBody());
                    throw new BusinessException("Dịch vụ OCR báo lỗi: " + errorMsg);
                }

                JsonNode parsedResults = rootNode.path("ParsedResults");
                if (parsedResults.isArray() && !parsedResults.isEmpty()) {
                    return parsedResults.get(0).path("ParsedText").asText();
                }
            }
            log.error("OCR.space failed response: {}", response.getStatusCode());
            throw new BusinessException("Dịch vụ OCR.space phản hồi thất bại.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling OCR.space API: ", e);
            throw new BusinessException("Lỗi kết nối dịch vụ OCR.space. Vui lòng tự nhập tay.");
        }
    }

    private byte[] compressAndResizeImage(byte[] originalBytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                log.warn("Could not read image bytes, returning original");
                return originalBytes;
            }

            // 1. Resize nếu ảnh quá lớn (giới hạn 1600px cạnh dài nhất)
            BufferedImage processedImage = resizeImage(image);

            // 2. Nén chất lượng JPEG xuống 60%
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                log.warn("No JPEG ImageWriter found, returning original bytes");
                return originalBytes;
            }
            ImageWriter writer = writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType("JPEG");
                param.setCompressionQuality(0.6f);
            }

            writer.write(null, new IIOImage(processedImage, null, null), param);
            writer.dispose();
            ios.close();

            byte[] compressedBytes = baos.toByteArray();
            log.info("Image optimized: {} KB -> {} KB", originalBytes.length / 1024, compressedBytes.length / 1024);
            return compressedBytes;
        } catch (Exception e) {
            log.error("Failed to optimize image: ", e);
            return originalBytes;
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage) {
        int maxBoundary = 1600;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        if (width <= maxBoundary && height <= maxBoundary) {
            return originalImage;
        }
        
        int newWidth;
        int newHeight;
        if (width > height) {
            newWidth = maxBoundary;
            newHeight = (height * maxBoundary) / width;
        } else {
            newHeight = maxBoundary;
            newWidth = (width * maxBoundary) / height;
        }
        
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, java.awt.Color.WHITE, null);
        g.dispose();
        
        return resizedImage;
    }

    private Map<String, Object> callDeepSeek(String ocrText, List<String> availableCategories) {
        try {
            String categoriesList = String.join(", ", availableCategories);
            String systemPrompt = "Bạn là trợ lý ảo AI chuyên phân tích văn bản OCR thô được trích xuất từ hóa đơn (receipt), biên lai chuyển khoản hoặc bảng kê chi tiêu.\n" +
                    "Hãy phân tích văn bản OCR và trả về thông tin dưới dạng JSON.\n" +
                    "Cấu trúc JSON đầu ra PHẢI LÀ một đối tượng JSON hợp lệ chứa chính xác 5 trường:\n" +
                    "1. 'isReceipt' (boolean): true nếu văn bản thể hiện nội dung hóa đơn, biên lai, vé, hóa đơn chuyển tiền/thanh toán, bảng kê chi tiêu; false nếu là nội dung ngẫu nhiên khác.\n" +
                    "2. 'totalAmount' (double): Tổng số tiền thanh toán cuối cùng hoặc số tiền chuyển khoản được ghi trong hóa đơn. Hãy tìm dòng tổng tiền (ví dụ: Tổng cộng, Thành tiền, Total, Amount, Số tiền). Nếu không tìm thấy, trả về 0.0.\n" +
                    "3. 'description' (string): Tóm tắt nội dung chi tiêu bằng tiếng Việt ngắn gọn, súc tích (ví dụ: tên cửa hàng/dịch vụ + mặt hàng chính, tối đa 50 ký tự, ví dụ: 'Starbucks Coffee - Cà phê', 'Grab - Di chuyển').\n" +
                    "4. 'categoryName' (string): Gợi ý tên danh mục phù hợp nhất từ danh sách danh mục có sẵn sau: [" + categoriesList + "]. Nếu không có danh mục nào phù hợp, trả về 'Chi phí phát sinh'.\n" +
                    "5. 'expenseDate' (string): Ngày chi tiêu định dạng yyyy-MM-dd (ví dụ: '2026-06-15'). Hãy tìm trong hóa đơn ngày giao dịch, ngày in, ngày thanh toán, date, vv. Nếu không tìm thấy, hãy trả về null.\n\n" +
                    "Đầu ra PHẢI LÀ một chuỗi JSON hợp lệ. Không kèm theo bất kỳ văn bản giải thích hay markdown code block nào.";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepseekModel);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Văn bản OCR thô trích xuất từ ảnh:\n" + ocrText);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepseekApiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            log.info("Sending prompt to DeepSeek API with model: {}...", deepseekModel);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.deepseek.com/chat/completions", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String jsonText = rootNode.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();

                jsonText = cleanJsonText(jsonText);
                log.info("DeepSeek cleaned response text: {}", jsonText);

                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(jsonText, Map.class);
                return result;
            } else {
                log.error("DeepSeek API failed response: {}", response.getStatusCode());
                throw new BusinessException("Dịch vụ DeepSeek phản hồi thất bại.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("DeepSeek API Rate limit: {}", e.getMessage());
            throw new BusinessException("Dịch vụ DeepSeek giới hạn tần suất gọi. Vui lòng tự nhập tay.");
        } catch (HttpClientErrorException e) {
            log.error("DeepSeek HttpClientError: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException("Lỗi kết nối dịch vụ DeepSeek: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error calling DeepSeek API: ", e);
            throw new BusinessException("Lỗi xử lý DeepSeek. Vui lòng tự nhập tay.");
        }
    }

    private String cleanJsonText(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
