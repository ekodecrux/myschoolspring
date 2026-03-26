package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.DigitalBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/digital-board")
public class DigitalBoardController {

    @Autowired
    private DigitalBoardService digitalBoardService;

    /**
     * GET /api/rest/digital-board/list
     * List all digital boards for current user
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listBoards(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = digitalBoardService.listBoards(currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/digital-board/{boardId}
     * Get a specific digital board
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<Map<String, Object>> getBoard(
            @PathVariable String boardId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = digitalBoardService.getBoard(boardId, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/digital-board/save
     * Save or update a digital board
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveBoard(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = digitalBoardService.saveBoard(body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/rest/digital-board/{boardId}
     * Delete a digital board
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Map<String, Object>> deleteBoard(
            @PathVariable String boardId,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = digitalBoardService.deleteBoard(boardId, currentUser);
        return ResponseEntity.ok(result);
    }
}
