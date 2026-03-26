package com.myschool.backend.controller;

import com.myschool.backend.models.entity.User;
import com.myschool.backend.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rest/support")
public class SupportController {

    @Autowired
    private SupportService supportService;

    /**
     * POST /api/rest/support/ticket
     * Create a new support ticket
     */
    @PostMapping("/ticket")
    public ResponseEntity<Map<String, Object>> createTicket(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = supportService.createTicket(body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/rest/support/tickets
     * List support tickets (own tickets for users, all for super admin)
     */
    @GetMapping("/tickets")
    public ResponseEntity<Map<String, Object>> listTickets(@AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = supportService.listTickets(currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/rest/support/tickets/{ticketId}/reply
     * Reply to a support ticket
     */
    @PostMapping("/tickets/{ticketId}/reply")
    public ResponseEntity<Map<String, Object>> replyToTicket(
            @PathVariable String ticketId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = supportService.replyToTicket(ticketId, body, currentUser);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/rest/support/tickets/{ticketId}/status
     * Update ticket status (Super Admin only)
     */
    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<Map<String, Object>> updateTicketStatus(
            @PathVariable String ticketId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User currentUser) {
        String status = (String) body.get("status");
        Map<String, Object> result = supportService.updateTicketStatus(ticketId, status, currentUser);
        return ResponseEntity.ok(result);
    }
}
