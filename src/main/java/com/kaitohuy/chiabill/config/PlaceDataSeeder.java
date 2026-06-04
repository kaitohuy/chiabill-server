package com.kaitohuy.chiabill.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceImage;
import com.kaitohuy.chiabill.repository.PlaceImageRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Slf4j
@Component
public class PlaceDataSeeder {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;

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

            ExecutorService executorService = Executors.newFixedThreadPool(10);
            log.info("Khởi chạy thread pool với 10 workers để nạp song song {} địa điểm lên Cloudinary...", requests.size());

            for (PlaceRequest req : requests) {
                executorService.submit(() -> {
                    try {
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

                        final Place savedPlace = placeRepository.save(place);

                        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
                            for (String url : req.getImageUrls()) {
                                String cloudinaryUrl = cloudinaryService.uploadImageFromUrl(url);
                                PlaceImage img = PlaceImage.builder()
                                        .place(savedPlace)
                                        .imageUrl(cloudinaryUrl)
                                        .album("Khác")
                                        .build();
                                placeImageRepository.save(img);
                            }
                        }
                    } catch (Exception ex) {
                        log.error("Lỗi khi import địa điểm song song [{}]: ", req.getName(), ex);
                    }
                });
            }

            executorService.shutdown();
            log.info("Đang nạp dữ liệu chạy ngầm dưới background...");

        } catch (Exception e) {
            log.error("Lỗi khi đọc hoặc ghi file places.json: ", e);
        }
    }
}