package com.contractanalysis.repository;

import com.contractanalysis.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
	
	
}