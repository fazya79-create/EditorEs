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
if command -v $binary >/dev/null 2>&1; then
  echo "==> $binary is already installed, skipping npm download"
else
  echo "==> npm install -g $pkg"
  npm install -g $pkg
fi
$postInstall
echo "==> ready: $(command -v $binary || echo NOT_FOUND)"
"""
    )

    private val DshMobilePatch = """
cat << 'EOF_DSH_PATCH' > /tmp/dsh_patch.js
const fs = require('fs');
const paths = [
  '/usr/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-web-frontend/dist/index.html',
  '/usr/local/lib/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-web-frontend/dist/index.html'
];
const style = `
<style id="dsh-mobile-responsive-engine">
@media (max-width: 768px) {
  *, *::before, *::after { box-sizing: border-box !important; }
  :root {
    --dsh-chat-content-width: 100% !important;
    --dsh-composer-card-max-width: 100% !important;
    --dsh-composer-side-clearance: 8px !important;
    --dsh-composer-dock-inset: 4px !important;
  }
  body, html, #root { width: 100vw !important; max-width: 100vw !important; overflow-x: hidden !important; }
  div[class*="frame"] { width: 100% !important; max-width: 100vw !important; overflow-x: hidden !important; }
  div[class*="centerCol"] { min-width: 0 !important; flex: 1 1 auto !important; width: 100% !important; max-width: 100% !important; }
  div[class*="handle"] { display: none !important; }
  div[class*="SettingsRoot_overlay"] { padding: 0 !important; align-items: stretch !important; justify-content: stretch !important; }
  div[class*="SettingsRoot_panel"] {
    position: fixed !important; inset: 0 !important; width: 100vw !important; max-width: 100vw !important; height: 100vh !important; max-height: 100vh !important;
    border-radius: 0 !important; display: flex !important; flex-direction: column !important; box-shadow: none !important; overflow: hidden !important;
  }
  nav[class*="SettingsRoot_nav"] {
    flex: none !important; width: 100% !important; display: flex !important; flex-direction: row !important; align-items: center !important;
    padding: 8px 10px !important; gap: 6px !important; overflow-x: auto !important; overflow-y: hidden !important;
    border-bottom: 1px solid var(--dsw-alias-border-l2) !important; background: var(--dsw-specific-sidebar-fill) !important;
  }
  div[class*="SettingsRoot_navTitle"] { display: none !important; }
  div[class*="SettingsRoot_navList"] { display: flex !important; flex-direction: row !important; flex-wrap: nowrap !important; gap: 6px !important; width: auto !important; }
  button[class*="SettingsRoot_navCell"] { flex: 0 0 auto !important; width: auto !important; height: 36px !important; padding: 6px 14px !important; font-size: 13px !important; white-space: nowrap !important; }
  div[class*="SettingsRoot_content"] { flex: 1 1 auto !important; width: 100% !important; min-height: 0 !important; display: flex !important; flex-direction: column !important; overflow: hidden !important; }
  div[class*="SettingsRoot_header"] { flex: none !important; height: 48px !important; padding: 8px 14px !important; }
  div[class*="SettingsRoot_options"] { flex: 1 1 auto !important; width: 100% !important; padding: 12px 14px 28px !important; overflow-y: auto !important; overflow-x: hidden !important; }
  div[class*="ModelsSection_section"],
  div[class*="GeneralSection_section"],
  div[class*="PluginsSettingsSection_section"],
  div[class*="PluginInventorySettingsTab_tab"],
  div[class*="AgentPresetSection_section"] { max-width: 100% !important; width: 100% !important; }
  div[class*="ModelsSection_rowCard"],
  div[class*="PluginCard_card"] { max-width: 100% !important; width: 100% !important; padding: 12px 10px !important; }
  div[class*="ModelsSection_rowHead"],
  div[class*="PluginCard_head"] { flex-wrap: wrap !important; gap: 8px !important; }
  div[class*="ProviderEditor_row"],
  div[class*="fields_row"] { flex-direction: column !important; align-items: stretch !important; gap: 6px !important; }
  div[class*="ProviderEditor_label"],
  div[class*="fields_label"] { width: 100% !important; max-width: 100% !important; }
  input, textarea, select { max-width: 100% !important; }
  div[class*="Modal_root"] { padding: 12px !important; }
  div[class*="Modal_dialog"] { width: 100% !important; max-width: calc(100vw - 24px) !important; border-radius: 16px !important; }
  div[class*="ChatView_scroll"] { padding: 10px 8px !important; }
  div[class*="InputBar_card"] { width: 100% !important; max-width: 100% !important; }
}
</style>`;
for (const p of paths) {
  if (fs.existsSync(p)) {
    let html = fs.readFileSync(p, 'utf8');
    if (!html.includes('dsh-mobile-responsive-engine')) {
      html = html.replace('</head>', style + '\n</head>');
      fs.writeFileSync(p, html);
      console.log('==> applied mobile engine to:', p);
    }
  }
}
EOF_DSH_PATCH
node /tmp/dsh_patch.js || true
rm -f /tmp/dsh_patch.js
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