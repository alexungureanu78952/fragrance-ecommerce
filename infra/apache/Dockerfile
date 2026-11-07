# --- Build stage: produce the demo frontend bundle ---
FROM node:22-alpine AS build
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# Vite reads env files from the repository root (envDir: '..').
COPY .env.demo /src/.env.demo
ARG VITE_CAPTCHA_SITE_KEY=""
ENV VITE_CAPTCHA_SITE_KEY=${VITE_CAPTCHA_SITE_KEY}
RUN npm run build:demo

# --- Runtime stage: Apache with .htaccess support and an /api reverse proxy ---
FROM httpd:2.4-alpine
RUN sed -i \
      -e 's/^#\(LoadModule headers_module\)/\1/' \
      -e 's/^#\(LoadModule proxy_module\)/\1/' \
      -e 's/^#\(LoadModule proxy_http_module\)/\1/' \
      conf/httpd.conf \
 && echo 'Include conf/extra/fragrance.conf' >> conf/httpd.conf
COPY infra/apache/fragrance.conf conf/extra/fragrance.conf
COPY --from=build /src/frontend/dist/ htdocs/
