# Multi-stage build: compile the Vite app, serve the static output with nginx.
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vite bakes VITE_* vars in at BUILD time, not runtime — this has to be the
# deployed backend's public URL, passed in as a build arg by whatever builds
# this image (Railway: set it as a "Build Variable" on this service).
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/nginx.conf.template

# Railway injects PORT dynamically, so nginx has to listen on whatever that
# is — substituted into the real config at container start (see CMD).
# envsubst is restricted to just $PORT so it doesn't also mangle nginx's own
# $uri-style variables in the template.
ENV PORT=80
EXPOSE 80

CMD ["/bin/sh", "-c", "envsubst '$PORT' < /etc/nginx/nginx.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
