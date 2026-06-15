# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install --no-audit --no-fund

FROM node:20-alpine AS runtime
ENV NODE_ENV=development \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000

RUN addgroup --system --gid 1001 app \
    && adduser --system --uid 1001 --ingroup app app \
    && mkdir -p /app \
    && chown app:app /app
WORKDIR /app

COPY --from=deps --chown=app:app /app/node_modules ./node_modules
COPY --chown=app:app . .

USER app
EXPOSE 3000

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=10 \
    CMD wget --spider -q http://127.0.0.1:3000 || exit 1

CMD ["npx", "next", "dev", "-H", "0.0.0.0", "-p", "3000"]
