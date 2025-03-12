package com.contractanalysis.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Compliance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private boolean gdpr;
    private boolean hipaa;
    private boolean pci;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public boolean isGdpr() {
		return gdpr;
	}
	public boolean isHipaa() {
		return hipaa;
	}
	public boolean isPci() {
		return pci;
	}
	public void setHipaa(boolean hippa) {
		
		this.hipaa = hipaa;
	}
	public void setGdpr(boolean gdpr) {
		this.gdpr = gdpr;
		
	}
	public void setPci(boolean pci) {
		this.pci = pci;
		
	}
	
}