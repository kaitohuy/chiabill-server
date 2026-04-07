package com.kaitohuy.chiabill.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeepLinkController {

    @Value("${download_link}")
    private String downloadLink;

    @Value("${base_url}")
    private String baseUrl;

    @GetMapping(value = "/join/{inviteCode}", produces = MediaType.TEXT_HTML_VALUE)
    public String joinTripLandingPage(@PathVariable String inviteCode) {
        String intentUrl = "intent://chiabill-server.onrender.com/join/" + inviteCode + "#Intent;scheme=https;package=com.kaitohuy.chiabill;end";
        try {
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/deeplink.html");
            String html = new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return html
                    .replace("{{intentUrl}}", intentUrl)
                    .replace("{{downloadLink}}", downloadLink != null ? downloadLink : "#");
        } catch (Exception e) {
            return "<h2>Đã xảy ra lỗi khi tải trang mời. Vui lòng mở ứng dụng ChiaBill của bạn!</h2>";
        }
    }
}
