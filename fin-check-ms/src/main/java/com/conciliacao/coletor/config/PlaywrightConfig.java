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
     * Milliseconds added between every Playwright action.
     * Set PLAYWRIGHT_SLOW_MO=500 with headless=false to observe each step during debugging.
     * Default: 0 (no delay — production).
     */
    @Value("${playwright.slow-mo:0}")
    private double slowMo;

    /**
     * Playwright instance created once at startup; destroyed automatically by Spring on shutdown.
     */
    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    /**
     * Shared Chromium browser — each collection run opens an isolated BrowserContext
     * to prevent session leakage between establishments.
     * Launch args suppress sandbox and GPU to run reliably inside Docker.
     * For local debugging: PLAYWRIGHT_HEADLESS=false and PLAYWRIGHT_SLOW_MO=500.
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
