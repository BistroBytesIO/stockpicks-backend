package com.stockpicks.backend.controller;

import com.stockpicks.backend.entity.ContactMessage;
import com.stockpicks.backend.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "http://localhost:3000")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitContactForm(@Valid @RequestBody ContactMessage contactMessage) {
        try {
            ContactMessage savedMessage = contactService.saveContactMessage(contactMessage);
            return ResponseEntity.ok("Contact message submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error submitting contact message: " + e.getMessage());
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ContactMessage>> getAllContactMessages() {
        List<ContactMessage> messages = contactService.getAllContactMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/unresolved")
    public ResponseEntity<List<ContactMessage>> getUnresolvedContactMessages() {
        List<ContactMessage> messages = contactService.getUnresolvedContactMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<ContactMessage> getContactMessageById(@PathVariable Long id) {
        Optional<ContactMessage> message = contactService.getContactMessageById(id);
        return message.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/messages/{id}/resolve")
    public ResponseEntity<?> markAsResolved(@PathVariable Long id) {
        try {
            ContactMessage resolvedMessage = contactService.markAsResolved(id);
            return ResponseEntity.ok("Contact message marked as resolved");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteContactMessage(@PathVariable Long id) {
        contactService.deleteContactMessage(id);
        return ResponseEntity.ok("Contact message deleted successfully");
    }
}