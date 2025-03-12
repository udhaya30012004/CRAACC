package com.contractanalysis.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.contractanalysis.model.Contract;
import com.contractanalysis.model.Risk;
import com.contractanalysis.model.Compliance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class GeminiService {

    @Value("${gemini.api.key:AIzaSyDlwir5wQ7gX7xXV8KOz9GMZT8IEQKk4Ac}")
    private String apiKey;
    
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    public Contract analyzeContract(String text) {
        String prompt = createAnalysisPrompt(text);
        String response = callGeminiApi(prompt);
        return parseGeminiResponse(response, text);
    }

    private String createAnalysisPrompt(String contractText) {
        return "Analyze this contract text and provide a structured JSON response with the following: " +
               "1. riskLevel (0-100 integer representing overall risk) " +
               "2. risks (array of objects with type, severity [High, Medium, Low], and description) " +
               "3. compliance (object with boolean fields: gdpr, hipaa, pci). " +
               "Contract text: " + contractText;
    }

    private String callGeminiApi(String prompt) {
        try {
            String url = GEMINI_URL + apiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            
            parts.put("text", prompt);
            List<Map<String, Object>> partsList = new ArrayList<>();
            partsList.add(parts);
            
            contents.put("parts", partsList);
            List<Map<String, Object>> contentsList = new ArrayList<>();
            contentsList.add(contents);
            
            requestBody.put("contents", contentsList);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String response = restTemplate.postForObject(url, entity, String.class);
            
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty response received from Gemini API");
            }
            
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private Contract parseGeminiResponse(String response, String originalText) {
        try {
            // Parse JSON from Gemini response
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            
            // Safely navigate the JSON structure with type checking
            String content = "";
            
            // Check if we have a candidates field and it's an array
            if (jsonResponse.has("candidates")) {
                JsonElement candidatesElement = jsonResponse.get("candidates");
                if (candidatesElement.isJsonArray()) {
                    JsonArray candidatesArray = candidatesElement.getAsJsonArray();
                    if (candidatesArray.size() > 0) {
                        JsonElement candidateElement = candidatesArray.get(0);
                        if (candidateElement.isJsonObject()) {
                            JsonObject candidate = candidateElement.getAsJsonObject();
                            
                            // Check if content exists and is an array
                            if (candidate.has("content")) {
                                JsonElement contentElement = candidate.get("content");
                                if (contentElement.isJsonObject()) {
                                    JsonObject contentObj = contentElement.getAsJsonObject();
                                    
                                    // Check if parts exists and is an array
                                    if (contentObj.has("parts")) {
                                        JsonElement partsElement = contentObj.get("parts");
                                        if (partsElement.isJsonArray()) {
                                            JsonArray partsArray = partsElement.getAsJsonArray();
                                            if (partsArray.size() > 0) {
                                                JsonElement partElement = partsArray.get(0);
                                                if (partElement.isJsonObject() && 
                                                    partElement.getAsJsonObject().has("text")) {
                                                    content = partElement.getAsJsonObject().get("text").getAsString();
                                                }
                                            }
                                        } else if (partsElement.isJsonObject() && 
                                                   partsElement.getAsJsonObject().has("text")) {
                                            // Handle case where parts is an object, not an array
                                            content = partsElement.getAsJsonObject().get("text").getAsString();
                                        }
                                    }
                                } else if (contentElement.isJsonArray()) {
                                    // Handle case where content is an array
                                    JsonArray contentArray = contentElement.getAsJsonArray();
                                    if (contentArray.size() > 0) {
                                        JsonElement contentItem = contentArray.get(0);
                                        if (contentItem.isJsonObject() && contentItem.getAsJsonObject().has("parts")) {
                                            JsonElement partsElement = contentItem.getAsJsonObject().get("parts");
                                            if (partsElement.isJsonArray()) {
                                                JsonArray partsArray = partsElement.getAsJsonArray();
                                                if (partsArray.size() > 0 && 
                                                    partsArray.get(0).isJsonObject() && 
                                                    partsArray.get(0).getAsJsonObject().has("text")) {
                                                    content = partsArray.get(0).getAsJsonObject().get("text").getAsString();
                                                }
                                            } else if (partsElement.isJsonObject() && 
                                                      partsElement.getAsJsonObject().has("text")) {
                                                content = partsElement.getAsJsonObject().get("text").getAsString();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (content.isEmpty()) {
                throw new RuntimeException("Could not extract text content from Gemini response");
            }
            
            // Use regex to find JSON object in the response
            Pattern pattern = Pattern.compile("\\{[^\\{\\}]*(\\{[^\\{\\}]*\\})*[^\\{\\}]*\\}");
            Matcher matcher = pattern.matcher(content);
            
            if (!matcher.find()) {
                throw new RuntimeException("No JSON object found in Gemini response text");
            }
            
            String jsonStr = matcher.group(0);
            JsonObject analysisJson = gson.fromJson(jsonStr, JsonObject.class);
            
            // Create Contract object
            Contract contract = new Contract();
            contract.setText(originalText);
            
            // Get risk level with null check
            if (analysisJson.has("riskLevel") && !analysisJson.get("riskLevel").isJsonNull()) {
                contract.setRiskLevel(analysisJson.get("riskLevel").getAsInt());
            } else {
                contract.setRiskLevel(50); // Default value
            }
            
            // Parse risks with null checks
            List<Risk> risks = new ArrayList<>();
            if (analysisJson.has("risks") && !analysisJson.get("risks").isJsonNull()) {
                JsonElement risksElement = analysisJson.get("risks");
                if (risksElement.isJsonArray()) {
                    JsonArray risksArray = risksElement.getAsJsonArray();
                    for (JsonElement riskElement : risksArray) {
                        if (riskElement.isJsonObject()) {
                            JsonObject riskJson = riskElement.getAsJsonObject();
                            Risk risk = new Risk();
                            if (riskJson.has("type")) {
                                risk.setType(riskJson.get("type").getAsString());
                            } else {
                                risk.setType("Unknown");
                            }
                            if (riskJson.has("severity")) {
                                risk.setSeverity(riskJson.get("severity").getAsString());
                            } else {
                                risk.setSeverity("Medium");
                            }
                            if (riskJson.has("description")) {
                                risk.setDescription(riskJson.get("description").getAsString());
                            } else {
                                risk.setDescription("No description provided");
                            }
                            risks.add(risk);
                        }
                    }
                }
            }
            contract.setRisks(risks);
            
            // Parse compliance with null checks
            Compliance compliance = new Compliance();
            if (analysisJson.has("compliance") && !analysisJson.get("compliance").isJsonNull()) {
                JsonElement complianceElement = analysisJson.get("compliance");
                if (complianceElement.isJsonObject()) {
                    JsonObject complianceJson = complianceElement.getAsJsonObject();
                    compliance.setGdpr(complianceJson.has("gdpr") && !complianceJson.get("gdpr").isJsonNull() ? 
                                      complianceJson.get("gdpr").getAsBoolean() : false);
                    compliance.setHipaa(complianceJson.has("hipaa") && !complianceJson.get("hipaa").isJsonNull() ? 
                                      complianceJson.get("hipaa").getAsBoolean() : false);
                    compliance.setPci(complianceJson.has("pci") && !complianceJson.get("pci").isJsonNull() ? 
                                     complianceJson.get("pci").getAsBoolean() : false);
                }
            }
            contract.setCompliance(compliance);
            
            return contract;
        } catch (Exception e) {
            // Enhanced error logging
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback for parsing errors
            Contract contract = new Contract();
            contract.setText(originalText);
            contract.setRiskLevel(50); // Default medium risk
            
            List<Risk> risks = new ArrayList<>();
            Risk risk = new Risk();
            risk.setType("Parsing Error");
            risk.setSeverity("Medium");
            risk.setDescription("Unable to parse AI response: " + e.getMessage());
            risks.add(risk);
            contract.setRisks(risks);
            
            Compliance compliance = new Compliance();
            compliance.setGdpr(false);
            compliance.setHipaa(false);
            compliance.setPci(false);
            contract.setCompliance(compliance);
            
            return contract;
        }
    }
}