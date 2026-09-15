<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, max-age=0');

function aquaritm_files(string $directory, string $webDirectory, array $extensions, string $field): array
{
    $result = [];
    if (!is_dir($directory)) {
        return $result;
    }

    $entries = scandir($directory);
    if ($entries === false) {
        return $result;
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
        if (!in_array($extension, $extensions, true)) {
            continue;
        }

        $stem = pathinfo($filename, PATHINFO_FILENAME);
        $id = preg_replace('/[^A-Za-z0-9._-]+/', '-', $stem);
        $id = trim((string) $id, '-');
        if ($id === '') {
            continue;
        }

        $displayName = preg_replace('/[_-]+/', ' ', $stem);
        $displayName = ucwords((string) $displayName);

        $result[] = [
            'id' => $id,
            'name' => $displayName,
            $field => $webDirectory . '/' . rawurlencode($filename),
            $field . '_sha256' => hash_file('sha256', $path),
            'size_bytes' => filesize($path),
            'modified_at' => gmdate('c', (int) filemtime($path)),
        ];
    }

    usort($result, static function (array $left, array $right): int {
        return strnatcasecmp($left['name'], $right['name']);
    });

    return $result;
}

$payload = [
    'schema_version' => 2,
    'generated_at' => gmdate('c'),
    'vectors' => aquaritm_files(__DIR__ . '/vectors', 'vectors', ['csv'], 'vector'),
    'audio' => aquaritm_files(
        __DIR__ . '/audio',
        'audio',
        ['wav', 'ogg', 'mp3', 'm4a', 'aac', 'flac'],
        'audio'
    ),
    'frequencies' => aquaritm_files(
        __DIR__ . '/frecvente',
        'frecvente',
        ['csv'],
        'frequency'
    ),
];

echo json_encode(
    $payload,
    JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT
);
