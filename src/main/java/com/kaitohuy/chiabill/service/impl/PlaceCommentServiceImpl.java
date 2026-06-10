package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.PlaceCommentRequest;
import com.kaitohuy.chiabill.dto.response.PlaceCommentResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.entity.Place;
import com.kaitohuy.chiabill.entity.PlaceComment;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.PlaceCommentRepository;
import com.kaitohuy.chiabill.repository.PlaceCommentLikeRepository;
import com.kaitohuy.chiabill.repository.PlaceRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.PlaceCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlaceCommentServiceImpl implements PlaceCommentService {

    private final PlaceCommentRepository placeCommentRepository;
    private final PlaceCommentLikeRepository placeCommentLikeRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    @Override
    public Page<PlaceCommentResponse> getComments(Long placeId, Long currentUserId, Pageable pageable) {
        return placeCommentRepository.findAllByPlaceIdAndIsDeletedFalseAndParentIdIsNull(placeId, pageable)
                .map(comment -> mapToResponseWithReplies(comment, currentUserId));
    }

    @Override
    @Transactional
    public PlaceCommentResponse addComment(Long placeId, PlaceCommentRequest request, Long userId) {
        Place place = placeRepository.findByIdAndIsDeletedFalse(placeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy địa điểm"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        placeCommentRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(lastComment -> {
                    if (Duration.between(lastComment.getCreatedAt(), LocalDateTime.now()).getSeconds() < 10) {
                        throw new BusinessException("Vui lòng chờ 10 giây trước khi bình luận tiếp");
                    }
                });

        PlaceComment comment = PlaceComment.builder()
                .place(place)
                .user(user)
                .content(request.getContent())
                .build();

        return mapToResponse(placeCommentRepository.save(comment), userId);
    }

    @Override
    @Transactional
    public PlaceCommentResponse replyComment(Long commentId, PlaceCommentRequest request, Long userId) {
        PlaceComment parentComment = placeCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bình luận gốc"));
                
        if (parentComment.getParentId() != null) {
            // Chỉ hỗ trợ 1 cấp reply, nếu reply vào 1 reply thì gán parent là parent của reply đó
            parentComment = placeCommentRepository.findById(parentComment.getParentId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy bình luận gốc"));
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        placeCommentRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(lastComment -> {
                    if (Duration.between(lastComment.getCreatedAt(), LocalDateTime.now()).getSeconds() < 10) {
                        throw new BusinessException("Vui lòng chờ 10 giây trước khi bình luận tiếp");
                    }
                });

        PlaceComment reply = PlaceComment.builder()
                .place(parentComment.getPlace())
                .user(user)
                .content(request.getContent())
                .parentId(parentComment.getId())
                .build();

        return mapToResponse(placeCommentRepository.save(reply), userId);
    }

    @Override
    @Transactional
    public void toggleLikeComment(Long commentId, Long userId) {
        PlaceComment comment = placeCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bình luận"));
        
        var existingLike = placeCommentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        
        if (existingLike.isPresent()) {
            placeCommentLikeRepository.delete(existingLike.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
                
            com.kaitohuy.chiabill.entity.PlaceCommentLike like = com.kaitohuy.chiabill.entity.PlaceCommentLike.builder()
                .user(user)
                .comment(comment)
                .build();
            placeCommentLikeRepository.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
        }
        placeCommentRepository.save(comment);
    }

    @Override
    @Transactional
    public PlaceCommentResponse updateComment(Long commentId, PlaceCommentRequest request, Long userId) {
        PlaceComment comment = placeCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bình luận"));

        if (comment.getIsDeleted() || !comment.getUser().getId().equals(userId)) {
            throw new BusinessException("Không có quyền chỉnh sửa");
        }

        comment.setContent(request.getContent());
        return mapToResponse(placeCommentRepository.save(comment), userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PlaceComment comment = placeCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy bình luận"));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));
        boolean isAdmin = "ADMIN".equals(actor.getRole());

        // Let creator of the comment OR the creator of the Place OR admin delete the comment
        boolean isCommentOwner = comment.getUser().getId().equals(userId);
        boolean isPlaceOwner = comment.getPlace().getCreator() != null && comment.getPlace().getCreator().getId().equals(userId);

        if (comment.getIsDeleted() || (!isCommentOwner && !isPlaceOwner && !isAdmin)) {
            throw new BusinessException("Không có quyền xóa");
        }

        comment.setIsDeleted(true);
        placeCommentRepository.save(comment);
    }

    @Override
    public Page<PlaceCommentResponse> getAllComments(Pageable pageable) {
        return placeCommentRepository.findAllByIsDeletedFalse(pageable)
                .map(comment -> mapToResponse(comment, null));
    }

    private PlaceCommentResponse mapToResponse(PlaceComment comment, Long currentUserId) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(comment.getUser().getId());
        userResponse.setName(comment.getUser().getName());
        userResponse.setEmail(comment.getUser().getEmail());
        userResponse.setAvatarUrl(comment.getUser().getAvatarUrl());
        userResponse.setIsGhost(comment.getUser().getIsGhost());
        
        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = placeCommentLikeRepository.existsByUserIdAndCommentId(currentUserId, comment.getId());
        }

        return PlaceCommentResponse.builder()
                .id(comment.getId())
                .placeId(comment.getPlace().getId())
                .placeName(comment.getPlace().getName())
                .user(userResponse)
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0)
                .isLikedByCurrentUser(isLiked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
    
    private PlaceCommentResponse mapToResponseWithReplies(PlaceComment comment, Long currentUserId) {
        PlaceCommentResponse response = mapToResponse(comment, currentUserId);
        
        List<PlaceComment> rawReplies = placeCommentRepository.findAllByParentIdAndIsDeletedFalse(comment.getId());
        List<PlaceCommentResponse> replies = rawReplies.stream()
                .map(reply -> mapToResponse(reply, currentUserId))
                .collect(Collectors.toList());
                
        response.setReplies(replies);
        return response;
    }
}
