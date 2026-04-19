package com.conciliacao.coletor.service;

import com.conciliacao.coletor.dto.ConcifixSession;
import com.conciliacao.coletor.exception.LoginConciflexException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
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
 * Fluxo real mapeado do HTML do Conciflex:
 *
 *  1. /login: preencher #username e #userpassword, clicar <a id="submitFormLogin">
 *  2. Aguardar resposta AJAX do POST /login (waitForResponse) — o login ocorre via AJAX
 *  3. Após AJAX: Bootstrap/jQuery abre #modal_clientes (user_comum com clientes)
 *               OU #staticBackdrop (usuario_global com múltiplas empresas)
 *               OU #modalModulos diretamente (user_comum sem clientes)
 *  4. Em #modal_clientes: selecionar #combo_cliente, clicar #bt_form_clientes
 *     → submitFormModalClientes() mostra #modalModulos
 *  5. Em #modalModulos: clicar #card-modulo-cartoes
 *     → selecionarModulo() submete form_modal_empresas para /login-global (navegação completa)
 *  6. Após navegação: extrair cookies laravel_session, XSRF-TOKEN, cf_clearance
 *  7. Extrair _token CSRF da meta tag ou input[name="_token"]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightSessionService {

    private static final String BASE_URL  = "https://login.conciflex.com.br";
    private static final String DEBUG_DIR = "/tmp/playwright-debug";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HHmmss");

    private final Browser browser;

    @Value("${coleta.timeout-segundos:60}")
    private int timeoutSegundos;

    public ConcifixSession autenticar(String login, String senha, String identificadorConciflex) {
        long timeoutMs = (long) timeoutSegundos * 1000;
        String sid = LocalDateTime.now().format(TS) + "_" +
            identificadorConciflex.replaceAll("[^a-zA-Z0-9]", "_")
                .substring(0, Math.min(20, identificadorConciflex.length()));

        prepararDir(sid);

        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                          "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setViewportSize(1280, 720)
            .setLocale("pt-BR"));

        Page page = context.newPage();

        try {
            // ─── PASSO 1: Abrir /login ────────────────────────────────────────────────
            log.info("[{}] PASSO 1 — Abrindo {}/login", sid, BASE_URL);
            page.navigate(BASE_URL + "/login");

            page.waitForSelector("#username",
                new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));

            logEstado(page, sid, "p1");
            shot(page, sid, "p1_login_carregado");

            // ─── PASSO 2: Credenciais + clicar #submitFormLogin ───────────────────────
            // O botão é <a id="submitFormLogin"> — dispara um AJAX POST para /login.
            // Usamos waitForResponse para aguardar a resposta AJAX antes de inspecionar o DOM.
            log.info("[{}] PASSO 2 — Preenchendo credenciais e aguardando resposta AJAX do login", sid);

            page.locator("#username").fill(login);
            page.locator("#userpassword").fill(senha);
            shot(page, sid, "p2_credenciais_preenchidas");

            // Clica em #submitFormLogin e aguarda a resposta do POST /login simultaneamente.
            // Sem waitForResponse, o AJAX pode não ter completado quando checamos o DOM.
            try {
                page.waitForResponse(
                    r -> r.url().contains("/login") && "POST".equals(r.request().method()),
                    () -> page.locator("#submitFormLogin").click()
                );
            } catch (PlaywrightException e) {
                diagnostico(page, sid, "p2_ajax_falhou");
                throw falha(sid, identificadorConciflex, "AJAX do login falhou: " + e.getMessage());
            }

            log.info("[{}] PASSO 2 — Resposta AJAX recebida", sid);

            // Aguarda o jQuery/Bootstrap processar a resposta e animar o modal (Bootstrap transition)
            page.waitForTimeout(800);
            shot(page, sid, "p2_pos_ajax");

            // ─── PASSO 3: Detectar qual modal apareceu ────────────────────────────────
            // user_comum com clientes  → #modal_clientes (mais comum)
            // usuario_global           → #staticBackdrop (Empresas)
            // user_comum sem clientes  → #modalModulos diretamente
            // credenciais erradas      → nenhum modal / SweetAlert de erro
            log.info("[{}] PASSO 3 — Verificando estado dos modais", sid);

            boolean clientesVisivel  = isModalVisible(page, "modal_clientes");
            boolean empresasVisivel  = isModalVisible(page, "staticBackdrop");
            boolean modulosVisivel   = isModalVisible(page, "modalModulos");

            log.info("[{}] PASSO 3 — modal_clientes={} staticBackdrop={} modalModulos={}",
                sid, clientesVisivel, empresasVisivel, modulosVisivel);
            shot(page, sid, "p3_estado_modais");

            if (!clientesVisivel && !empresasVisivel && !modulosVisivel) {
                // Nenhum modal — verifica se há mensagem de erro de credenciais
                String bodyText = page.innerText("body");
                if (bodyText.contains("incorretas") || bodyText.contains("inválid") ||
                    bodyText.contains("incorreto")) {
                    throw falha(sid, identificadorConciflex,
                        "Credenciais rejeitadas pelo Conciflex. Screenshots em " + DEBUG_DIR + "/" + sid);
                }
                // Modal pode ainda estar animando — aguarda mais um pouco
                log.info("[{}] PASSO 3 — Nenhum modal ainda visível, aguardando mais 2s...", sid);
                page.waitForTimeout(2000);

                clientesVisivel = isModalVisible(page, "modal_clientes");
                empresasVisivel = isModalVisible(page, "staticBackdrop");
                modulosVisivel  = isModalVisible(page, "modalModulos");
                log.info("[{}] PASSO 3 — (re-check) modal_clientes={} staticBackdrop={} modalModulos={}",
                    sid, clientesVisivel, empresasVisivel, modulosVisivel);
                shot(page, sid, "p3_recheck_modais");

                if (!clientesVisivel && !empresasVisivel && !modulosVisivel) {
                    diagnostico(page, sid, "p3_sem_modal_apos_espera");
                    throw falha(sid, identificadorConciflex,
                        "Nenhum modal apareceu 3s após o AJAX. Screenshots em " + DEBUG_DIR + "/" + sid);
                }
            }

            // ─── PASSO 3-A: Modal Empresas (#staticBackdrop) ─────────────────────────
            if (empresasVisivel) {
                log.info("[{}] PASSO 3-A — Modal Empresas visível, buscando empresa", sid);
                shot(page, sid, "p3a_modal_empresas");

                // Digita o nome da empresa no campo de busca para filtrar
                page.locator("#buscaEmpresaNome").fill(identificadorConciflex);
                page.waitForTimeout(500); // aguarda o setTimeout(300) do escolherNome() JS

                String items = (String) page.evaluate(
                    "() => Array.from(document.querySelectorAll('#bodymodal a'))" +
                    "       .map(a => a.innerText?.trim()).filter(Boolean).join(' | ')");
                log.info("[{}] PASSO 3-A — Empresas filtradas: {}", sid, items);
                shot(page, sid, "p3a_empresas_filtradas");

                // Clica no link <a> com o nome da empresa dentro de #bodymodal
                boolean clicou = false;
                for (String sel : new String[]{
                    "#bodymodal a:visible",
                    "#bodymodal div[id] a",
                    "#bodymodal a"
                }) {
                    try {
                        Locator loc = page.locator(sel).first();
                        if (loc.count() > 0) {
                            loc.click();
                            clicou = true;
                            log.info("[{}] PASSO 3-A — Empresa clicada via '{}'", sid, sel);
                            break;
                        }
                    } catch (PlaywrightException ignored) {}
                }

                if (!clicou) {
                    diagnostico(page, sid, "p3a_empresa_nao_encontrada");
                    throw falha(sid, identificadorConciflex,
                        "Empresa não encontrada no modal Empresas. Items: " + items);
                }

                page.waitForTimeout(800);
                modulosVisivel  = isModalVisible(page, "modalModulos");
                clientesVisivel = isModalVisible(page, "modal_clientes");
                log.info("[{}] PASSO 3-A — Após clicar empresa: modal_clientes={} modalModulos={}",
                    sid, clientesVisivel, modulosVisivel);
                shot(page, sid, "p3a_pos_empresa");
            }

            // ─── PASSO 4: Modal Clientes (#modal_clientes) ───────────────────────────
            if (clientesVisivel || isModalVisible(page, "modal_clientes")) {
                log.info("[{}] PASSO 4 — Selecionando '{}' em #combo_cliente", sid, identificadorConciflex);
                shot(page, sid, "p4_modal_clientes");

                String opcoes = (String) page.evaluate(
                    "() => Array.from(document.querySelectorAll('#combo_cliente option'))" +
                    "       .map(o => '\"' + o.text.trim() + '\"=' + o.value).join(' | ')");
                log.info("[{}] PASSO 4 — Opções disponíveis: {}", sid, opcoes);

                try {
                    page.locator("#combo_cliente").selectOption(
                        new com.microsoft.playwright.options.SelectOption()
                            .setLabel(identificadorConciflex));
                    log.info("[{}] PASSO 4 — Selecionado pelo texto exato", sid);
                } catch (PlaywrightException e) {
                    // Fallback: seleciona a primeira opção com valor não-vazio
                    log.warn("[{}] PASSO 4 — '{}' não encontrado, selecionando primeiro disponível. Opções: {}",
                        sid, identificadorConciflex, opcoes);
                    page.evaluate(
                        "() => { const s = document.getElementById('combo_cliente'); " +
                        "for (let i = 0; i < s.options.length; i++) { " +
                        "  if (s.options[i].value) { s.selectedIndex = i; " +
                        "    s.dispatchEvent(new Event('change')); break; } } }");
                }

                shot(page, sid, "p4_cliente_selecionado");
                page.locator("#bt_form_clientes").click();
                log.info("[{}] PASSO 4 — #bt_form_clientes clicado", sid);

                // Aguarda jQuery animar o #modalModulos
                page.waitForTimeout(800);
                shot(page, sid, "p4_pos_bt_clientes");
            }

            // ─── PASSO 5: Modal de módulos (#modalModulos) → clicar Cartões ──────────
            // Verifica se #modalModulos está visível (pode já estar se veio direto de user_comum sem clientes)
            modulosVisivel = isModalVisible(page, "modalModulos");
            log.info("[{}] PASSO 5 — #modalModulos visível: {}", sid, modulosVisivel);

            if (!modulosVisivel) {
                diagnostico(page, sid, "p5_modalModulos_nao_visivel");
                throw falha(sid, identificadorConciflex,
                    "#modalModulos não apareceu. URL: " + page.url());
            }

            String modulos = (String) page.evaluate(
                "() => Array.from(document.querySelectorAll('[id^=card-modulo]'))" +
                "       .map(el => el.id + (el.className.includes('disabled') ? '[off]' : '[ok]')).join(' | ')");
            log.info("[{}] PASSO 5 — Módulos: {}", sid, modulos);
            shot(page, sid, "p5_modal_modulos");

            // Clicar em #card-modulo-cartoes submete form_modal_empresas (POST /login-global) → navegação completa!
            // waitForNavigation envolve o click para aguardar a navegação resultante do form.submit()
            log.info("[{}] PASSO 5 — Clicando #card-modulo-cartoes (causará navegação para /login-global)", sid);
            try {
                page.waitForNavigation(
                    new Page.WaitForNavigationOptions()
                        .setTimeout(timeoutMs)
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED),
                    () -> page.locator("#card-modulo-cartoes").click()
                );
            } catch (PlaywrightException e) {
                // Pode já ter navegado antes do waitForNavigation — verifica a URL atual
                log.warn("[{}] PASSO 5 — waitForNavigation exception (pode ter já navegado): {}", sid, e.getMessage());
            }

            logEstado(page, sid, "p5_pos_navegacao");
            shot(page, sid, "p5_pos_navegacao");

            // ─── PASSO 6: Extrair cookies ─────────────────────────────────────────────
            log.info("[{}] PASSO 6 — Extraindo cookies. URL atual: {}", sid, page.url());

            List<com.microsoft.playwright.options.Cookie> cookies = context.cookies(BASE_URL);
            log.info("[{}] PASSO 6 — Cookies presentes: {}",
                sid, cookies.stream().map(c -> c.name).toList());

            String laravelSession = extractCookie(cookies, "laravel_session")
                .orElseThrow(() -> falha(sid, identificadorConciflex,
                    "Cookie laravel_session ausente. Cookies: " +
                    cookies.stream().map(c -> c.name).toList()));

            String xsrfToken = extractCookie(cookies, "XSRF-TOKEN")
                .map(v -> URLDecoder.decode(v, StandardCharsets.UTF_8))
                .orElseThrow(() -> falha(sid, identificadorConciflex,
                    "Cookie XSRF-TOKEN ausente. Cookies: " +
                    cookies.stream().map(c -> c.name).toList()));

            String cfClearance = extractCookie(cookies, "cf_clearance").orElse("");

            // ─── PASSO 7: Extrair CSRF token ─────────────────────────────────────────
            log.info("[{}] PASSO 7 — Extraindo CSRF token", sid);
            String csrfToken = extrairCsrfToken(page, sid);

            if (csrfToken == null || csrfToken.isBlank()) {
                // Tenta navegar para a página de conciliação para obter o _token
                page.navigate(BASE_URL + "/conciliacao-taxas");
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                shot(page, sid, "p7_conciliacao_taxas");
                csrfToken = extrairCsrfToken(page, sid);
            }

            if (csrfToken == null || csrfToken.isBlank()) {
                diagnostico(page, sid, "p7_csrf_nao_encontrado");
                throw falha(sid, identificadorConciflex, "Token CSRF não encontrado após navegação");
            }

            log.info("[{}] Sessão autenticada com sucesso para '{}'", sid, identificadorConciflex);
            return new ConcifixSession(laravelSession, xsrfToken, cfClearance, csrfToken);

        } catch (LoginConciflexException e) {
            throw e;
        } catch (PlaywrightException e) {
            diagnostico(page, sid, "erro_playwright_inesperado");
            throw falha(sid, identificadorConciflex, "Playwright inesperado: " + e.getMessage());
        } finally {
            page.close();
            context.close();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Verifica se um modal Bootstrap está visível verificando tanto a classe 'show'
     * quanto o atributo style 'display: block' — pois jQuery adiciona os dois.
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

    private String extrairCsrfToken(Page page, String sid) {
        try {
            return (String) page.evaluate(
                "() => document.querySelector('meta[name=\"csrf-token\"]')?.content " +
                "   || document.querySelector('input[name=\"_token\"]')?.value " +
                "   || ''");
        } catch (Exception e) {
            log.warn("[{}] Erro ao extrair CSRF token: {}", sid, e.getMessage());
            return null;
        }
    }

    private void logEstado(Page page, String sid, String passo) {
        try {
            log.info("[{}] [{}] URL={} | Título='{}'", sid, passo, page.url(), page.title());
        } catch (Exception ignored) {}
    }

    private void shot(Page page, String sid, String nome) {
        try {
            Path dir = Paths.get(DEBUG_DIR, sid);
            Files.createDirectories(dir);
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(dir.resolve(nome + ".png")).setFullPage(true));
            log.info("[{}] Screenshot: {}.png", sid, nome);
        } catch (Exception e) {
            log.warn("[{}] Falha screenshot '{}': {}", sid, nome, e.getMessage());
        }
    }

    private void diagnostico(Page page, String sid, String nome) {
        shot(page, sid, nome);
        try {
            Path dir = Paths.get(DEBUG_DIR, sid);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(nome + ".html"), page.content());
        } catch (Exception ignored) {}
    }

    private void prepararDir(String sid) {
        try { Files.createDirectories(Paths.get(DEBUG_DIR, sid)); } catch (Exception ignored) {}
    }

    private LoginConciflexException falha(String sid, String estab, String motivo) {
        return new LoginConciflexException(estab,
            motivo + " [debug: " + DEBUG_DIR + "/" + sid + "]");
    }

    private Optional<String> extractCookie(
            List<com.microsoft.playwright.options.Cookie> cookies, String name) {
        return cookies.stream()
            .filter(c -> name.equals(c.name))
            .map(c -> c.value)
            .findFirst();
    }
}
