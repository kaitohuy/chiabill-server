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
        
        // KHÔNG dùng String.formatted() vì ký tự % trong CSS gây lỗi UnknownFormatConversionException
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Tham gia chuyến đi - ChiaBill</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap\" rel=\"stylesheet\">"
                + "<style>"
                + ":root { --primary: #6366f1; --primary-dark: #4f46e5; --bg: #0f172a; --card-bg: rgba(30, 41, 59, 0.7); --text: #f8fafc; --text-muted: #94a3b8; }"
                + "* { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Outfit', sans-serif; }"
                + "body { background: var(--bg); background: radial-gradient(circle at top left, #1e1b4b, #0f172a); color: var(--text); height: 100vh; display: flex; align-items: center; justify-content: center; overflow: hidden; }"
                + ".container { max-width: 450px; width: 100%; padding: 2.5rem; background: var(--card-bg); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 2rem; text-align: center; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); animation: fadeIn 0.8s ease-out; }"
                + "@keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }"
                + ".logo { width: 80px; height: 80px; background: linear-gradient(135deg, var(--primary), #a855f7); border-radius: 1.5rem; margin: 0 auto 1.5rem; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; font-weight: 700; color: white; box-shadow: 0 10px 20px rgba(99, 102, 241, 0.3); }"
                + "h1 { font-size: 1.8rem; font-weight: 700; margin-bottom: 0.75rem; background: linear-gradient(to right, #fff, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }"
                + "p { color: var(--text-muted); line-height: 1.6; margin-bottom: 2rem; }"
                + ".btn { display: inline-block; width: 100%; padding: 1rem 2rem; background: linear-gradient(to right, var(--primary), var(--primary-dark)); color: white; text-decoration: none; border-radius: 1rem; font-weight: 600; transition: all 0.3s ease; margin-bottom: 1rem; border: none; cursor: pointer; }"
                + ".btn:hover { transform: translateY(-2px); box-shadow: 0 10px 20px rgba(99, 102, 241, 0.4); }"
                + ".btn-secondary { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); }"
                + ".btn-secondary:hover { background: rgba(255, 255, 255, 0.1); }"
                + ".badge { display: inline-block; padding: 0.25rem 0.75rem; background: rgba(99, 102, 241, 0.15); color: #818cf8; border-radius: 2rem; font-size: 0.8rem; font-weight: 600; margin-bottom: 1rem; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<div class=\"logo\">C</div>"
                + "<div class=\"badge\">LỜI MỜI THAM GIA</div>"
                + "<h1>ChiaBill - Chia sẻ chi phí</h1>"
                + "<p>Bạn đã được mời tham gia một chuyến đi mới. Vui lòng mở ứng dụng ChiaBill hoặc tải xuống để bắt đầu chia sẻ chi phí cùng mọi người!</p>"
                + "<a href=\"" + intentUrl + "\" class=\"btn\">Mở ứng dụng</a>"
                + "<a href=\"" + downloadLink + "\" class=\"btn btn-secondary\">Tải ứng dụng (Google Drive)</a>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
