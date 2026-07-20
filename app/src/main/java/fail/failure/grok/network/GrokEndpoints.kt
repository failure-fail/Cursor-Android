package fail.failure.grok.network

/**
 * Production endpoints from xAI Grok Build (`xai-org/grok-build` /
 * `xai-grok-env` PRODUCTION_ENDPOINTS).
 */
object GrokEndpoints {
    /** Inference + sandbox proxy used by the Grok Build CLI. */
    const val CLI_CHAT_PROXY_BASE = "https://cli-chat-proxy.grok.com/v1/"

    /** SpaceXAI OAuth2 issuer (browser + device-code login). */
    const val OAUTH_ISSUER = "https://auth.x.ai"

    /** Public Grok Build OAuth2 client id (same as `grok login`). */
    const val OAUTH_CLIENT_ID = "b1a00492-073a-47ea-816f-4c329264a828"

    /** Token-auth header value the CLI sends as `X-XAI-Token-Auth`. */
    const val TOKEN_AUTH_HEADER = "xai-grok-cli"

    /** Match current Grok Build CLI (`xai-grok-version`); proxy rejects older builds. */
    const val CLIENT_VERSION = "0.2.106"

    /** Alias kept for call sites that still expect a single API base. */
    const val API_BASE = CLI_CHAT_PROXY_BASE
}
