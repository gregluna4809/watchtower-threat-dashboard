set shell := ["powershell.exe", "-NoLogo", "-Command"]

dev:
    docker compose up -d
    Write-Host "Postgres is running. Start the app with 'just backend' and 'just frontend' in separate terminals."

backend:
    cd backend; mvn spring-boot:run

frontend:
    cd frontend; npm run dev

test:
    cd backend; mvn test
    cd frontend; npm test -- --passWithNoTests

build:
    cd backend; mvn package
    cd frontend; npm run build

docker-up:
    docker compose up -d

docker-down:
    docker compose down

clean:
    if (Test-Path backend\target) { Remove-Item -Recurse -Force backend\target }
    if (Test-Path frontend\dist) { Remove-Item -Recurse -Force frontend\dist }
    if (Test-Path frontend\coverage) { Remove-Item -Recurse -Force frontend\coverage }
