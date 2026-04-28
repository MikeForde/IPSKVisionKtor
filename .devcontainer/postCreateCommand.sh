#!/usr/bin/env bash
set -e

# Load SDKMAN! into this shell’s environment
source "/usr/local/sdkman/bin/sdkman-init.sh"  

# Install Kotlin via SDKMAN!
sdk install kotlin

# Enable Corepack shims for Yarn/pnpm
corepack enable

# Install OpenCode into the vscode user's home directory
if [ ! -x "$HOME/.opencode/bin/opencode" ]; then
  curl -fsSL https://opencode.ai/install | bash
fi

# Verify OpenCode
"$HOME/.opencode/bin/opencode" --version || true

# Correct kotlin permissions
 sudo chown -R vscode:vscode /workspaces/IPSKVisionKtor

# Load kotlin into this shell’s environment
#source "/usr/local/sdkman/candidates/kotlin/current/bin/kotlin"