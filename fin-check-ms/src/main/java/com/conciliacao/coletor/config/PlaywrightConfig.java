package com.conciliacao.coletor.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PlaywrightConfig {

    @Value("${playwright.headless:true}")
    private boolean headless;

    /**
     * Em milissegundos — adiciona pausa entre cada ação do Playwright.
     * Útil para debug com headless=false: defina PLAYWRIGHT_SLOW_MO=500 para ver cada passo.
     * Padrão: 0 (sem pausa, produção).
     */
    @Value("${playwright.slow-mo:0}")
    private double slowMo;

    /**
     * Playwright é criado uma única vez na inicialização da aplicação.
     * Encerrado automaticamente pelo Spring ao destruir o contexto.
     */
    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    /**
     * Browser Chromium compartilhado — cada coleta abre um BrowserContext isolado.
     * Isso evita vazamento de sessão entre estabelecimentos diferentes.
     * Configurado para evitar detecção de automação por fingerprint.
     *
     * Para debug local: PLAYWRIGHT_HEADLESS=false e PLAYWRIGHT_SLOW_MO=500
     */
    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
            .setHeadless(headless)
            .setArgs(List.of(
                "--no-sandbox",                              // obrigatório em Docker
                "--disable-setuid-sandbox",
                "--disable-dev-shm-usage",                  // evita crash com /dev/shm limitado (64MB padrão)
                "--disable-blink-features=AutomationControlled", // evita detecção pelo site
                "--disable-extensions",
                "--disable-gpu"
            ));

        if (slowMo > 0) {
            opts.setSlowMo(slowMo);
        }

        return playwright.chromium().launch(opts);
    }
}
