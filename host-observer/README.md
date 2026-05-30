# Watchtower Host Observer

The host observer is an optional Linux-only service for collecting host socket metadata while keeping the main Watchtower backend on normal Docker bridge networking.

It does not capture packets, block traffic, change firewall rules, kill processes, or take any host action. It runs `ss -tunap`, normalizes the observed socket rows, and posts them to the backend internal ingestion endpoint.

## Namespace permissions

The observer enters the host network namespace with:

- `pid: host`
- read-only `/proc` mount at `/host/proc`
- `CAP_SYS_ADMIN`
- `seccomp:unconfined`
- `apparmor:unconfined`
- `no-new-privileges:true`
- read-only container filesystem
- all other Linux capabilities dropped

`CAP_SYS_ADMIN` is required for `nsenter --net=/host/proc/1/ns/net`. On Docker hosts with default seccomp/AppArmor confinement, `nsenter` can still fail with `Permission denied` unless the observer is allowed to call `setns` against the host namespace. The seccomp and AppArmor relaxations are scoped only to this optional observer container.

## Security tradeoff

This grants the observer enough privilege to enter the host network namespace and view host socket tables. That is broader than a normal application container and should be enabled only when host-level socket visibility is required.

The service remains advisory-only:

- no Docker socket mount
- no host write mounts
- no packet capture tooling
- no `NET_ADMIN`
- no `NET_RAW`
- no firewall or routing changes
- no blocking or attack-back behavior

Enable it only with the `host-observer` Compose profile and a strong `WATCHTOWER_OBSERVER_TOKEN`.
