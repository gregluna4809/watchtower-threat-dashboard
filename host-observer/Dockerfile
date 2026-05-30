FROM python:3.12-alpine

RUN apk add --no-cache iproute2 util-linux

WORKDIR /app
COPY observer.py /app/observer.py

ENV WATCHTOWER_BACKEND_URL=http://watchtower-backend:8088/internal/v1/host-snapshots
ENV WATCHTOWER_OBSERVER_INTERVAL_SECONDS=3
ENV WATCHTOWER_SS_COMMAND="nsenter --net=/host/proc/1/ns/net ss -tunap"
ENV PYTHONDONTWRITEBYTECODE=1

CMD ["python", "/app/observer.py"]
