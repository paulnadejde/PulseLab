<?php
declare(strict_types=1);

header('Content-Type: text/html; charset=utf-8');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
?>
<!doctype html>
<html lang="ro">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#080b12">
    <title>AquaRitm pentru Android</title>
    <style>
        :root {
            color-scheme: dark;
            --background: #080b12;
            --panel: #141923;
            --text: #f4f7fb;
            --muted: #b8c0cc;
            --accent: #45d6c4;
            --accent-dark: #176f69;
            --gold: #f5c75b;
        }

        * { box-sizing: border-box; }

        body {
            min-height: 100vh;
            margin: 0;
            display: grid;
            place-items: center;
            padding: 24px 16px;
            color: var(--text);
            background:
                radial-gradient(circle at 50% 0%, #18283c 0, transparent 42%),
                var(--background);
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }

        main {
            width: min(100%, 660px);
            padding: clamp(24px, 6vw, 46px);
            text-align: center;
            background: color-mix(in srgb, var(--panel) 94%, transparent);
            border: 1px solid #303a49;
            border-radius: 24px;
            box-shadow: 0 24px 70px #0008;
        }

        .mark {
            width: 88px;
            height: 88px;
            margin: 0 auto 18px;
            display: grid;
            place-items: center;
            border-radius: 24px;
            border: 2px solid #ffffff32;
            background: linear-gradient(145deg, #1e4965, #101927);
            box-shadow: inset 0 1px 1px #ffffff30, 0 12px 30px #0007;
        }

        .sun {
            width: 42px;
            height: 42px;
            border-radius: 50%;
            background: var(--gold);
            box-shadow: 0 0 24px #f5c75b88;
        }

        h1 { margin: 0; font-size: clamp(2rem, 9vw, 3.25rem); }
        .tagline { margin: 8px 0 24px; color: var(--accent); font-weight: 700; }
        .description { margin: 0 auto 26px; max-width: 520px; color: var(--muted); line-height: 1.6; }

        .version {
            min-height: 26px;
            margin-bottom: 13px;
            color: var(--muted);
            font-size: .95rem;
        }

        .download {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: min(100%, 300px);
            min-height: 54px;
            padding: 13px 22px;
            color: #041513;
            background: var(--accent);
            border-radius: 12px;
            font-weight: 800;
            text-decoration: none;
            box-shadow: inset 0 -3px 0 var(--accent-dark), 0 9px 22px #0006;
        }

        .download[aria-disabled="true"] {
            pointer-events: none;
            color: #aab0b8;
            background: #363e48;
            box-shadow: none;
        }

        .help { margin: 24px 0 0; color: var(--muted); font-size: .9rem; line-height: 1.5; }
        .error { color: #ff9b9b; }

        noscript p {
            padding: 12px;
            color: #ffd8a8;
            border: 1px solid #7f5b2c;
            border-radius: 10px;
        }
    </style>
</head>
<body>
<main>
    <div class="mark" aria-hidden="true"><div class="sun"></div></div>
    <h1>AquaRitm</h1>
    <p class="tagline">Ritm, sunet și cicluri solare</p>
    <p class="description">
        Aplicație Android cu Metronom, BioStim, MindExtra și SolaRitm.
        Descarcă mai jos cea mai nouă versiune disponibilă.
    </p>

    <div id="version" class="version" role="status" aria-live="polite">
        Citesc versiunea disponibilă…
    </div>
    <a id="download" class="download" href="#" aria-disabled="true">
        SE ÎNCARCĂ…
    </a>

    <p class="help">
        Fișierul este un APK pentru Android. Dacă telefonul solicită permisiunea,
        autorizează instalarea din browserul folosit pentru descărcare.
    </p>
    <noscript><p>Pagina are nevoie de JavaScript pentru identificarea ultimei versiuni.</p></noscript>
</main>

<script>
    (() => {
        'use strict';

        const version = document.getElementById('version');
        const download = document.getElementById('download');

        const humanSize = bytes => {
            if (!Number.isFinite(bytes) || bytes <= 0) return '';
            if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
            return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
        };

        fetch('./catalog_aplicatie.php', { cache: 'no-store' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(catalog => {
                const latest = catalog && catalog.latest;
                if (!latest || !latest.version || !latest.apk) {
                    throw new Error('Catalogul nu conține niciun APK.');
                }

                const size = humanSize(Number(latest.size_bytes));
                version.textContent = `Versiunea ${latest.version}${size ? ` · ${size}` : ''}`;
                download.href = latest.apk;
                download.textContent = `DESCARCĂ AQUARITM ${latest.version}`;
                download.setAttribute('download', latest.filename || `AquaRitm-${latest.version}.apk`);
                download.removeAttribute('aria-disabled');
            })
            .catch(() => {
                version.textContent = 'Descărcarea nu este disponibilă momentan.';
                version.classList.add('error');
                download.textContent = 'REÎNCEARCĂ';
                download.removeAttribute('aria-disabled');
                download.href = window.location.href;
            });
    })();
</script>
</body>
</html>
