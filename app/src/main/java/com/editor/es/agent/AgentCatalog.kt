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
echo "==> adapting deepseek-harness web UI layout & settings for mobile responsiveness"
node -e "
const fs = require('fs');

// 1. Layout: fluid main container & center column
const layoutPaths = [
  '/usr/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-layout/lib/client.js',
  '/usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-layout/lib/client.js'
];
const layoutFluidCss = '@media(max-width:768px){.pI_x6G_frame{width:100%!important;max-width:100vw!important;overflow-x:hidden!important}.pI_x6G_sidebarCol{min-width:0!important}.pI_x6G_centerCol{min-width:0!important;flex:1 1 auto!important;width:100%!important;max-width:100%!important}.pI_x6G_detailsCol{min-width:0!important}.pI_x6G_handle{display:none!important}}';

for (const p of layoutPaths) {
  if (fs.existsSync(p)) {
    let code = fs.readFileSync(p, 'utf8');
    if (!code.includes('layoutFluidMarker')) {
      code = code.replace('.pI_x6G_frame{', '/*layoutFluidMarker*/' + layoutFluidCss + '.pI_x6G_frame{');
      fs.writeFileSync(p, code);
      console.log('==> fluidly patched layout:', p);
    }
  }
}

// 2. Settings: horizontal scroll tabs and full-screen vertical content (no crushed columns)
const settingsPaths = [
  '/usr/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-settings-general/lib/client.js',
  '/usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-client-ui-settings-general/lib/client.js'
];
const mobileSettingsCss = '@media(max-width:768px){.qA42UA_panel{position:fixed!important;inset:0!important;width:100vw!important;max-width:100vw!important;height:100vh!important;max-height:100vh!important;border-radius:0!important;display:flex!important;flex-direction:column!important}.qA42UA_nav{width:100%!important;flex-direction:row!important;padding:8px 10px 4px!important;gap:8px!important;overflow-x:auto!important}.qA42UA_navTitle{display:none!important}.qA42UA_navList{flex-direction:row!important;gap:6px!important;width:100%!important}.qA42UA_navCell{flex:none!important;width:auto!important;height:34px!important;padding:6px 12px!important;white-space:nowrap!important}.qA42UA_content{width:100%!important;flex:1 1 auto!important;min-height:0!important;overflow-y:auto!important}}';

for (const p of settingsPaths) {
  if (fs.existsSync(p)) {
    let code = fs.readFileSync(p, 'utf8');
    if (!code.includes('mobileSettingsMarker')) {
      code = code.replace('.qA42UA_panel{', '/*mobileSettingsMarker*/' + mobileSettingsCss + '.qA42UA_panel{');
      fs.writeFileSync(p, code);
      console.log('==> fluidly patched settings tabs:', p);
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