FROM node:22-alpine
WORKDIR /app
COPY . .
RUN node build.js
EXPOSE 3000
CMD ["node", "dist/server.js"]
