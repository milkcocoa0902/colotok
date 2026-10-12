#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
root_dir="$(cd "$script_dir/../.." && pwd -P)"

mode="${1:-linux}"
if [[ "$mode" != linux && "$mode" != apple ]]; then
    echo "Usage: $0 [linux|apple]" >&2
    exit 2
fi
if [[ "$mode" == apple && "$(uname -s)" != Darwin ]]; then
    echo "Apple consumer verification requires macOS" >&2
    exit 2
fi
if [[ "$mode" == apple && "$(uname -m)" != arm64 ]]; then
    echo "macosArm64 consumer runtime verification requires an Apple Silicon host" >&2
    exit 2
fi

cleanup_repository=false
if [[ -n "${COLOTOK_CONSUMER_REPOSITORY:-}" ]]; then
    mkdir -p "$COLOTOK_CONSUMER_REPOSITORY"
    repository="$(cd "$COLOTOK_CONSUMER_REPOSITORY" && pwd -P)"
else
    repository="$(mktemp -d "${TMPDIR:-/tmp}/colotok-consumer-m2.XXXXXX")"
    cleanup_repository=true
fi

default_maven_repository="$(cd "$HOME" && pwd -P)/.m2/repository"
if [[ "$repository" == "$default_maven_repository" ]]; then
    echo "Refusing to use the default Maven local repository: $repository" >&2
    exit 2
fi

cleanup() {
    if [[ "$cleanup_repository" == true ]]; then
        rm -rf "$repository"
    fi
}
trap cleanup EXIT

colotok_version="$("$root_dir/gradlew" -q :colotok:properties --no-configuration-cache |
    awk '/^version:/ { print $2; exit }')"
if [[ -z "$colotok_version" ]]; then
    echo "Could not determine the Colotok publication version" >&2
    exit 2
fi

publications=(KotlinMultiplatform Jvm Android Js)
if [[ "$mode" == apple ]]; then
    publications=(KotlinMultiplatform Android IosArm64 IosSimulatorArm64 MacosArm64)
fi
publish_tasks=()
for module in colotok colotok-coroutines colotok-loki; do
    for publication in "${publications[@]}"; do
        publish_tasks+=(":$module:publish${publication}PublicationToMavenLocal")
    done
done
if [[ "$mode" == linux ]]; then
    publish_tasks+=(:colotok-slf4j:publishMavenPublicationToMavenLocal :colotok-slf4j2:publishMavenPublicationToMavenLocal)
fi

"$root_dir/gradlew" "${publish_tasks[@]}" \
    "-Dmaven.repo.local=$repository" \
    -Pcolotok.skipPublicationSigning=true \
    --rerun-tasks \
    --no-configuration-cache \
    --max-workers=1

if [[ "$mode" == linux ]]; then
    for consumer in slf4j1 slf4j2; do
        "$root_dir/gradlew" \
            -p "$script_dir/$consumer" \
            consumerSmoke \
            "-PcolotokRepository=$repository" \
            "-PcolotokVersion=$colotok_version" \
            --refresh-dependencies \
            --rerun-tasks \
            --no-configuration-cache \
            --max-workers=1
    done
fi

# Only the consumer compiler changes; the producer remains on the repository baseline.
IFS=',' read -r -a consumer_versions <<< "${COLOTOK_CONSUMER_KOTLIN_VERSIONS:-2.4.21}"
consumer_apple=false
if [[ "$mode" == apple ]]; then
    consumer_apple=true
fi
for consumer_kotlin in "${consumer_versions[@]}"; do
    echo "KMP consumer: Kotlin=$consumer_kotlin, mode=$mode, Colotok=$colotok_version"
    "$root_dir/gradlew" \
        -p "$script_dir/kmp" \
        clean consumerSmoke \
        "-PcolotokRepository=$repository" \
        "-PcolotokVersion=$colotok_version" \
        "-PconsumerKotlin=$consumer_kotlin" \
        "-PconsumerApple=$consumer_apple" \
        --refresh-dependencies \
        --no-configuration-cache \
        --max-workers=1
done
