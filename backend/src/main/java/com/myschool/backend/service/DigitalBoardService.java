package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.DigitalBoard;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.repository.DigitalBoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DigitalBoardService {

    @Autowired private DigitalBoardRepository digitalBoardRepository;

    public Map<String, Object> listBoards(User currentUser) {
        List<DigitalBoard> boards = digitalBoardRepository.findByUserId(currentUser.getId());
        return Map.of(
                "data", boards.stream().map(this::buildBoardResponse).collect(Collectors.toList()),
                "total", boards.size()
        );
    }

    public Map<String, Object> getBoard(String boardId, User currentUser) {
        DigitalBoard board = digitalBoardRepository.findByIdAndUserId(boardId, currentUser.getId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        return buildBoardResponse(board);
    }

    public Map<String, Object> saveBoard(Map<String, Object> body, User currentUser) {
        String id = (String) body.get("id");
        String name = (String) body.get("name");
        String data = body.get("data") != null ? body.get("data").toString() : null;
        String thumbnail = (String) body.get("thumbnail");

        if (id != null && !id.isEmpty()) {
            Optional<DigitalBoard> existing = digitalBoardRepository.findByIdAndUserId(id, currentUser.getId());
            if (existing.isPresent()) {
                DigitalBoard board = existing.get();
                if (name != null) board.setName(name);
                if (data != null) board.setData(data);
                if (thumbnail != null) board.setThumbnail(thumbnail);
                board.setUpdatedAt(Instant.now().toString());
                digitalBoardRepository.save(board);
                return Map.of("message", "Board updated", "id", board.getId());
            }
        }

        String boardId = UUID.randomUUID().toString();
        DigitalBoard board = DigitalBoard.builder()
                .id(boardId)
                .userId(currentUser.getId())
                .name(name != null ? name : "Untitled Board")
                .data(data)
                .thumbnail(thumbnail)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        digitalBoardRepository.save(board);
        return Map.of("message", "Board saved", "id", boardId);
    }

    public Map<String, Object> deleteBoard(String boardId, User currentUser) {
        DigitalBoard board = digitalBoardRepository.findByIdAndUserId(boardId, currentUser.getId())
                .orElseThrow(() -> new AppException("Board not found", HttpStatus.NOT_FOUND));
        digitalBoardRepository.delete(board);
        return Map.of("message", "Board deleted");
    }

    private Map<String, Object> buildBoardResponse(DigitalBoard b) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", b.getId());
        map.put("name", b.getName());
        map.put("data", b.getData());
        map.put("thumbnail", b.getThumbnail());
        map.put("createdAt", b.getCreatedAt());
        map.put("updatedAt", b.getUpdatedAt());
        return map;
    }
}
