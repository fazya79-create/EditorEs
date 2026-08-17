package com.editor.es.agent

data class AgentSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val binary: String,
    val installScript: String
)

object AgentCatalog {

    private const val EnsureNode = """
if ! command -v node >/dev/null 2>&1; then
  echo "==> installing Node.js runtime"
  apt-get update -y
  apt-get install -y curl ca-certificates gnupg
  curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
  apt-get install -y nodejs
fi
echo "==> node $(node --version), npm $(npm --version)"
"""

    private const val EnsurePipx = """
if ! command -v pipx >/dev/null 2>&1; then
  echo "==> installing pipx"
  apt-get update -y
  apt-get install -y python3 python3-pip pipx
  pipx ensurepath || true
fi
"""

    private fun npmAgent(
        id: String,
        name: String,
        subtitle: String,
        binary: String,
        pkg: String
    ) = AgentSpec(
        id = id,
        name = name,
        subtitle = subtitle,
        binary = binary,
        installScript = """
set -e
$EnsureNode
echo "==> npm install -g $pkg"
npm install -g $pkg
echo "==> installed: ${'$'}(command -v $binary || echo NOT_FOUND)"
"""
    )

    val agents: List<AgentSpec> = listOf(
        npmAgent(
            id = "claude-code",
            name = "Claude Code",
            subtitle = "Anthropic agentic CLI · npm @anthropic-ai/claude-code",
            binary = "claude",
            pkg = "@anthropic-ai/claude-code"
        ),
        npmAgent(
            id = "opencode",
            name = "OpenCode",
            subtitle = "Open source terminal agent · npm opencode-ai",
            binary = "opencode",
            pkg = "opencode-ai"
        ),
        npmAgent(
            id = "codex",
            name = "OpenAI Codex",
            subtitle = "OpenAI coding agent · npm @openai/codex",
            binary = "codex",
            pkg = "@openai/codex"
        ),
        npmAgent(
            id = "gemini-cli",
            name = "Gemini CLI",
            subtitle = "Google Gemini agent · npm @google/gemini-cli",
            binary = "gemini",
            pkg = "@google/gemini-cli"
        ),
        npmAgent(
            id = "qwen-code",
            name = "Qwen Code",
            subtitle = "Qwen3-Coder agent · npm @qwen-code/qwen-code",
            binary = "qwen",
            pkg = "@qwen-code/qwen-code"
        ),
        npmAgent(
            id = "copilot-cli",
            name = "GitHub Copilot CLI",
            subtitle = "GitHub Copilot agent · npm @github/copilot",
            binary = "copilot",
            pkg = "@github/copilot"
        ),
        AgentSpec(
            id = "aider",
            name = "Aider",
            subtitle = "Pair programming in your terminal · pipx aider-chat",
            binary = "aider",
            installScript = """
set -e
$EnsurePipx
echo "==> pipx install aider-chat"
pipx install aider-chat --force
echo "==> installed: ${'$'}(command -v aider || echo NOT_FOUND)"
"""
        )
    )

    fun byId(id: String): AgentSpec? = agents.firstOrNull { it.id == id }
}
