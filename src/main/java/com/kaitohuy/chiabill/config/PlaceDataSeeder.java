package com.kaitohuy.chiabill.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceImage;
import com.kaitohuy.chiabill.repository.PlaceImageRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class PlaceDataSeeder {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seedPlaces() {
        if (placeRepository.count() > 0) {
            log.info("Bảng places đã có dữ liệu, bỏ qua bước seeder.");
            return;
        }

        log.info("Bắt đầu nạp dữ liệu mồi cho places từ file JSON...");

        try {
            ClassPathResource resource = new ClassPathResource("places.json");
            if (!resource.exists()) {
                log.warn("Không tìm thấy file places.json trong thư mục resources. Bỏ qua seeder.");
                return;
            }

            InputStream inputStream = resource.getInputStream();
            List<PlaceRequest> requests = objectMapper.readValue(inputStream, new TypeReference<List<PlaceRequest>>() {});

            for (PlaceRequest req : requests) {
                Place place = Place.builder()
                        .name(req.getName())
                        .category(req.getCategory())
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .city(req.getCity())
                        .summary(req.getSummary())
                        .ticketPrices(req.getTicketPrices())
                        .openingHours(req.getOpeningHours())
                        .creator(null) // Null means System seeded
                        .build();

                place = placeRepository.save(place);

                if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
                    for (String url : req.getImageUrls()) {
                        PlaceImage img = PlaceImage.builder()
                                .place(place)
                                .imageUrl(url)
                                .album("Khác")
                                .build();
                        placeImageRepository.save(img);
                    }
                }
            }

            log.info("Đã nạp thành công {} địa điểm vào hệ thống!", requests.size());

        } catch (Exception e) {
            log.error("Lỗi khi đọc hoặc ghi file places.json: ", e);
        }
    }
}