package com.gluna.watchtower.model;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "intel_cache")
public class IntelCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Type(PostgreSQLInetType.class)
    @Column(name = "ip", nullable = false, columnDefinition = "inet")
    private Inet ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32, columnDefinition = "varchar(32)")
    private IntelSource source;

    @Type(JsonType.class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "fetched_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime fetchedAt;

    @Column(name = "ttl_seconds", nullable = false)
    private Integer ttlSeconds;

    public IntelCache() {
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

    public IntelSource getSource() {
        return source;
    }

    public void setSource(IntelSource source) {
        this.source = source;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Integer getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntelCache that)) {
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
        return "IntelCache(id=" + id + ")";
    }

    private String stripCidrSuffix(String value) {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }
}
