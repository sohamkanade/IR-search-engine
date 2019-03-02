/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author soham
 */
@Entity
@Table(name = "TOKEN")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Token.findAll", query = "SELECT t FROM Token t")
    , @NamedQuery(name = "Token.findByTokenName", query = "SELECT t FROM Token t WHERE t.tokenPK.tokenName = :tokenName")
    , @NamedQuery(name = "Token.findByDocumentID", query = "SELECT t FROM Token t WHERE t.tokenPK.documentID = :documentID")
    , @NamedQuery(name = "Token.findByFrequency", query = "SELECT t FROM Token t WHERE t.frequency = :frequency")})
public class Token implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected TokenPK tokenPK;
    @Basic(optional = false)
    @Column(name = "frequency")
    private int frequency;

    public Token() {
    }

    public Token(TokenPK tokenPK) {
        this.tokenPK = tokenPK;
    }

    public Token(TokenPK tokenPK, int frequency) {
        this.tokenPK = tokenPK;
        this.frequency = frequency;
    }

    public Token(String tokenName, String documentID) {
        this.tokenPK = new TokenPK(tokenName, documentID);
    }

    public TokenPK getTokenPK() {
        return tokenPK;
    }

    public void setTokenPK(TokenPK tokenPK) {
        this.tokenPK = tokenPK;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (tokenPK != null ? tokenPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Token)) {
            return false;
        }
        Token other = (Token) object;
        if ((this.tokenPK == null && other.tokenPK != null) || (this.tokenPK != null && !this.tokenPK.equals(other.tokenPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "searchengine.Token[ tokenPK=" + tokenPK + " ]";
    }
    
}
