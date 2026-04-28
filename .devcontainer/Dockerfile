FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

# The base devcontainer image currently includes a Yarn Debian apt source
# whose signing key is missing/stale on fresh rebuilds.
# We use Corepack for Yarn/pnpm, so the Yarn apt repo is not needed.
RUN set -eux; \
    if grep -R "dl.yarnpkg.com" /etc/apt/sources.list /etc/apt/sources.list.d 2>/dev/null; then \
        grep -RIl "dl.yarnpkg.com" /etc/apt/sources.list /etc/apt/sources.list.d \
          | xargs -r sed -i '/dl.yarnpkg.com/d'; \
    fi; \
    rm -f /etc/apt/sources.list.d/yarn.list \
          /etc/apt/sources.list.d/yarnpkg.list \
          /usr/share/keyrings/yarnkey.gpg \
          /usr/share/keyrings/yarn-archive-keyring.gpg || true