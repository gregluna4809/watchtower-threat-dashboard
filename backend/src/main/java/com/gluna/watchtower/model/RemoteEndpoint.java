package com.gluna.watchtower.model;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "remote_endpoints")
public class RemoteEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Type(PostgreSQLInetType.class)
    @Column(name = "ip", nullable = false, unique = true, columnDefinition = "inet")
    private Inet ip;

    @Column(name = "asn")
    private Integer asn;

    @Column(name = "asn_org", columnDefinition = "text")
    private String asnOrg;

    @Column(name = "country_iso", length = 2, columnDefinition = "char(2)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String countryIso;

    @Column(name = "country_name", columnDefinition = "text")
    private String countryName;

    @Column(name = "city", columnDefinition = "text")
    private String city;

    @Column(name = "reverse_dns", columnDefinition = "text")
    private String reverseDns;

    @Column(name = "first_seen", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime firstSeen;

    @Column(name = "last_seen", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime lastSeen;

    public RemoteEndpoint() {
    }

    public Long getId() {
        return id;
    }

    public String getIp() {
        return ip == null ? null : stripCidrSuffix(ip.getAddress());
    }

    public void setIp(String ip) {
        this.ip = ip == null ? null : new Inet(ip);
    }

    public Integer getAsn() {
        return asn;
    }

    public void setAsn(Integer asn) {
        this.asn = asn;
    }

    public String getAsnOrg() {
        return asnOrg;
    }

    public void setAsnOrg(String asnOrg) {
        this.asnOrg = asnOrg;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public void setCountryIso(String countryIso) {
        this.countryIso = countryIso;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getReverseDns() {
        return reverseDns;
    }

    public void setReverseDns(String reverseDns) {
        this.reverseDns = reverseDns;
    }

    public OffsetDateTime getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(OffsetDateTime firstSeen) {
        this.firstSeen = firstSeen;
    }

    public OffsetDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(OffsetDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RemoteEndpoint that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "RemoteEndpoint(id=" + id + ")";
    }

    private String stripCidrSuffix(String value) {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }
}
