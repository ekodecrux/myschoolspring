package com.myschool.backend.service;

import com.myschool.backend.exception.AppException;
import com.myschool.backend.models.entity.SupportTicket;
import com.myschool.backend.models.entity.User;
import com.myschool.backend.models.entity.UserRole;
import com.myschool.backend.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupportService {

    @Autowired private SupportTicketRepository ticketRepository;
    @Autowired private MongoTemplate mongoTemplate;

    public Map<String, Object> createTicket(Map<String, Object> body, User currentUser) {
        String ticketId = UUID.randomUUID().toString();

        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .userId(currentUser.getId())
                .userEmail(currentUser.getEmail())
                .userName(currentUser.getName())
                .subject((String) body.get("subject"))
                .message((String) body.get("message"))
                .category((String) body.getOrDefault("category", "general"))
                .priority((String) body.getOrDefault("priority", "medium"))
                .status("open")
                .replies(new ArrayList<>())
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        ticketRepository.save(ticket);
        return Map.of("message", "Support ticket created", "ticketId", ticketId);
    }

    public Map<String, Object> listTickets(User currentUser) {
        List<SupportTicket> tickets;
        if (UserRole.SUPER_ADMIN.equals(currentUser.getRole())) {
            tickets = ticketRepository.findAll();
        } else {
            tickets = ticketRepository.findByUserId(currentUser.getId());
        }

        return Map.of(
                "data", tickets.stream().map(this::buildTicketResponse).collect(Collectors.toList()),
                "total", tickets.size()
        );
    }

    public Map<String, Object> replyToTicket(String ticketId, Map<String, Object> body, User currentUser) {
        SupportTicket ticket = ticketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new AppException("Ticket not found", HttpStatus.NOT_FOUND));

        // Only ticket owner or super admin can reply
        if (!ticket.getUserId().equals(currentUser.getId()) && !UserRole.SUPER_ADMIN.equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        Map<String, Object> reply = new HashMap<>();
        reply.put("message", body.get("message"));
        reply.put("by", currentUser.getId());
        reply.put("byName", currentUser.getName());
        reply.put("byRole", currentUser.getRole());
        reply.put("at", Instant.now().toString());

        if (ticket.getReplies() == null) ticket.setReplies(new ArrayList<>());
        ticket.getReplies().add(reply);
        ticket.setUpdatedAt(Instant.now().toString());

        // Update status if admin replied
        if (UserRole.SUPER_ADMIN.equals(currentUser.getRole()) && "open".equals(ticket.getStatus())) {
            ticket.setStatus("in_progress");
        }

        ticketRepository.save(ticket);
        return Map.of("message", "Reply added");
    }

    public Map<String, Object> updateTicketStatus(String ticketId, String status, User currentUser) {
        if (!UserRole.SUPER_ADMIN.equals(currentUser.getRole())) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        SupportTicket ticket = ticketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new AppException("Ticket not found", HttpStatus.NOT_FOUND));

        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now().toString());
        if ("resolved".equals(status)) {
            ticket.setResolvedAt(Instant.now().toString());
        }
        ticketRepository.save(ticket);

        return Map.of("message", "Ticket status updated");
    }

    private Map<String, Object> buildTicketResponse(SupportTicket t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("subject", t.getSubject());
        map.put("message", t.getMessage());
        map.put("category", t.getCategory());
        map.put("priority", t.getPriority());
        map.put("status", t.getStatus());
        map.put("replies", t.getReplies());
        map.put("userEmail", t.getUserEmail());
        map.put("userName", t.getUserName());
        map.put("createdAt", t.getCreatedAt());
        map.put("updatedAt", t.getUpdatedAt());
        return map;
    }
}
