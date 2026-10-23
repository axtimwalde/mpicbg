FROM node:24-trixie

ARG TZ
ENV TZ="$TZ"

# Install basic development tools and iptables/ipset
RUN apt-get update && apt-get install -y --no-install-recommends \
  aggregate \
  dnsutils \
  fzf \
  gh \
  git \
  gnupg2 \
  iproute2 \
  ipset \
  iptables \
  jq \
  less \
  libx11-6 \
  libxext6 \
  libxi6 \
  libxrender1 \
  libxtst6 \
  man-db \
  nano \
  procps \
  python3 \
  python3-dev \
  python3-pip \
  python3-venv \
  sudo \
  unzip \
  vim \
  x11-xserver-utils \
  xauth \
  xdg-utils \
  zsh \
  default-jdk \
  maven \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Ensure default node user has access to /usr/local/share
RUN mkdir -p /usr/local/share/npm-global && \
  chown -R node:node /usr/local/share

ARG USERNAME=node

# Persist bash history.
RUN SNIPPET="export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  && mkdir /commandhistory \
  && touch /commandhistory/.bash_history \
  && chown -R $USERNAME /commandhistory

# Set `DEVCONTAINER` environment variable to help with orientation
ENV DEVCONTAINER=true

# Create workspace and config directories and set permissions
RUN mkdir -p /workspace /home/node/.claude && \
  chown -R node:node /workspace /home/node/.claude

WORKDIR /workspace

ARG GIT_DELTA_VERSION=0.19.2
RUN ARCH=$(dpkg --print-architecture) && \
  wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  sudo dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Python
RUN ln -s /usr/bin/python3 /usr/bin/python

# Install Python packages from requirements.txt and local MCP wheels.
# --ignore-installed: Debian-packaged Python dists (pulled in by apt packages
# like python3-setuptools) ship without RECORD files, so pip cannot uninstall
# them when the resolved set needs a newer version; skip the uninstall and let
# the fresh copy in /usr/local shadow /usr/lib/python3/dist-packages.
COPY requirements.txt /tmp/requirements.txt
RUN pip3 install --no-cache-dir --break-system-packages --ignore-installed -r /tmp/requirements.txt


# Set up non-root user
USER node

# Install global packages
ENV NPM_CONFIG_PREFIX=/usr/local/share/npm-global
ENV PATH=$PATH:/usr/local/share/npm-global/bin

# Set the default shell to zsh rather than sh
ENV SHELL=/bin/zsh

# Set the default editor and visual
ENV EDITOR nano
ENV VISUAL nano

# Claude Code privacy
ENV CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC 1
ENV CLAUDE_CODE_DISABLE_ANALYTICS 1

# Default powerline10k theme
ARG ZSH_IN_DOCKER_VERSION=1.2.1
RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v${ZSH_IN_DOCKER_VERSION}/zsh-in-docker.sh)" -- \
  -p git \
  -p fzf \
  -a "source /usr/share/doc/fzf/examples/key-bindings.zsh" \
  -a "source /usr/share/doc/fzf/examples/completion.zsh" \
  -a "export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  -a "zstyle ':completion:*:options' matcher-list ''" \
  -x

# Install pixi package manager
RUN curl -fsSL https://pixi.sh/install.sh | sh && \
    echo 'export PATH="$HOME/.pixi/bin:$PATH"' >> ~/.bashrc && \
    echo 'export PATH="$HOME/.pixi/bin:$PATH"' >> ~/.zshrc

RUN /home/node/.pixi/bin/pixi global install ijava

# Install Claude Code
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/home/node/.local/bin:${PATH}"


# Copy and set up firewall script
COPY init-firewall.sh /usr/local/bin/
USER root
RUN chmod +x /usr/local/bin/init-firewall.sh && \
  echo "node ALL=(root) NOPASSWD: /usr/local/bin/init-firewall.sh" > /etc/sudoers.d/node-firewall && \
  chmod 0440 /etc/sudoers.d/node-firewall
USER node
