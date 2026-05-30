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
import org.hibernate.annotations.Type;

@Entity
@Table(name = "honeypot_hits")
public class HoneypotHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Type(PostgreSQLInetType.class)
    @Column(name = "source_ip", nullable = false, columnDefinition = "inet")
    private Inet sourceIp;

    @Column(name = "observed_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime observedAt;

    @Column(name = "request_path", nullable = false, columnDefinition = "text")
    private String requestPath;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "method", nullable = false, length = 16, columnDefinition = "varchar(16)")
    private String method;

    public HoneypotHit() {
    }

    public Long getId() {
        return id;
    }

    public String getSourceIp() {
        return sourceIp == null ? null : stripCidrSuffix(sourceIp.getAddress());
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp == null ? null : new Inet(sourceIp);
    }

    public OffsetDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(OffsetDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof HoneypotHit that)) {
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
        return "HoneypotHit(id=" + id + ")";
    }

    private String stripCidrSuffix(String value) {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }
}
