# Hướng Dẫn Phát Triển Hệ Thống Đa Ngôn Ngữ (Localization Developer Guide)

Tài liệu này hướng dẫn lập trình viên cách phát triển, mở rộng và bảo trì hệ thống bản địa hóa (localization) đa ngôn ngữ (tiếng Việt & tiếng Anh) trong dự án Chiabill cho cả Backend (Spring Boot) và Frontend (Flutter).

---

## 1. Bản Địa Hóa Mã Lỗi Nghiệp Vụ (Business Error Codes)

Hệ thống sử dụng cơ chế xử lý lỗi tập trung: Backend chỉ trả về mã lỗi (`errorCode`) và các tham số kỹ thuật, trong khi Flutter chịu trách nhiệm hiển thị chuỗi dịch tương ứng cho người dùng.

### Các bước thêm một mã lỗi mới:

#### Bước 1: Khai báo mã lỗi ở Backend
Mở file [ErrorCode.java](file:///d:/Code/Java/chia-bill/src/main/java/com/kaitohuy/chiabill/exception/ErrorCode.java) và bổ sung mã lỗi mới vào enum:
```java
public enum ErrorCode {
    NEW_ERROR_CODE("NEW_ERROR_CODE", "Thông báo lỗi tiếng Việt mặc định (cho logs)", HttpStatus.BAD_REQUEST),
    // ...
}
```

#### Bước 2: Ném ngoại lệ trong Logic nghiệp vụ
Sử dụng `BusinessException` với enum vừa định nghĩa:
```java
throw new BusinessException(ErrorCode.NEW_ERROR_CODE);
```

#### Bước 3: Cấu hình chuỗi dịch ở Flutter
Mở file [app_translations.dart](file:///d:/Code/Android/chiabill/lib/utils/app_translations.dart) và thêm mã lỗi vào cả hai bản dịch `vi_VN` và `en_US`:
```dart
  Map<String, String> get vi => {
    'NEW_ERROR_CODE': 'Thông báo lỗi hiển thị cho người dùng bằng tiếng Việt.',
  };

  Map<String, String> get en => {
    'NEW_ERROR_CODE': 'Error message displayed to the user in English.',
  };
```

---

## 2. Bản Bản Địa Hóa Push Notification (FCM & Database)

Push Notifications được dịch tự động ở tầng Service của Backend trước khi lưu vào Cơ sở dữ liệu và gửi đến thiết bị thông qua Firebase Cloud Messaging (FCM). Điều này đảm bảo lịch sử thông báo lưu trên DB khớp với thông báo đẩy mà người dùng nhìn thấy.

### Các bước thêm mẫu thông báo mới:

#### Bước 1: Khai báo loại thông báo
Mở file `NotificationType.java` ở Backend và thêm type mới nếu cần.

#### Bước 2: Định nghĩa chuỗi dịch tự động
Mở file [NotificationServiceImpl.java](file:///d:/Code/Java/chia-bill/src/main/java/com/kaitohuy/chiabill/service/impl/NotificationServiceImpl.java) và cập nhật hai phương thức dịch:

1. **`translateTitle`**:
   Định nghĩa tiêu đề tiếng Anh dựa theo tiêu đề tiếng Việt gốc hoặc theo `NotificationType`:
   ```java
   if (title.startsWith("Tiêu đề tiếng Việt: ")) {
       return "English Title: " + title.substring("Tiêu đề tiếng Việt: ".length());
   }
   ```

2. **`translateBody`**:
   Sử dụng regex hoặc trích xuất chuỗi động (tên người dùng, số tiền...) để dịch nội dung:
   ```java
   if (type == NotificationType.YOUR_NEW_TYPE && body.startsWith("Tên tiếng Việt ")) {
       int idx = body.indexOf(" hành động ");
       if (idx != -1) {
           String name = body.substring("Tên tiếng Việt ".length(), idx);
           return name + " action in English";
       }
   }
   ```

---

## 3. Bản Địa Hóa Email Templates

Chiabill sử dụng HTML templates động được sinh trực tiếp ở Backend cho các tác vụ như gửi thư mời tham gia chuyến đi.

### Cách thức hoạt động và mở rộng:

1. **Tra cứu ngôn ngữ người nhận**:
   Khi gửi email, sử dụng `UserRepository` để tìm kiếm tùy chọn ngôn ngữ dựa trên email người nhận:
   ```java
   String language = userRepository.findByEmail(toEmail)
           .map(User::getLanguage)
           .orElse("vi"); // Mặc định là tiếng Việt
   ```

2. **Chọn chuỗi dịch thích hợp**:
   Sử dụng cờ `isEn` để quyết định giá trị các chuỗi hiển thị:
   ```java
   boolean isEn = "en".equalsIgnoreCase(language);
   String greeting = isEn ? "Hello," : "Chào bạn,";
   String directButtonText = isEn ? "Join Trip Directly" : "Tham Gia Chuyến Đi Trực Tiếp";
   ```

3. **Chèn chuỗi dịch vào HTML**:
   Thay thế các biến động vào mẫu HTML trước khi chuyển cho `MimeMessageHelper`:
   ```java
   helper.setText(htmlContent, true);
   ```
