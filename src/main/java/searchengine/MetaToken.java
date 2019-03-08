/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchengine;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author soham
 */
@Entity
@Table(name = "META_TOKEN")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MetaToken.findAll", query = "SELECT m FROM MetaToken m")
    , @NamedQuery(name = "MetaToken.findByTokenName", query = "SELECT m FROM MetaToken m WHERE m.tokenName = :tokenName")
    , @NamedQuery(name = "MetaToken.findByIdf", query = "SELECT m FROM MetaToken m WHERE m.idf = :idf")})
public class MetaToken implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "token_name")
    private String tokenName;
    @Basic(optional = false)
    @Column(name = "idf")
    private double idf;

    public MetaToken() {
    }

    public MetaToken(String tokenName) {
        this.tokenName = tokenName;
    }

    public MetaToken(String tokenName, double idf) {
        this.tokenName = tokenName;
        this.idf = idf;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public double getIdf() {
        return idf;
    }

    public void setIdf(double idf) {
        this.idf = idf;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (tokenName != null ? tokenName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MetaToken)) {
            return false;
        }
        MetaToken other = (MetaToken) object;
        if ((this.tokenName == null && other.tokenName != null) || (this.tokenName != null && !this.tokenName.equals(other.tokenName))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "searchengine.MetaToken[ tokenName=" + tokenName + " ]";
    }
    
}
