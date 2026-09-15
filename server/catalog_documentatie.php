<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, max-age=0');
header('X-Content-Type-Options: nosniff');

const AQUARITM_DOCUMENTATION_BASE_URL =
    'https://aquanano.eu/aquaweb/aquaritm/documentatie';

function aquaritm_document_title(string $path, string $fallback, string $extension): string
{
    if (in_array($extension, ['md', 'txt'], true)) {
        $preview = file_get_contents($path, false, null, 0, 65536);
        if (is_string($preview)
            && preg_match('/^(?:\xEF\xBB\xBF)?\s*#\s+(.+)$/m', $preview, $matches) === 1) {
            $title = trim($matches[1]);
            if ($title !== '') {
                return $title;
            }
        }
    }

    $title = preg_replace('/^\d+[._ -]*/', '', $fallback);
    $title = preg_replace('/[_-]+/', ' ', (string) $title);
    return ucwords(trim((string) $title));
}

function aquaritm_document_id(string $stem): string
{
    $stem = preg_replace('/^\d+[._ -]*/', '', $stem);
    $id = preg_replace('/[^A-Za-z0-9._-]+/', '-', (string) $stem);
    return trim(strtolower((string) $id), '-');
}

function aquaritm_document_order(string $stem): int
{
    if (preg_match('/^(\d+)[._ -]/', $stem, $matches) === 1) {
        return (int) $matches[1];
    }
    return 10000;
}

function aquaritm_document_catalog(string $directory): array
{
    $documents = [];
    $warnings = [];
    $usedIds = [];

    $entries = scandir($directory);
    if ($entries === false) {
        throw new RuntimeException('Directorul documentației nu poate fi citit.');
    }

    foreach ($entries as $filename) {
        if ($filename === '.' || $filename === '..' || $filename[0] === '.') {
            continue;
        }

        $path = $directory . DIRECTORY_SEPARATOR . $filename;
        if (!is_file($path)) {
            continue;
        }

        $extension = strtolower(pathinfo($filename, PATHINFO_EXTENSION));
        if (!in_array($extension, ['md', 'txt', 'pdf'], true)) {
            continue;
        }

        $stem = pathinfo($filename, PATHINFO_FILENAME);
        $id = aquaritm_document_id($stem);
        if ($id === '') {
            $warnings[] = 'Ignorat; nume neacceptat: ' . $filename;
            continue;
        }
        if (isset($usedIds[$id])) {
            $warnings[] = 'Ignorat; identificator duplicat: ' . $filename;
            continue;
        }
        $usedIds[$id] = true;

        $documents[] = [
            'id' => $id,
            'order' => aquaritm_document_order($stem),
            'title' => aquaritm_document_title($path, $stem, $extension),
            'format' => $extension,
            'mime_type' => $extension === 'pdf'
                ? 'application/pdf'
                : ($extension === 'md' ? 'text/markdown' : 'text/plain'),
            'document' => AQUARITM_DOCUMENTATION_BASE_URL
                . '/' . rawurlencode($filename),
            'filename' => $filename,
            'size_bytes' => filesize($path),
            'sha256' => hash_file('sha256', $path),
            'modified_at' => gmdate('c', (int) filemtime($path)),
        ];
    }

    usort($documents, static function (array $left, array $right): int {
        $byOrder = $left['order'] <=> $right['order'];
        if ($byOrder !== 0) {
            return $byOrder;
        }
        return strnatcasecmp($left['title'], $right['title']);
    });

    return [
        'schema_version' => 1,
        'generated_at' => gmdate('c'),
        'documents' => $documents,
        'warnings' => $warnings,
    ];
}

try {
    echo json_encode(
        aquaritm_document_catalog(__DIR__),
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
