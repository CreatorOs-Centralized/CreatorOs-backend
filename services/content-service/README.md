# Content Service

Content drafts, workflow states, and version history for CreatorOS.

## Run (local)

1. Ensure Postgres is running and `CONTENT_DB_URL`, `CONTENT_DB_USERNAME`, `CONTENT_DB_PASSWORD` are set.
2. From this folder:
   - `./gradlew.bat bootRun`

## API

- `POST /contents`
- `GET /contents`
- `PUT /contents/{contentId}`
- `DELETE /contents/{contentId}`
- `POST /contents/{contentId}/versions`
- `GET /contents/{contentId}/versions`

Swagger UI: `/swagger-ui.html`