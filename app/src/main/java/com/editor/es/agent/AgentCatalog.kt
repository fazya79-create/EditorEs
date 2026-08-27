package com.editor.es.agent

import androidx.annotation.DrawableRes
import com.editor.es.R

data class AgentSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val binary: String,
    val docUrl: String,
    @DrawableRes val iconRes: Int,
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

    private fun npmAgent(
        id: String,
        name: String,
        subtitle: String,
        binary: String,
        docUrl: String,
        @DrawableRes iconRes: Int,
        pkg: String,
        postInstall: String = ""
    ) = AgentSpec(
        id = id,
        name = name,
        subtitle = subtitle,
        binary = binary,
        docUrl = docUrl,
        iconRes = iconRes,
        installScript = """
set -e
$EnsureNode
echo "==> npm install -g $pkg"
npm install -g $pkg
$postInstall
echo "==> installed: $(command -v $binary || echo NOT_FOUND)"
"""
    )

    private const val DshMobilePatch = """
echo "==> applying mobile responsive layout patch to deepseek-harness web UI"
node -e "
const fs = require('fs');
const globPaths = [
  '/usr/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-layout/lib/client.js',
  '/usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-layout/lib/client.js'
];
for (const p of globPaths) {
  if (fs.existsSync(p)) {
    let code = fs.readFileSync(p, 'utf8');
    const mobileCss = '@media(max-width:768px){.pI_x6G_frame{display:block!important;position:relative!important;width:100vw!important;height:100%!important}.pI_x6G_handle{display:none!important}.pI_x6G_centerCol{width:100%!important;height:100%!important}.pI_x6G_sidebarCol{position:fixed!important;top:0!important;left:0!important;bottom:0!important;width:280px!important;max-width:85vw!important;z-index:50!important;box-shadow:4px 0 24px rgba(0,0,0,0.45);transition:transform var(--ds-transition-duration-slow) var(--ds-ease-in-out);transform:translateX(0)}.pI_x6G_frame[data-sidebar-collapsed] .pI_x6G_sidebarCol{transform:translateX(-100%)!important;pointer-events:none!important;border-right:none!important}.pI_x6G_detailsCol{position:fixed!important;top:0!important;right:0!important;bottom:0!important;width:100vw!important;max-width:420px!important;z-index:45!important;background:var(--dsw-alias-bg-base);box-shadow:-4px 0 24px rgba(0,0,0,0.45);transition:transform var(--ds-transition-duration-slow) var(--ds-ease-in-out);transform:translateX(0)}.pI_x6G_frame[data-details-collapsed] .pI_x6G_detailsCol{transform:translateX(100%)!important;pointer-events:none!important;border-left:none!important}}';
    if (!code.includes('@media(max-width:768px)')) {
      code = code.replace('.pI_x6G_frame{', mobileCss + '.pI_x6G_frame{');
      fs.writeFileSync(p, code);
      console.log('==> successfully patched:', p);
    }
  }
}
" || true
"""

    val agents: List<AgentSpec> = listOf(
        npmAgent(
            id = "deepseek-harness",
            name = "DeepSeek Harness",
            subtitle = "DeepSeek agentic harness · npm @deepseek-ai/dsh",
            binary = "dsh",
            docUrl = "https://github.com/deepseek-ai/deepseek-harness",
            iconRes = R.drawable.ic_agent_deepseek,
            pkg = "@deepseek-ai/dsh",
            postInstall = DshMobilePatch
        ),
        npmAgent(
            id = "claude-code",
            name = "Claude Code",
            subtitle = "Anthropic agentic CLI · npm @anthropic-ai/claude-code",
            binary = "claude",
            docUrl = "https://docs.anthropic.com/en/docs/agents-and-tools/claude-code/overview",
            iconRes = R.drawable.ic_agent_claude,
            pkg = "@anthropic-ai/claude-code"
        ),
        npmAgent(
            id = "opencode",
            name = "OpenCode",
            subtitle = "Open source terminal agent · npm opencode-ai",
            binary = "opencode",
            docUrl = "https://github.com/opencode-ai/opencode",
            iconRes = R.drawable.ic_agent_opencode,
            pkg = "opencode-ai"
        ),
        npmAgent(
            id = "codex",
            name = "OpenAI Codex",
            subtitle = "OpenAI coding agent · npm @openai/codex",
            binary = "codex",
            docUrl = "https://github.com/openai/codex",
            iconRes = R.drawable.ic_agent_codex,
            pkg = "@openai/codex"
        ),
        npmAgent(
            id = "qodercli",
            name = "Qoder CLI",
            subtitle = "Alibaba Qoder agent · npm @qoder-ai/qodercli",
            binary = "qodercli",
            docUrl = "https://qoder.alibabacloud.com",
            iconRes = R.drawable.ic_agent_qoder,
            pkg = "@qoder-ai/qodercli"
        ),
        npmAgent(
            id = "copilot-cli",
            name = "GitHub Copilot CLI",
            subtitle = "GitHub Copilot agent · npm @github/copilot",
            binary = "copilot",
            docUrl = "https://docs.github.com/en/copilot",
            iconRes = R.drawable.ic_agent_copilot,
            pkg = "@github/copilot"
        )
    )

    fun byId(id: String): AgentSpec? = agents.firstOrNull { it.id == id }
}