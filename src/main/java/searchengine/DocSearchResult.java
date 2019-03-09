/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.util.Objects;

/**
 * Output DTO used in final Search Results to be displayed
 *
 * @author soham
 */
public class DocSearchResult {

    /**
     * Document ID
     */
    private String docID;
    /**
     * Calculated TF-IDF value for the Document
     */
    private double tfIdf;
    /**
     * Calculate weight for the Document
     */
    private double weight;

    /**
     * Constructor
     *
     * @param docID
     * @param tfIDF
     * @param weight
     */
    public DocSearchResult(String docID, double tfIDF, double weight) {
        this.docID = docID;
        this.tfIdf = tfIDF;
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getDocID() {
        return docID;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public double getTfIdf() {
        return tfIdf;
    }

    public void setTfIdf(double tfIdf) {
        this.tfIdf = tfIdf;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.docID);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DocSearchResult other = (DocSearchResult) obj;
        return Objects.equals(this.docID, other.docID);
    }

}
