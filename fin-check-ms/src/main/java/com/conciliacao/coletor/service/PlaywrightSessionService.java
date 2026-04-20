package com.conciliacao.coletor.service;

import com.conciliacao.coletor.dto.ConcifixSession;
import com.conciliacao.coletor.exception.LoginConciflexException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Handles the full Conciflex login flow as mapped from the live HTML:
 *
 *  1. /login: fill #username and #userpassword, click &lt;a id="submitFormLogin"&gt;
 *  2. Wait for the AJAX POST /login response (waitForResponse) — login is AJAX-based
 *  3. After AJAX: Bootstrap/jQuery opens #modal_clientes (user_comum with clients)
 *               OR #staticBackdrop (usuario_global with multiple companies)
 *               OR #modalModulos directly (user_comum without clients)
 *  4. In #modal_clientes: select #combo_cliente, click #bt_form_clientes
 *     → submitFormModalClientes() shows #modalModulos
 *  5. In #modalModulos: click #card-modulo-cartoes
 *     → selecionarModulo() submits form_modal_empresas to /login-global (full navigation)
 *  6. After navigation: extract cookies laravel_session, XSRF-TOKEN, cf_clearance
 *  7. Extract _token CSRF from meta tag or input[name="_token"]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightSessionService {

    private static final String BASE_URL  = "https://login.conciflex.com.br";
    private static final String DEBUG_DIR = "/tmp/playwright-debug";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HHmmss");

    // Selectors
    private static final String   SEL_USERNAME          = "#username";
    private static final String   SEL_USERPASSWORD      = "#userpassword";
    private static final String   SEL_SUBMIT_LOGIN      = "#submitFormLogin";
    private static final String   SEL_BUSCA_EMPRESA     = "#buscaEmpresaNome";
    private static final String   SEL_COMBO_CLIENTE     = "#combo_cliente";
    private static final String   SEL_BT_FORM_CLIENTES  = "#bt_form_clientes";
    private static final String   SEL_CARD_CARTOES      = "#card-modulo-cartoes";
    private static final String[] COMPANY_LINK_SELECTORS = {
        "#bodymodal a:visible", "#bodymodal div[id] a", "#bodymodal a"
    };

    // Modal IDs
    private static final String MODAL_CLIENTES = "modal_clientes";
    private static final String MODAL_EMPRESAS = "staticBackdrop";
    private static final String MODAL_MODULOS  = "modalModulos";

    // Cookie names
    private static final String COOKIE_LARAVEL_SESSION = "laravel_session";
    private static final String COOKIE_XSRF_TOKEN      = "XSRF-TOKEN";
    private static final String COOKIE_CF_CLEARANCE    = "cf_clearance";

    // Wait durations (ms) — values unchanged from original
    private static final long MODAL_ANIMATION_WAIT_MS = 800;
    private static final long FILTER_DEBOUNCE_WAIT_MS = 500;
    private static final long MODAL_RECHECK_WAIT_MS   = 2000;

    private final Browser browser;

    @Value("${coleta.timeout-segundos:60}")
    private int timeoutSegundos;

    public ConcifixSession autenticar(String login, String senha, String identificadorConciflex) {
        long timeoutMs = (long) timeoutSegundos * 1000;
        DebugContext ctx = DebugContext.create(identificadorConciflex);
        prepararDir(ctx);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                          "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setViewportSize(1280, 720)
            .setLocale("pt-BR"));

        Page page = context.newPage();

        try {
            openLogin(page, ctx, timeoutMs, login, senha, identificadorConciflex);

            log.info("[{}] PASSO 2 — AJAX response received", ctx.sessionId());

            page.waitForTimeout(MODAL_ANIMATION_WAIT_MS);
            takeScreenshot(page, ctx, "p2_pos_ajax");

            log.info("[{}] PASSO 3 — Checking modal state", ctx.sessionId());

            boolean[] modalState    = waitForModalsAfterLogin(page, ctx, identificadorConciflex);
            boolean clientesVisivel = modalState[0];
            boolean empresasVisivel = modalState[1];

            if (empresasVisivel) {
                boolean[] afterEmpresas = handleEmpresasModal(page, ctx, identificadorConciflex);
                clientesVisivel = afterEmpresas[0];
            }

            if (clientesVisivel || isModalVisible(page, MODAL_CLIENTES)) {
                handleClientesModal(page, ctx, identificadorConciflex);
            }

            clickModalCards(page, ctx, identificadorConciflex, timeoutMs);

            logEstado(page, ctx, "p5_pos_navegacao");
            takeScreenshot(page, ctx, "p5_pos_navegacao");

            log.info("[{}] Session authenticated for '{}'", ctx.sessionId(), identificadorConciflex);

            return extrairSessao(page, ctx, identificadorConciflex, context);

        } catch (LoginConciflexException e) {
            throw e;
        } catch (PlaywrightException e) {
            diagnostico(page, ctx, "erro_playwright_inesperado");
            throw buildLoginException(ctx, identificadorConciflex, "Playwright inesperado: " + e.getMessage());
        } finally {
            page.close();
            context.close();
        }
    }

    /** Polls all three post-login modals; re-checks after 2s if none appear; throws on bad credentials or timeout. */
    private boolean[] waitForModalsAfterLogin(Page page, DebugContext ctx, String identificadorConciflex) {
        boolean clientesVisivel = isModalVisible(page, MODAL_CLIENTES);
        boolean empresasVisivel = isModalVisible(page, MODAL_EMPRESAS);
        boolean modulosVisivel  = isModalVisible(page, MODAL_MODULOS);

        log.info("[{}] PASSO 3 — modal_clientes={} staticBackdrop={} modalModulos={}",
            ctx.sessionId(), clientesVisivel, empresasVisivel, modulosVisivel);
        takeScreenshot(page, ctx, "p3_estado_modais");

        if (!clientesVisivel && !empresasVisivel && !modulosVisivel) {
            String bodyText = page.innerText("body");
            if (bodyText.contains("incorretas") || bodyText.contains("inválid") ||
                bodyText.contains("incorreto")) {
                throw buildLoginException(ctx, identificadorConciflex,
                    "Credenciais rejeitadas pelo Conciflex. Screenshots em " + DEBUG_DIR + "/" + ctx.sessionId());
            }
            log.info("[{}] PASSO 3 — No modal visible yet, waiting 2s...", ctx.sessionId());
            page.waitForTimeout(MODAL_RECHECK_WAIT_MS);

            clientesVisivel = isModalVisible(page, MODAL_CLIENTES);
            empresasVisivel = isModalVisible(page, MODAL_EMPRESAS);
            modulosVisivel  = isModalVisible(page, MODAL_MODULOS);
            log.info("[{}] PASSO 3 — (re-check) modal_clientes={} staticBackdrop={} modalModulos={}",
                ctx.sessionId(), clientesVisivel, empresasVisivel, modulosVisivel);
            takeScreenshot(page, ctx, "p3_recheck_modais");

            if (!clientesVisivel && !empresasVisivel && !modulosVisivel) {
                diagnostico(page, ctx, "p3_sem_modal_apos_espera");
                throw buildLoginException(ctx, identificadorConciflex,
                    "Nenhum modal apareceu 3s após o AJAX. Screenshots em " + DEBUG_DIR + "/" + ctx.sessionId());
            }
        }

        return new boolean[]{clientesVisivel, empresasVisivel, modulosVisivel};
    }

    /** Handles #staticBackdrop (multi-company user): filters by name, clicks the company link, re-checks next modal. */
    private boolean[] handleEmpresasModal(Page page, DebugContext ctx, String identificadorConciflex) {
        log.info("[{}] PASSO 3-A — Modal Empresas visible, searching for company", ctx.sessionId());
        takeScreenshot(page, ctx, "p3a_modal_empresas");

        page.locator(SEL_BUSCA_EMPRESA).fill(identificadorConciflex);
        page.waitForTimeout(FILTER_DEBOUNCE_WAIT_MS); // waits for the escolherNome() JS setTimeout(300)

        String items = getFilteredCompanyNames(page);
        log.info("[{}] PASSO 3-A — Filtered companies: {}", ctx.sessionId(), items);
        takeScreenshot(page, ctx, "p3a_empresas_filtradas");

        clickFirstVisibleCompanyLink(page, ctx, identificadorConciflex);

        page.waitForTimeout(MODAL_ANIMATION_WAIT_MS);
        boolean modulosVisivel  = isModalVisible(page, MODAL_MODULOS);
        boolean clientesVisivel = isModalVisible(page, MODAL_CLIENTES);
        log.info("[{}] PASSO 3-A — After company click: modal_clientes={} modalModulos={}",
            ctx.sessionId(), clientesVisivel, modulosVisivel);
        takeScreenshot(page, ctx, "p3a_pos_empresa");

        return new boolean[]{clientesVisivel, modulosVisivel};
    }

    /** Tries each selector in COMPANY_LINK_SELECTORS and clicks the first match; throws if none found. */
    private void clickFirstVisibleCompanyLink(Page page, DebugContext ctx, String identificadorConciflex) {
        boolean companyClicked = false;
        for (String sel : COMPANY_LINK_SELECTORS) {
            try {
                Locator loc = page.locator(sel).first();
                if (loc.count() > 0) {
                    loc.click();
                    companyClicked = true;
                    log.info("[{}] PASSO 3-A — Company clicked via '{}'", ctx.sessionId(), sel);
                    break;
                }
            } catch (PlaywrightException ignored) {}
        }

        if (!companyClicked) {
            diagnostico(page, ctx, "p3a_empresa_nao_encontrada");
            throw buildLoginException(ctx, identificadorConciflex,
                "Empresa não encontrada no modal Empresas. Items: " + getFilteredCompanyNames(page));
        }
    }

    /** Handles #modal_clientes: selects the establishment in #combo_cliente and confirms. */
    private void handleClientesModal(Page page, DebugContext ctx, String identificadorConciflex) {
        log.info("[{}] PASSO 4 — Selecting '{}' in #combo_cliente", ctx.sessionId(), identificadorConciflex);
        takeScreenshot(page, ctx, "p4_modal_clientes");

        String opcoes = getAvailableComboOptions(page);
        log.info("[{}] PASSO 4 — Available options: {}", ctx.sessionId(), opcoes);

        selectClienteFromCombo(page, ctx, identificadorConciflex);

        takeScreenshot(page, ctx, "p4_cliente_selecionado");
        page.locator(SEL_BT_FORM_CLIENTES).click();
        log.info("[{}] PASSO 4 — #bt_form_clientes clicked", ctx.sessionId());

        page.waitForTimeout(MODAL_ANIMATION_WAIT_MS);
        takeScreenshot(page, ctx, "p4_pos_bt_clientes");
    }

    /** Selects the establishment by exact label in #combo_cliente; falls back to first non-empty option if not found. */
    private void selectClienteFromCombo(Page page, DebugContext ctx, String identificadorConciflex) {
        try {
            page.locator(SEL_COMBO_CLIENTE).selectOption(
                new com.microsoft.playwright.options.SelectOption()
                    .setLabel(identificadorConciflex));
            log.info("[{}] PASSO 4 — Selected by exact text", ctx.sessionId());
        } catch (PlaywrightException e) {
            log.warn("[{}] PASSO 4 — '{}' not found, selecting first available. Options: {}",
                ctx.sessionId(), identificadorConciflex, getAvailableComboOptions(page));
            selectFirstNonEmptyComboOption(page);
        }
    }

    private void clickModalCards(Page page, DebugContext ctx, String identificadorConciflex, long timeoutMs) {
        boolean modulosVisivel = isModalVisible(page, MODAL_MODULOS);
        log.info("[{}] PASSO 5 — #modalModulos visible: {}", ctx.sessionId(), modulosVisivel);

        if (!modulosVisivel) {
            diagnostico(page, ctx, "p5_modalModulos_nao_visivel");
            throw buildLoginException(ctx, identificadorConciflex,
                "#modalModulos não apareceu. URL: " + page.url());
        }

        String modulos = getVisibleModuleIds(page);
        log.info("[{}] PASSO 5 — Modules: {}", ctx.sessionId(), modulos);
        takeScreenshot(page, ctx, "p5_modal_modulos");

        // Clicking #card-modulo-cartoes submits form_modal_empresas (POST /login-global) — full page navigation.
        // waitForNavigation wraps the click to synchronize with the resulting form.submit().
        log.info("[{}] PASSO 5 — Clicking #card-modulo-cartoes (will navigate to /login-global)", ctx.sessionId());
        try {
            page.waitForNavigation(
                new Page.WaitForNavigationOptions()
                    .setTimeout(timeoutMs)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED),
                () -> page.locator(SEL_CARD_CARTOES).click()
            );
        } catch (PlaywrightException e) {
            // Navigation may have completed before waitForNavigation registered — check current URL.
            log.warn("[{}] PASSO 5 — waitForNavigation exception (may have already navigated): {}",
                ctx.sessionId(), e.getMessage());
        }
    }

    private ConcifixSession extrairSessao(Page page, DebugContext ctx, String identificadorConciflex,
                                           BrowserContext context) {
        log.info("[{}] PASSO 6 — Extracting cookies. Current URL: {}", ctx.sessionId(), page.url());

        List<com.microsoft.playwright.options.Cookie> cookies = context.cookies(BASE_URL);
        log.info("[{}] PASSO 6 — Cookies present: {}",
            ctx.sessionId(), cookies.stream().map(c -> c.name).toList());

        String laravelSession = extractCookie(cookies, COOKIE_LARAVEL_SESSION)
            .orElseThrow(() -> buildLoginException(ctx, identificadorConciflex,
                "Cookie laravel_session ausente. Cookies: " +
                    cookies.stream().map(c -> c.name).toList()));

        String xsrfToken = extractCookie(cookies, COOKIE_XSRF_TOKEN)
            .map(v -> URLDecoder.decode(v, StandardCharsets.UTF_8))
            .orElseThrow(() -> buildLoginException(ctx, identificadorConciflex,
                "Cookie XSRF-TOKEN ausente. Cookies: " +
                    cookies.stream().map(c -> c.name).toList()));

        String cfClearance = extractCookie(cookies, COOKIE_CF_CLEARANCE).orElse("");

        log.info("[{}] PASSO 7 — Extracting CSRF token", ctx.sessionId());
        String csrfToken = extrairCsrfToken(page, ctx);

        if (csrfToken == null || csrfToken.isBlank()) {
            diagnostico(page, ctx, "p7_csrf_nao_encontrado");
            throw buildLoginException(ctx, identificadorConciflex,
                "Token CSRF não encontrado após navegação");
        }

        return new ConcifixSession(laravelSession, xsrfToken, cfClearance, csrfToken);
    }

    private void openLogin(Page page, DebugContext ctx, long timeoutMs,
                           String login, String senha, String identificadorConciflex) {
        log.info("[{}] PASSO 1 — Opening {}/login", ctx.sessionId(), BASE_URL);
        page.navigate(BASE_URL + "/login");

        page.waitForSelector(SEL_USERNAME,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));

        logEstado(page, ctx, "p1");
        takeScreenshot(page, ctx, "p1_login_carregado");

        // The submit button is <a id="submitFormLogin"> — it fires an AJAX POST to /login.
        // waitForResponse ensures the AJAX has completed before we inspect the DOM.
        log.info("[{}] PASSO 2 — Filling credentials and waiting for AJAX login response", ctx.sessionId());

        page.locator(SEL_USERNAME).fill(login);
        page.locator(SEL_USERPASSWORD).fill(senha);
        takeScreenshot(page, ctx, "p2_credenciais_preenchidas");

        try {
            page.waitForResponse(
                r -> r.url().contains("/login") && "POST".equals(r.request().method()),
                () -> page.locator(SEL_SUBMIT_LOGIN).click()
            );
        } catch (PlaywrightException e) {
            diagnostico(page, ctx, "p2_ajax_falhou");
            throw buildLoginException(ctx, identificadorConciflex, "AJAX do login falhou: " + e.getMessage());
        }
    }

    // ─── JS helper methods ────────────────────────────────────────────────────

    /** Returns pipe-separated names of filtered company anchors inside #bodymodal. */
    private String getFilteredCompanyNames(Page page) {
        return (String) page.evaluate(
            "() => Array.from(document.querySelectorAll('#bodymodal a'))" +
            "       .map(a => a.innerText?.trim()).filter(Boolean).join(' | ')");
    }

    /** Returns pipe-separated label=value pairs for all #combo_cliente options. */
    private String getAvailableComboOptions(Page page) {
        return (String) page.evaluate(
            "() => Array.from(document.querySelectorAll('#combo_cliente option'))" +
            "       .map(o => '\"' + o.text.trim() + '\"=' + o.value).join(' | ')");
    }

    /** Returns pipe-separated IDs and enabled state for all [id^=card-modulo] elements. */
    private String getVisibleModuleIds(Page page) {
        return (String) page.evaluate(
            "() => Array.from(document.querySelectorAll('[id^=card-modulo]'))" +
            "       .map(el => el.id + (el.className.includes('disabled') ? '[off]' : '[ok]')).join(' | ')");
    }

    /** Selects the first non-empty option in #combo_cliente and dispatches a change event. */
    private void selectFirstNonEmptyComboOption(Page page) {
        page.evaluate(
            "() => { const s = document.getElementById('combo_cliente'); " +
            "for (let i = 0; i < s.options.length; i++) { " +
            "  if (s.options[i].value) { s.selectedIndex = i; " +
            "    s.dispatchEvent(new Event('change')); break; } } }");
    }

    // ─── Utility helpers ──────────────────────────────────────────────────────

    /**
     * Checks whether a Bootstrap modal is visible by testing both the 'show' class
     * and style.display — jQuery sets both during the open transition.
     */
    private boolean isModalVisible(Page page, String modalId) {
        try {
            return (Boolean) page.evaluate(
                "() => { " +
                "  const m = document.getElementById('" + modalId + "'); " +
                "  if (!m) return false; " +
                "  const cls = m.classList.contains('show'); " +
                "  const style = m.style.display === 'block'; " +
                "  return cls || style; " +
                "}");
        } catch (Exception e) {
            return false;
        }
    }

    /** Reads the CSRF token from the meta tag or a hidden _token input. */
    private String extrairCsrfToken(Page page, DebugContext ctx) {
        try {
            return (String) page.evaluate(
                "() => document.querySelector('meta[name=\"csrf-token\"]')?.content " +
                "   || document.querySelector('input[name=\"_token\"]')?.value " +
                "   || ''");
        } catch (Exception e) {
            log.warn("[{}] Error extracting CSRF token: {}", ctx.sessionId(), e.getMessage());
            return null;
        }
    }

    /** Logs the current URL and page title at a named step. */
    private void logEstado(Page page, DebugContext ctx, String passo) {
        try {
            log.info("[{}] [{}] URL={} | Título='{}'", ctx.sessionId(), passo, page.url(), page.title());
        } catch (Exception ignored) {}
    }

    /** Captures a full-page screenshot to the session debug directory. */
    private void takeScreenshot(Page page, DebugContext ctx, String nome) {
        try {
            Files.createDirectories(ctx.debugDir());
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(ctx.debugDir().resolve(nome + ".png")).setFullPage(true));
            log.info("[{}] Screenshot: {}.png", ctx.sessionId(), nome);
        } catch (Exception e) {
            log.warn("[{}] Failed screenshot '{}': {}", ctx.sessionId(), nome, e.getMessage());
        }
    }

    /** Captures a screenshot and dumps the page HTML to the session debug directory. */
    private void diagnostico(Page page, DebugContext ctx, String nome) {
        takeScreenshot(page, ctx, nome);
        try {
            Files.createDirectories(ctx.debugDir());
            Files.writeString(ctx.debugDir().resolve(nome + ".html"), page.content());
        } catch (Exception ignored) {}
    }

    /** Creates the session debug directory up-front so screenshots never fail on a missing parent. */
    private void prepararDir(DebugContext ctx) {
        try { Files.createDirectories(ctx.debugDir()); } catch (Exception ignored) {}
    }

    /** Builds a LoginConciflexException with the debug path appended to the reason. */
    private LoginConciflexException buildLoginException(DebugContext ctx, String estab, String motivo) {
        return new LoginConciflexException(estab,
            motivo + " [debug: " + DEBUG_DIR + "/" + ctx.sessionId() + "]");
    }

    /** Finds the first cookie matching {@code name} and returns its value. */
    private Optional<String> extractCookie(
            List<com.microsoft.playwright.options.Cookie> cookies, String name) {
        return cookies.stream()
            .filter(c -> name.equals(c.name))
            .map(c -> c.value)
            .findFirst();
    }

    // ─── Nested types ─────────────────────────────────────────────────────────

    /** Bundles session identity and debug directory to avoid passing both as separate parameters. */
    private record DebugContext(String sessionId, Path debugDir) {
        static DebugContext create(String identificadorConciflex) {
            String id = LocalDateTime.now().format(TS) + "_" +
                identificadorConciflex.replaceAll("[^a-zA-Z0-9]", "_")
                    .substring(0, Math.min(20, identificadorConciflex.length()));
            return new DebugContext(id, Paths.get(DEBUG_DIR, id));
        }
    }
}
