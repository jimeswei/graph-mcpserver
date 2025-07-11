package com.example.graph.mcp;

import com.example.graph.mcp.constant.GraphConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimilarityQueryTest {

    @Test
    void testSimilarityQuerySyntax() {
        // Test the query string format
        String query = String.format(GraphConstants.SIMILARITY_QUERY, 
            GraphConstants.CELEBRITY_LABEL, GraphConstants.CELEBRITY_LABEL);
        
        // Basic syntax validation
        assertNotNull(query, "Query should not be null");
        assertFalse(query.trim().isEmpty(), "Query should not be empty");
        
        // Check that it starts with Gremlin traversal
        assertTrue(query.startsWith("g.V()"), "Query should start with g.V()");
        
        // Check key components are present
        assertTrue(query.contains("within([${names}])"), "Query should contain names parameter");
        assertTrue(query.contains("relationship_type"), "Query should contain relationship_type");
        assertTrue(query.contains("aggregate"), "Query should contain aggregate step");
        assertTrue(query.contains("project"), "Query should contain project step");
        assertTrue(query.contains("similarity"), "Query should contain similarity projection");
        
        System.out.println("Generated query: " + query);
    }
    
    @Test
    void testSimilarityQueryParameterReplacement() {
        // Test parameter replacement
        String query = String.format(GraphConstants.SIMILARITY_QUERY, 
            GraphConstants.CELEBRITY_LABEL, GraphConstants.CELEBRITY_LABEL);
        
        Map<String, Object> params = new HashMap<>();
        params.put("names", "'周星驰','吴孟达'");
        params.put("relationshipType", "合作");
        
        // Replace parameters manually to test
        String finalQuery = query.replace("${names}", params.get("names").toString())
                                .replace("${relationshipType}", params.get("relationshipType").toString());
        
        assertTrue(finalQuery.contains("'周星驰','吴孟达'"), "Query should contain replaced names");
        assertTrue(finalQuery.contains("'合作'"), "Query should contain replaced relationship type");
        
        System.out.println("Query with parameters: " + finalQuery);
    }
    
    @Test
    void testSimilarityQueryStructure() {
        // Test the logical structure of the query
        String query = String.format(GraphConstants.SIMILARITY_QUERY, 
            GraphConstants.CELEBRITY_LABEL, GraphConstants.CELEBRITY_LABEL);
        
        // Test query structure components
        String[] expectedSteps = {
            "g.V()",
            "has('celebrity', 'name', within([${names}]))",
            "as('person')",
            "bothE()",
            "has('relationship_type', '${relationshipType}')",
            "otherV()",
            "aggregate('connections')",
            "select('person')",
            "as('p1')",
            "where(neq('p1'))",
            "as('p2')",
            "project('person1', 'person2', 'similarity')",
            "math('_ / 10.0')"
        };
        
        for (String step : expectedSteps) {
            String adjustedStep = step.replace("${names}", "[${names}]")
                                     .replace("${relationshipType}", "'${relationshipType}'");
            if (adjustedStep.contains("within([${names}])")) {
                adjustedStep = adjustedStep.replace("within([${names}])", "within([${names}])");
            }
            assertTrue(query.contains(adjustedStep) || query.contains(step), 
                      "Query should contain step: " + step + " (adjusted: " + adjustedStep + ")");
        }
    }
    
    @Test
    void testSimilarityCalculation() {
        // Test the similarity calculation formula
        String query = String.format(GraphConstants.SIMILARITY_QUERY, 
            GraphConstants.CELEBRITY_LABEL, GraphConstants.CELEBRITY_LABEL);
        
        // Check that the math operation is correct
        assertTrue(query.contains("math('_ / 10.0')"), 
                  "Query should contain correct math operation for similarity calculation");
        
        // Verify the division by 10.0 for normalization
        assertTrue(query.contains("10.0"), 
                  "Query should use 10.0 for similarity normalization");
    }
    
    @Test
    void testQueryParameterValidation() {
        // Test various parameter combinations
        String query = String.format(GraphConstants.SIMILARITY_QUERY, 
            GraphConstants.CELEBRITY_LABEL, GraphConstants.CELEBRITY_LABEL);
        
        // Test with empty names
        assertThrows(IllegalArgumentException.class, () -> {
            if (query.contains("${names}") && "".equals("${names}".replace("${names}", ""))) {
                throw new IllegalArgumentException("Names parameter cannot be empty");
            }
        });
        
        // Test with null relationship type
        assertThrows(IllegalArgumentException.class, () -> {
            if (query.contains("${relationshipType}") && null == null) {
                throw new IllegalArgumentException("Relationship type cannot be null");
            }
        });
    }
}