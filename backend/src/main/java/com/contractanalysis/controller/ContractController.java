package com.contractanalysis.controller;

import com.contractanalysis.model.Contract;
import com.contractanalysis.service.ContractService;
import com.contractanalysis.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "http://localhost:3000")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private PdfService pdfService;

    @PostMapping("/analyze")
    public ResponseEntity<Contract> analyzeContract(@RequestBody String contractText) {
        Contract analyzedContract = contractService.analyzeAndSaveContract(contractText);
        return ResponseEntity.ok(analyzedContract);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = pdfService.extractTextFromPdf(file);
            Contract analyzedContract = contractService.analyzeAndSaveContract(extractedText);
            return ResponseEntity.ok(analyzedContract);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to process PDF: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        if (contract != null) {
            return ResponseEntity.ok(contract);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}