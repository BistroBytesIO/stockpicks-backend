package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.ContactMessage;
import com.stockpicks.backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private EmailService emailService;

    public ContactMessage saveContactMessage(ContactMessage contactMessage) {
        contactMessage.setCreatedAt(LocalDateTime.now());
        contactMessage.setResolved(false);
        
        ContactMessage savedMessage = contactMessageRepository.save(contactMessage);
        
        // Send notification email to admin
        emailService.sendContactFormNotification(
            contactMessage.getName(),
            contactMessage.getEmail(),
            contactMessage.getSubject(),
            contactMessage.getMessage()
        );
        
        return savedMessage;
    }

    public List<ContactMessage> getAllContactMessages() {
        return contactMessageRepository.findByOrderByCreatedAtDesc();
    }

    public List<ContactMessage> getUnresolvedContactMessages() {
        return contactMessageRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    public Optional<ContactMessage> getContactMessageById(Long id) {
        return contactMessageRepository.findById(id);
    }

    public ContactMessage markAsResolved(Long id) {
        Optional<ContactMessage> messageOpt = contactMessageRepository.findById(id);
        if (messageOpt.isPresent()) {
            ContactMessage message = messageOpt.get();
            message.setResolved(true);
            message.setResolvedAt(LocalDateTime.now());
            return contactMessageRepository.save(message);
        }
        throw new RuntimeException("Contact message not found with id: " + id);
    }

    public void deleteContactMessage(Long id) {
        contactMessageRepository.deleteById(id);
    }
}