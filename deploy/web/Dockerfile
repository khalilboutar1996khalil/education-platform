# Public entry point of the platform: Caddy serving the compiled Angular app and proxying the API.
# Built by compose.prod.yaml, which passes the Angular project as the "frontend" build context.

# ---------- build the Angular app ----------
FROM node:22-alpine AS frontend
WORKDIR /app

# Dependencies first, so they stay cached while only the source changes
COPY --from=frontend package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY --from=frontend . .
RUN rm -rf node_modules/.cache dist && npx ng build --configuration production

# ---------- serve it ----------
FROM caddy:2-alpine
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=frontend /app/dist/education-platform-front/browser /srv
