<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, max-age=0');
header('X-Content-Type-Options: nosniff');

const AQUARITM_APPLICATION_BASE_URL =
    'https://aquanano.eu/aquaweb/aquaritm/aplicatie';

/**
 * Extrage o versiune de forma 0.1.10 din numele APK-ului.
 */
function aquaritm_version_from_filename(string $filename): ?string
{
    if (preg_match(
        '/(?:^|[^0-9])(\d+\.\d+\.\d+)(?:[^0-9]|$)/',
        $filename,
        $matches
    ) !== 1) {
        return null;
    }

    return $matches[1];
}

function aquaritm_apk_catalog(string $directory): array
{
    $versions = [];
    $warnings = [];

    $entries = scandir($directory);
    if ($entries === false) {
        throw new RuntimeException('Directorul aplicației nu poate fi citit.');
    }

    foreach ($entries as $filename) {
        if ($filename === '.' || $filename === '..' || $filename[0] === '.') {
            continue;
        }

        $path = $directory . DIRECTORY_SEPARATOR . $filename;
        if (!is_file($path) || strtolower(pathinfo($filename, PATHINFO_EXTENSION)) !== 'apk') {
            continue;
        }

        $version = aquaritm_version_from_filename($filename);
        if ($version === null) {
            $warnings[] = 'Ignorat; lipsește versiunea din nume: ' . $filename;
            continue;
        }

        $versions[] = [
            'id' => 'aquaritm-' . strtolower($version),
            'name' => 'AquaRitm ' . $version,
            'version' => $version,
            'apk' => AQUARITM_APPLICATION_BASE_URL . '/' . rawurlencode($filename),
            'filename' => $filename,
            'size_bytes' => filesize($path),
            'sha256' => hash_file('sha256', $path),
            'modified_at' => gmdate('c', (int) filemtime($path)),
        ];
    }

    usort($versions, static function (array $left, array $right): int {
        $byVersion = version_compare($right['version'], $left['version']);
        if ($byVersion !== 0) {
            return $byVersion;
        }

        return strcmp($right['modified_at'], $left['modified_at']);
    });

    return [
        'schema_version' => 1,
        'generated_at' => gmdate('c'),
        'latest' => $versions[0] ?? null,
        'versions' => $versions,
        'warnings' => $warnings,
    ];
}

try {
    echo json_encode(
        aquaritm_apk_catalog(__DIR__),
        JSON_UNESCAPED_UNICODE
            | JSON_UNESCAPED_SLASHES
            | JSON_PRETTY_PRINT
            | JSON_THROW_ON_ERROR
    );
} catch (Throwable $error) {
    http_response_code(500);
    echo json_encode(
        [
            'schema_version' => 1,
            'generated_at' => gmdate('c'),
            'error' => $error->getMessage(),
        ],
        JSON_UNESCAPED_UNICODE
            | JSON_UNESCAPED_SLASHES
            | JSON_PRETTY_PRINT
    );
}
