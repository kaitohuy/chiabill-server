package com.kaitohuy.chiabill.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaitohuy.chiabill.dto.request.PlaceRequest;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceImage;
import com.kaitohuy.chiabill.repository.PlaceImageRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import com.kaitohuy.chiabill.service.interfaces.PlaceSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceSeedServiceImpl implements PlaceSeedService {

    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;

    @Async
    @Override
    public void seedPlacesAsync(List<PlaceRequest> requests) {
        if (placeRepository.count() >= 300) {
            log.info("Bảng places đã có đủ dữ liệu (>= 300), bỏ qua bước seeder.");
            return;
        }

        List<PlaceRequest> placesToSeed = requests;
        if (placesToSeed == null || placesToSeed.isEmpty()) {
            try {
                ClassPathResource resource = new ClassPathResource("places.json");
                if (!resource.exists()) {
                    log.warn("Không tìm thấy file places.json trong thư mục resources. Bỏ qua seeder.");
                    return;
                }
                InputStream inputStream = resource.getInputStream();
                placesToSeed = objectMapper.readValue(inputStream, new TypeReference<List<PlaceRequest>>() {});
            } catch (Exception e) {
                log.error("Lỗi khi đọc file places.json: ", e);
                return;
            }
        }

        log.info("Bắt đầu nạp dữ liệu mồi cho places...");

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        log.info("Khởi chạy thread pool với 10 workers để nạp song song {} địa điểm lên Cloudinary...", placesToSeed.size());

        for (PlaceRequest req : placesToSeed) {
            executorService.submit(() -> {
                try {
                    if (placeRepository.existsByNameAndCityAndIsDeletedFalse(req.getName(), req.getCity())) {
                        log.debug("Địa điểm [{}] tại [{}] đã tồn tại, bỏ qua.", req.getName(), req.getCity());
                        return;
                    }

                    Place place = Place.builder()
                            .name(req.getName())
                            .category(req.getCategory())
                            .latitude(req.getLatitude())
                            .longitude(req.getLongitude())
                            .city(req.getCity())
                            .summary(req.getSummary())
                            .ticketPrices(req.getTicketPrices())
                            .openingHours(req.getOpeningHours())
                            .creator(null)
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
    }
}
