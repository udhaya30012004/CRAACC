package com.contractanalysis.service;

import com.contractanalysis.model.Contract;
import com.contractanalysis.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private GeminiService geminiService;

    public Contract analyzeAndSaveContract(String text) {
        Contract analyzedContract = geminiService.analyzeContract(text);
        return contractRepository.save(analyzedContract);
    }

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Contract getContractById(Long id) {
        return contractRepository.findById(id).orElse(null);
    }
}