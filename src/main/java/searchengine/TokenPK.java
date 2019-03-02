/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author soham
 */
@Embeddable
public class TokenPK implements Serializable {

    private static final long serialVersionUID = 7488214772071462430L;

    @Basic(optional = false)
    @Column(name = "token_name")
    private String tokenName;
    @Basic(optional = false)
    @Column(name = "document_ID")
    private String documentID;

    public TokenPK() {
    }

    public TokenPK(String tokenName, String documentID) {
        this.tokenName = tokenName;
        this.documentID = documentID;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getDocumentID() {
        return documentID;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (tokenName != null ? tokenName.hashCode() : 0);
        hash += (documentID != null ? documentID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TokenPK)) {
            return false;
        }
        TokenPK other = (TokenPK) object;
        if ((this.tokenName == null && other.tokenName != null) || (this.tokenName != null && !this.tokenName.equals(other.tokenName))) {
            return false;
        }
        if ((this.documentID == null && other.documentID != null) || (this.documentID != null && !this.documentID.equals(other.documentID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "searchengine.TokenPK[ tokenName=" + tokenName + ", documentID=" + documentID + " ]";
    }
    
}
