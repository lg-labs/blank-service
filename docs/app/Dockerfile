FROM python:3.11-alpine3.19

# Build-time flags
ARG WITH_PLUGINS=true

# Environment variables
ENV PACKAGES=/usr/local/lib/python3.11/site-packages
ENV PYTHONDONTWRITEBYTECODE=1

# Set build directory
WORKDIR /tmp

COPY *requirements.txt ./

# Perform build and cleanup artifacts and caches
RUN \
  apk upgrade --update-cache -a \
    && \
      apk add --no-cache \
        cairo \
        freetype-dev \
        git \
        git-fast-import \
        jpeg-dev \
        openssh \
        tini \
        zlib-dev \
  && \
    apk add --no-cache --virtual .build \
      gcc \
      libffi-dev \
      musl-dev \
  && \
    pip install --no-cache-dir --upgrade pip \
  && \
    pip install --no-cache-dir . \
  && \
    if [ -e user-requirements.txt ]; then \
      pip install -U -r user-requirements.txt; \
    fi \
  && \

# Set working directory
WORKDIR /docs

# Expose MkDocs development server port
EXPOSE 8000

# Start development server by default
ENTRYPOINT ["/sbin/tini", "--", "mkdocs"]
CMD ["serve", "--dev-addr=0.0.0.0:8000"]