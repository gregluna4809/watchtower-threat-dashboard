package com.gluna.watchtower.model;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "connections")
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id")
    private ProcessEntity process;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id")
    private RemoteEndpoint endpoint;

    @Type(PostgreSQLInetType.class)
    @Column(name = "local_ip", nullable = false, columnDefinition = "inet")
    private Inet localIp;

    @Column(name = "local_port", nullable = false)
    private Integer localPort;

    @Column(name = "remote_port")
    private Integer remotePort;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 8, columnDefinition = "varchar(8)")
    private Protocol protocol;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 16, columnDefinition = "varchar(16)")
    private ConnectionState state;

    @Column(name = "first_seen", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime firstSeen;

    @Column(name = "last_seen", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime lastSeen;

    @Column(name = "observation_count", nullable = false)
    private Integer observationCount;

    public Connection() {
    }

    public Long getId() {
        return id;
    }

    public ProcessEntity getProcess() {
        return process;
    }

    public void setProcess(ProcessEntity process) {
        this.process = process;
    }

    public RemoteEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(RemoteEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getLocalIp() {
        return localIp == null ? null : stripCidrSuffix(localIp.getAddress());
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp == null ? null : new Inet(localIp);
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public ConnectionState getState() {
        return state;
    }

    public void setState(ConnectionState state) {
        this.state = state;
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

    public Integer getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(Integer observationCount) {
        this.observationCount = observationCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Connection that)) {
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
        return "Connection(id=" + id + ")";
    }

    private String stripCidrSuffix(String value) {
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }
}
